#!/usr/bin/env python3
"""Codex/CI evidence runner for ReelNest. Never deploys, pays, resets or logs env secrets.

G0: actual Java/Next.js builds + Docker Compose structural validation.
G1: full, isolated disposable DEV stack validator (requires explicit opt-in).
TF: Terraform backend-free initialization/validation (NEVER plan/apply).

Usage (from repository root):
  python3 scripts/cloud_evidence.py --phase g0
  python3 scripts/cloud_evidence.py --phase g1 --disposable-confirmed
  python3 scripts/cloud_evidence.py --phase tf

Outputs build-evidence/cloud-<phase>-latest.json/.md and per-step .log.
The whole build-evidence directory is gitignored because raw logs may be sensitive.
"""
from __future__ import annotations

import argparse
import datetime as dt
import json
from pathlib import Path
import re
import shutil
import subprocess
import sys
import time

ROOT = Path(__file__).resolve().parents[1]
EVIDENCE = ROOT / "build-evidence"
SECRET_PATTERNS = [
    re.compile(r"(?i)((?:password|api[_-]?key|secret|token|authorization|credential)\s*[:=]\s*)\S+"),
    re.compile(r"\b(?:AKIA|ASIA)[0-9A-Z]{16}\b"),
]


def redact(text: str) -> str:
    for pattern in SECRET_PATTERNS:
        text = pattern.sub(lambda m: (m.group(1) + "[REDACTED]") if m.lastindex else "[REDACTED]", text)
    return text


def needs(cmd: list[str]) -> str | None:
    executable = cmd[0]
    return None if shutil.which(executable) else f"Executable missing: {executable}"


def run(label: str, cmd: list[str], cwd: Path, phase: str, timeout: int) -> dict:
    """Run one check with bounded wall time and evidence; no input shell involved."""
    result = {"step": label, "command": cmd, "cwd": str(cwd.relative_to(ROOT)),
              "status": "NOT_RUN", "exit_code": None, "elapsed_seconds": 0, "detail": ""}
    error = needs(cmd)
    if error:
        result.update(status="BLOCKED", detail=error)
        return result
    started = time.monotonic()
    try:
        out = subprocess.run(cmd, cwd=cwd, capture_output=True, text=True,
                             timeout=timeout, check=False, errors="replace")
        all_output = redact((out.stdout or "") + "\n" + (out.stderr or ""))
        result.update(status="PASS" if out.returncode == 0 else "FAIL",
                      exit_code=out.returncode, detail="\n".join(all_output.splitlines()[-15:])[-2500:])
    except subprocess.TimeoutExpired as ex:
        all_output = redact(str(ex))
        result.update(status="TIMEOUT", detail=all_output)
    except OSError as ex:
        all_output = redact(str(ex))
        result.update(status="BLOCKED", detail=all_output)
    result["elapsed_seconds"] = round(time.monotonic() - started, 2)
    EVIDENCE.mkdir(exist_ok=True)
    log_path = EVIDENCE / f"cloud-{phase}-{len(list(EVIDENCE.glob(f'cloud-{phase}-step-*.log')))+1:02d}.log"
    log_path.write_text(all_output + "\n", encoding="utf-8")
    result["log"] = str(log_path.relative_to(ROOT))
    return result


def skipped(label: str, reason: str) -> dict:
    return {"step": label, "command": [], "status": "SKIPPED", "detail": reason}


def evaluate(phase: str) -> list[dict]:
    results: list[dict] = []

    def step(name: str, cmd: list[str], cwd: Path = ROOT, timeout: int = 300) -> dict:
        item = run(name, cmd, cwd, phase, timeout)
        results.append(item)
        print(f"[{item['status']}] {name}", flush=True)
        return item

    if phase == "g0":
        # Independent backend and frontend checks are both attempted once.
        step("Java Maven verify and JUnit tests", ["mvn", "-B", "-ntp", "verify"], ROOT / "apps/api", 540)
        web = ROOT / "apps/web"
        if not (web / "package-lock.json").exists():
            lock = step("Generate npm lockfile without package lifecycle scripts",
                        ["npm", "install", "--package-lock-only", "--ignore-scripts", "--no-audit", "--no-fund"], web, 240)
        else:
            lock = {"status": "PASS"}
            results.append(skipped("Generate npm lockfile", "Already exists; preserved"))
        if lock["status"] == "PASS":
            install = step("Reproducible npm ci", ["npm", "ci", "--no-audit", "--no-fund"], web, 420)
            if install["status"] == "PASS":
                step("TypeScript typecheck", ["npm", "run", "typecheck"], web, 240)
                step("Next.js production build", ["npm", "run", "build"], web, 420)
            else:
                results.append(skipped("TypeScript typecheck and Next.js build", "npm ci did not pass"))
        else:
            results.append(skipped("npm ci, typecheck and Next.js build", "npm lockfile generation did not pass"))
        step("Docker Compose configuration", ["docker", "compose", "config", "-q"], ROOT, 80)
    elif phase == "g1":
        # validate_local_stack.sh --full already calls smoke, compose up/build,
        # processing and playback E2E. Do not duplicate those expensive steps.
        info = step("Isolated Docker daemon access", ["docker", "info"], ROOT, 30)
        if info["status"] == "PASS":
            step("Disposable complete local stack, Flyway, upload and playback",
                 ["bash", "scripts/validate_local_stack.sh", "--full"], ROOT, 1500)
        else:
            results.append(skipped("Local stack E2E", "Docker daemon absent/unavailable"))
    elif phase == "tf":
        step("Terraform format check", ["terraform", "-chdir=infrastructure/aws", "fmt", "-check", "-recursive"], ROOT, 60)
        init = step("Terraform init, backend disabled", ["terraform", "-chdir=infrastructure/aws", "init", "-backend=false", "-input=false"], ROOT, 420)
        if init["status"] == "PASS":
            step("Terraform provider-backed validate", ["terraform", "-chdir=infrastructure/aws", "validate", "-no-color"], ROOT, 120)
        else:
            results.append(skipped("Terraform validate", "Provider init did not pass"))
    return results


def report(phase: str, results: list[dict]) -> dict:
    EVIDENCE.mkdir(exist_ok=True)
    now = dt.datetime.now(dt.timezone.utc).isoformat()
    data = {"project": "ReelNest", "build": 25, "timestamp_utc": now,
            "phase": phase, "claim": "Only the listed commands were actually run",
            "results": results, "statuses": {}}
    for item in results:
        status = item["status"]
        data["statuses"][status] = data["statuses"].get(status, 0) + 1
    (EVIDENCE / f"cloud-{phase}-latest.json").write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
    lines = [f"# ReelNest Codex Cloud {phase.upper()} test results", f"Generated: {now}",
             "These results are not a staging or production sign-off.", "",
             "| Step | Status | Duration (s) |", "|---|---|---:|"]
    for item in results:
        lines.append(f"| {item['step'].replace('|', '/')} | {item['status']} | {item.get('elapsed_seconds', 0)} |")
    lines += ["", "Full per-step diagnostics are in ignored `build-evidence/cloud-*.log` files.",
              "Before sharing any log, inspect it for secrets and personal data."]
    (EVIDENCE / f"cloud-{phase}-latest.md").write_text("\n".join(lines) + "\n", encoding="utf-8")
    print("EVIDENCE:", EVIDENCE / f"cloud-{phase}-latest.json")
    print("SUMMARY:", data["statuses"])
    return data


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--phase", choices=["g0", "g1", "tf"], default="g0")
    parser.add_argument("--disposable-confirmed", action="store_true",
                        help="Required for G1: isolated stack; its E2E tests mutate the dev database")
    args = parser.parse_args(argv)
    if args.phase == "g1" and not args.disposable_confirmed:
        parser.error("G1 requires --disposable-confirmed; never run on shared or production volumes")
    results = evaluate(args.phase)
    report(args.phase, results)
    return 1 if any(r["status"] in {"FAIL", "BLOCKED", "TIMEOUT"} for r in results) else 0


if __name__ == "__main__":
    sys.exit(main())

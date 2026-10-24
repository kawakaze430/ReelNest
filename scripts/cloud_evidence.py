#!/usr/bin/env python3
"""Codex/CI evidence runner for ReelNest. Never deploys, pays, resets or logs env secrets.

G0: actual Java/Next.js builds, ESLint + Docker Compose structural validation.
G1: full, isolated disposable DEV stack validator (requires explicit opt-in).
TF: Terraform backend-free initialization/validation (NEVER plan/apply).

Usage (from repository root):
  python3 scripts/cloud_evidence.py --phase g0
  REELNEST_DISPOSABLE_STACK=true COMPOSE_PROJECT_NAME=reelnest-test-local \
    python3 scripts/cloud_evidence.py --phase g1 --disposable-confirmed
  python3 scripts/cloud_evidence.py --sanitize-log raw.log sanitized.log
  python3 scripts/cloud_evidence.py --phase tf

Outputs build-evidence/cloud-<phase>-latest.json/.md and per-step .log.
The whole build-evidence directory is gitignored because raw logs may be sensitive.
"""
from __future__ import annotations

import argparse
import datetime as dt
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import signal
import subprocess
import sys
import time
import uuid

ROOT = Path(__file__).resolve().parents[1]
EVIDENCE = ROOT / "build-evidence"
REQUIRED_G0_CHECKS = {
    "Java Maven verify and JUnit tests", "Reproducible npm ci", "Frontend ESLint", "TypeScript typecheck",
    "Next.js production build", "Docker Compose configuration",
}
REQUIRED_CI_COMPONENTS = {
    "api", "web", "worker", "worker-image", "recommendation-image",
    "release-offline", "compose-validation",
}
RUN_ID = uuid.uuid4().hex
SOURCE_AT_START: dict | None = None
SECRET_KEY = re.compile(
    r"(?i)(password|passwd|token|secret|authorization|credential|api[_-]?key|private[_-]?key|access[_-]?(?:key|id)|cookie)"
)
ASSIGNMENT = re.compile(
    r"(?i)(?P<prefix>[\"']?(?P<key>[A-Z_][A-Z0-9_.-]*)[\"']?\s*[:=]\s*)"
    r"(?P<value>\"[^\"\r\n]*\"|'[^'\r\n]*'|[^\s,;&]+)"
)


def redact(text: str) -> str:
    """Remove common credential forms before stdout, reports or artifacts are written."""
    text = re.sub(r"-----BEGIN [^-]*PRIVATE KEY-----.*?-----END [^-]*PRIVATE KEY-----",
                  "[REDACTED PRIVATE KEY]", text, flags=re.DOTALL)
    text = re.sub(r"(?im)(\b(?:authorization|proxy-authorization|cookie|set-cookie)\s*:\s*)[^\r\n]+",
                  r"\1[REDACTED]", text)
    text = re.sub(r"(?i)(\bBearer\s+)[A-Za-z0-9._~+/=-]+", r"\1[REDACTED]", text)
    text = re.sub(r"([a-zA-Z][a-zA-Z0-9+.-]*://)[^/\s@]+@", r"\1[REDACTED]@", text)
    text = re.sub(
        r"(?i)([?&][^=&\s]*(?:password|token|secret|signature|credential|api[_-]?key|access[_-]?(?:key|id))[^=&\s]*=)[^&#\s]+",
        r"\1[REDACTED]", text,
    )
    text = ASSIGNMENT.sub(
        lambda m: m.group("prefix") + "[REDACTED]" if SECRET_KEY.search(m.group("key")) else m.group(0), text,
    )
    text = re.sub(r"\b(?:AKIA|ASIA)[0-9A-Z]{16}\b", "[REDACTED]", text)
    text = re.sub(r"\beyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]*", "[REDACTED JWT]", text)
    text = re.sub(r"\bgh[pousr]_[A-Za-z0-9_]{20,}\b", "[REDACTED]", text)
    return text


def safe_command(cmd: list[str]) -> list[str]:
    safe: list[str] = []
    hide_next = False
    for arg in cmd:
        safe.append("[REDACTED]" if hide_next else redact(arg))
        hide_next = arg.startswith("-") and "=" not in arg and bool(SECRET_KEY.search(arg))
    return safe


def normalized_next_source(path: Path) -> bytes:
    """Ignore only documented Next-generated formatting, imports and type globs."""
    if not path.exists():
        return b"<missing>"
    content = path.read_text(encoding="utf-8")
    if path.name == "tsconfig.json":
        try:
            config = json.loads(content)
            if isinstance(config.get("include"), list):
                config["include"] = [item for item in config["include"] if item != ".next/dev/types/**/*.ts"]
            return json.dumps(config, sort_keys=True, separators=(",", ":")).encode()
        except (ValueError, AttributeError):
            return content.encode()
    generated = {
        'import "./.next/types/routes.d.ts";',
        'import "./.next/types/root-params.d.ts";',
        '// This file is normally maintained by Next.js.',
        '// NOTE: This file should not be edited',
        '// see https://nextjs.org/docs/app/api-reference/config/typescript for more information.',
    }
    return "\n".join(line for line in content.splitlines() if line and line not in generated).encode()


def source_state() -> dict:
    """Hash source identity without exposing diff contents, paths or credential values."""
    try:
        commit = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, stderr=subprocess.DEVNULL).decode().strip()
        diff = subprocess.check_output(["git", "diff", "--binary", "HEAD", "--"], cwd=ROOT)
        untracked = subprocess.check_output(["git", "ls-files", "--others", "--exclude-standard", "-z"], cwd=ROOT)
        digest = hashlib.sha256(commit.encode() + b"\0" + diff)
        generated_paths = ("apps/web/tsconfig.json", "apps/web/next-env.d.ts")
        source_diff = subprocess.check_output(
            ["git", "diff", "--binary", "HEAD", "--", ".", *[f":(exclude){name}" for name in generated_paths]], cwd=ROOT,
        )
        semantic = hashlib.sha256(commit.encode() + b"\0" + source_diff)
        for name in generated_paths:
            semantic.update(b"\0" + name.encode() + b"\0" + normalized_next_source(ROOT / name))
        for name in sorted(filter(None, untracked.split(b"\0"))):
            path = ROOT / os.fsdecode(name)
            digest.update(b"\0" + name + b"\0")
            content = os.fsencode(os.readlink(path)) if path.is_symlink() else path.read_bytes()
            digest.update(content)
            semantic.update(b"\0" + name + b"\0" + content)
        return {"commit": commit, "fingerprint": digest.hexdigest(),
                "application_fingerprint": semantic.hexdigest(), "dirty": bool(diff or untracked)}
    except (OSError, subprocess.CalledProcessError) as ex:
        return {"commit": None, "fingerprint": None, "dirty": None,
                "error": redact(f"Source provenance unavailable: {type(ex).__name__}")}


def g0_readiness_error(path: Path) -> str | None:
    """Accept G0 only for the exact currently tested source, never a stale passing report."""
    try:
        proof = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return "G0 evidence is missing or unreadable; run G0 for the current source first"
    current = source_state()
    if not current.get("commit") or not current.get("fingerprint"):
        return "Current source provenance is unavailable"
    if not isinstance(proof, dict) or proof.get("phase") != "g0" or proof.get("source_commit") != current["commit"]:
        return "G0 evidence does not match the current source commit"
    if proof.get("format") == "github-actions-g0":
        if (os.environ.get("GITHUB_ACTIONS") != "true" or
                not os.environ.get("GITHUB_RUN_ID") or
                proof.get("workflow_run_id") != os.environ.get("GITHUB_RUN_ID") or
                proof["source_commit"] != os.environ.get("GITHUB_SHA") or current["dirty"]):
            return "CI G0 proof requires this Actions run, exact SHA and an unchanged checkout"
        components = proof.get("components", {})
        if not isinstance(components, dict):
            return "CI G0 component results are invalid"
        for component in REQUIRED_CI_COMPONENTS:
            status = components.get(component)
            if isinstance(status, dict):
                status = status.get("result")
            if status != "success":
                return f"CI G0 component did not succeed: {component}"
        return None
    if proof.get("source_fingerprint") != current["fingerprint"]:
        return "Source files changed since G0; rerun affected checks and G0 evidence"
    if proof.get("source_meaning_changed_during_run"):
        return "Source changed during G0; the final worktree was not validated by every required check"
    results = proof.get("results")
    if not isinstance(results, list) or not results or proof.get("outcome") != "PASS":
        return "G0 evidence is not a passing run"
    if any(not isinstance(item, dict) or item.get("status") not in {"PASS", "SKIPPED"} for item in results):
        return "G0 has failed, blocked, timed-out or unrun checks"
    passed = {item.get("step") for item in results if item.get("status") == "PASS" and item.get("exit_code") == 0}
    if not REQUIRED_G0_CHECKS.issubset(passed):
        return "G0 evidence omits required successful build checks"
    return None


def needs(cmd: list[str]) -> str | None:
    executable = cmd[0]
    return None if shutil.which(executable) else f"Executable missing: {executable}"


def run(label: str, cmd: list[str], cwd: Path, phase: str, timeout: int) -> dict:
    """Preserve unique, sanitized command evidence and stop the process group on timeout."""
    result = {"step": label, "command": safe_command(cmd), "cwd": str(cwd.relative_to(ROOT)),
              "status": "NOT_RUN", "exit_code": None, "elapsed_seconds": 0, "detail": ""}
    error = needs(cmd)
    if error:
        result.update(status="BLOCKED", detail=redact(error))
        return result
    started = time.monotonic()
    try:
        proc = subprocess.Popen(cmd, cwd=cwd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                text=True, errors="replace", start_new_session=True)
        try:
            output, _ = proc.communicate(timeout=timeout)
            result.update(status="PASS" if proc.returncode == 0 else "FAIL", exit_code=proc.returncode)
        except subprocess.TimeoutExpired as ex:
            try:
                os.killpg(proc.pid, signal.SIGTERM)
            except ProcessLookupError:
                pass
            try:
                output, _ = proc.communicate(timeout=5)
            except subprocess.TimeoutExpired:
                try:
                    os.killpg(proc.pid, signal.SIGKILL)
                except ProcessLookupError:
                    pass
                output, _ = proc.communicate()
            if not output and ex.output:
                output = ex.output.decode(errors="replace") if isinstance(ex.output, bytes) else ex.output
            output = (output or "") + f"\nCommand timed out after {timeout} seconds; process group terminated."
            result.update(status="TIMEOUT", exit_code=proc.returncode)
        all_output = redact(output or "")
    except OSError as ex:
        all_output = redact(str(ex))
        result.update(status="BLOCKED")
    result.update(elapsed_seconds=round(time.monotonic() - started, 2),
                  detail="\n".join(all_output.splitlines()[-15:])[-2500:])
    EVIDENCE.mkdir(parents=True, exist_ok=True)
    log_path = EVIDENCE / f"cloud-{phase}-{RUN_ID}-step-{uuid.uuid4().hex[:12]}.log"
    with log_path.open("x", encoding="utf-8") as log:
        log.write(all_output + "\n")
    result["log"] = str(log_path.relative_to(ROOT))
    return result


def skipped(label: str, reason: str) -> dict:
    return {"step": label, "command": [], "status": "SKIPPED", "detail": reason}


def evaluate(phase: str, *, disposable_confirmed: bool = False, g0_evidence: Path | None = None) -> list[dict]:
    results: list[dict] = []

    def step(name: str, cmd: list[str], cwd: Path = ROOT, timeout: int = 300,
             blocked_exit_codes: tuple[int, ...] = ()) -> dict:
        item = run(name, cmd, cwd, phase, timeout)
        if item.get("exit_code") in blocked_exit_codes and item["status"] == "FAIL":
            item["status"] = "BLOCKED"
        results.append(item)
        print(f"[{item['status']}] {name}", flush=True)
        return item

    if phase == "g0":
        # Independent backend and frontend checks are both attempted once.
        step("Java Maven verify and JUnit tests", ["mvn", "-B", "-ntp", "-DfailIfNoTests=true", "verify"], ROOT / "apps/api", 540)
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
                step("Frontend ESLint", ["npm", "run", "lint"], web, 240)
                step("TypeScript typecheck", ["npm", "run", "typecheck"], web, 240)
                step("Next.js production build", ["npm", "run", "build"], web, 420)
            else:
                results.append(skipped("Frontend ESLint, TypeScript typecheck and Next.js build", "npm ci did not pass"))
        else:
            results.append(skipped("npm ci, ESLint, typecheck and Next.js build", "npm lockfile generation did not pass"))
        step("Docker Compose configuration", ["docker", "compose", "config", "-q"], ROOT, 80)
    elif phase == "g1":
        error = g0_readiness_error(g0_evidence or EVIDENCE / "cloud-g0-latest.json")
        project = os.environ.get("COMPOSE_PROJECT_NAME", "")
        if not disposable_confirmed:
            error = "G1 requires explicit disposable confirmation"
        elif os.environ.get("REELNEST_DISPOSABLE_STACK") != "true" or not re.fullmatch(r"reelnest-(?:ci|test)-[a-z0-9][a-z0-9_-]*", project):
            error = "G1 requires REELNEST_DISPOSABLE_STACK=true and a unique reelnest-ci-/reelnest-test- COMPOSE_PROJECT_NAME"
        if error:
            return [{"step": "G0 and disposable stack prerequisite", "command": [], "status": "BLOCKED",
                     "exit_code": None, "detail": error}]
        # validate_local_stack.sh --full already calls smoke, compose up/build,
        # processing and playback E2E. Do not duplicate those expensive steps.
        info = step("Isolated Docker daemon access", ["docker", "info", "--format", "{{.ServerVersion}}"], ROOT, 30)
        if info["status"] == "PASS":
            step("Disposable complete local stack, Flyway, upload and playback",
                 ["bash", "scripts/validate_local_stack.sh", "--full"], ROOT, 2400, blocked_exit_codes=(3,))
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


def outcome(results: list[dict]) -> str:
    statuses = {item.get("status") for item in results}
    if statuses & {"FAIL", "TIMEOUT"}:
        return "FAIL"
    if "BLOCKED" in statuses:
        return "BLOCKED"
    if not results or "NOT_RUN" in statuses or not statuses.issubset({"PASS", "SKIPPED"}) or "PASS" not in statuses:
        return "NOT_RUN"
    return "PASS"


def report(phase: str, results: list[dict]) -> dict:
    EVIDENCE.mkdir(parents=True, exist_ok=True)
    now = dt.datetime.now(dt.timezone.utc).isoformat()
    state = source_state()
    build_file = ROOT / "docs/BUILD_VERSION"
    build = build_file.read_text().strip() if build_file.exists() else "unknown"
    meaning_changed = bool(SOURCE_AT_START and (
        SOURCE_AT_START.get("commit") != state.get("commit") or
        SOURCE_AT_START.get("application_fingerprint", SOURCE_AT_START.get("fingerprint")) !=
        state.get("application_fingerprint", state.get("fingerprint"))
    ))
    data = {"project": "ReelNest", "build": build, "timestamp_utc": now, "run_id": RUN_ID,
            "phase": phase, "claim": "Only the listed commands were actually run",
            "source_commit": state.get("commit"), "source_fingerprint": state.get("fingerprint"),
            "source_dirty": state.get("dirty"), "source_at_start": SOURCE_AT_START,
            "source_changed_during_run": bool(SOURCE_AT_START and SOURCE_AT_START != state),
            "source_meaning_changed_during_run": meaning_changed,
            "outcome": outcome(results), "results": results, "statuses": {}}
    if phase == "g0" and data["outcome"] == "PASS":
        passed = {item.get("step") for item in results if item.get("status") == "PASS" and item.get("exit_code") == 0}
        if not REQUIRED_G0_CHECKS.issubset(passed):
            data["outcome"] = "NOT_RUN"
        if meaning_changed:
            data["outcome"] = "NOT_RUN"
            data["readiness_detail"] = "Source changed during G0; the final worktree was not validated by every required check"
    if state.get("error"):
        data["provenance_error"] = state["error"]
    for item in results:
        status = item["status"]
        data["statuses"][status] = data["statuses"].get(status, 0) + 1
    payload = json.dumps(data, indent=2) + "\n"
    (EVIDENCE / f"cloud-{phase}-{RUN_ID}.json").write_text(payload, encoding="utf-8")
    temporary = EVIDENCE / f".cloud-{phase}-{RUN_ID}.tmp"
    temporary.write_text(payload, encoding="utf-8")
    temporary.replace(EVIDENCE / f"cloud-{phase}-latest.json")
    lines = [f"# ReelNest Codex Cloud {phase.upper()} test results", f"Generated: {now}",
             f"Source: {state.get('commit')}; worktree dirty: {state.get('dirty')}; outcome: {data['outcome']}",
             "These results are not a staging or production sign-off.", "",
             "| Step | Status | Exit | Duration (s) |", "|---|---|---:|---:|"]
    for item in results:
        lines.append(f"| {item['step'].replace('|', '/')} | {item['status']} | {item.get('exit_code')} | {item.get('elapsed_seconds', 0)} |")
    lines += ["", "Per-step diagnostics use unique filenames and are sanitized before being saved.",
              "Inspect artifacts before sharing; no generic sanitizer proves a log contains no sensitive data."]
    markdown = "\n".join(lines) + "\n"
    (EVIDENCE / f"cloud-{phase}-{RUN_ID}.md").write_text(markdown, encoding="utf-8")
    (EVIDENCE / f"cloud-{phase}-latest.md").write_text(markdown, encoding="utf-8")
    print("EVIDENCE:", EVIDENCE / f"cloud-{phase}-latest.json")
    print("SUMMARY:", data["statuses"])
    return data


def main(argv: list[str] | None = None) -> int:
    global RUN_ID, SOURCE_AT_START
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--phase", choices=["g0", "g1", "tf"], default="g0")
    parser.add_argument("--disposable-confirmed", action="store_true",
                        help="Required for G1: isolated stack; its E2E tests mutate the dev database")
    parser.add_argument("--g0-evidence", type=Path, help="Exact-source passing G0 report or current Actions needs proof")
    parser.add_argument("--sanitize-log", nargs=2, metavar=("INPUT", "OUTPUT"),
                        help="Sanitize a diagnostic file without executing any phase")
    args = parser.parse_args(argv)
    if args.sanitize_log:
        source, destination = map(Path, args.sanitize_log)
        if source.resolve() == destination.resolve():
            parser.error("Sanitizer input and output must be different paths")
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_text(redact(source.read_text(encoding="utf-8", errors="replace")), encoding="utf-8")
        return 0
    if args.phase == "g1" and not args.disposable_confirmed:
        parser.error("G1 requires --disposable-confirmed; never run on shared or production volumes")
    RUN_ID = uuid.uuid4().hex
    SOURCE_AT_START = source_state()
    results = evaluate(args.phase, disposable_confirmed=args.disposable_confirmed, g0_evidence=args.g0_evidence)
    data = report(args.phase, results)
    return 0 if data["outcome"] == "PASS" else 1


if __name__ == "__main__":
    sys.exit(main())

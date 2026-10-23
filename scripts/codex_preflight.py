#!/usr/bin/env python3
"""Safe, cross-platform preflight for local ReelNest development.

No downloads, Docker startup, remote network calls, or secret reads.
Exit 2 when prerequisites for a full local integration session are missing.
Use --json for structured feedback to ChatGPT after a Codex session.
"""
from __future__ import annotations

import argparse
import json
import os
from pathlib import Path
import platform
import re
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
COMMANDS = {
    "python": ["python3", "python"],
    "java": ["java"],
    "maven": ["mvn", "mvn.cmd"],
    "node": ["node"],
    "npm": ["npm", "npm.cmd"],
    "docker": ["docker"],
    "git": ["git"],
}
OPTIONAL = {"terraform": ["terraform"], "ffmpeg": ["ffmpeg"], "curl": ["curl"], "codex": ["codex"]}


def which_first(names: list[str]) -> str | None:
    for name in names:
        executable = shutil.which(name)
        if executable:
            return executable
    return None


def version(exe: str, flag: str = "--version") -> str:
    try:
        done = subprocess.run([exe, flag], text=True, capture_output=True, timeout=6, check=False)
        combined = ((done.stdout or "") + "\n" + (done.stderr or "")).strip().splitlines()
        return combined[0][:160] if combined else "unknown"
    except (OSError, subprocess.TimeoutExpired):
        return "unavailable"


def diagnose(root: Path = ROOT) -> dict:
    found = {}
    for name, possibilities in {**COMMANDS, **OPTIONAL}.items():
        exe = which_first(possibilities)
        if name == "git":
            flag = "--version"
        else:
            flag = "-version" if name == "java" else "--version"
        found[name] = {"installed": bool(exe), "version": version(exe, flag) if exe else None}
    core = {
        "compose_yaml": root / "compose.yaml",
        "spring_pom": root / "apps/api/pom.xml",
        "next_package_json": root / "apps/web/package.json",
        "release_gates": root / "scripts/release_gate.py",
        "codex_instructions": root / "AGENTS.md",
    }
    repo = {name: p.is_file() for name, p in core.items()}
    # No runtime execution: we must not mistake file presence for passing tests.
    lock = (root / "apps/web/package-lock.json").exists()
    git_repo = (root / ".git").is_dir()
    warnings = []
    if not lock:
        warnings.append("No frontend npm lockfile: generate with npm install on networked machine, review and commit it")
    if not git_repo:
        warnings.append("No .git repository yet: initialize Git before running Codex or pushing to GitHub")
    missing = [name for name in COMMANDS if not found[name]["installed"]]
    missing += [name for name, present in repo.items() if not present]
    if platform.system() == "Windows" and not os.environ.get("WSL_DISTRO_NAME"):
        warnings.append("Windows: prefer WSL2 + Docker Desktop WSL integration for Linux shell scripts")
    return {
        "project": "ReelNest", "source": str(root), "platform": platform.platform(),
        "tools": found, "project_files": repo, "frontend_lockfile_present": lock,
        "git_initialized": git_repo, "missing_required": missing,
        "warnings": warnings, "local_integration_ready_for_attempt": not bool(missing),
        "actual_build_and_runtime_results": "NOT_RUN_BY_PREFLIGHT",
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--json", action="store_true", help="Print compact machine-readable results")
    parser.add_argument("--output", type=Path, help="Optional path for report (do not commit evidence)")
    args = parser.parse_args()
    result = diagnose()
    if args.output:
        path = args.output if args.output.is_absolute() else ROOT / args.output
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    if args.json:
        print(json.dumps(result, indent=2))
    else:
        for name, data in result["tools"].items():
            print(f"{'OK' if data['installed'] else '--'} {name:11s} {data['version'] or 'not installed'}")
        for warning in result["warnings"]:
            print("NOTE:", warning)
        print("Required missing:", ", ".join(result["missing_required"]) or "none")
        print("Builds/run tests: NOT RUN by this preflight")
    return 2 if result["missing_required"] else 0


if __name__ == "__main__":
    sys.exit(main())

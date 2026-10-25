"""Exercise CI prerequisite setup with fake binaries and no package/network access."""
import os
from pathlib import Path
import subprocess

import pytest


ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "scripts/ensure_ci_ffmpeg.sh"


def executable(path, text):
    path.write_text("#!/bin/bash\n" + text)
    path.chmod(0o755)


@pytest.fixture
def sandbox(tmp_path):
    binaries = tmp_path / "bin"
    binaries.mkdir()
    calls = tmp_path / "calls"
    executable(binaries / "sudo", '''
printf '%s\\n' "$*" >> "$FFMPEG_SETUP_CALLS"
if [[ "$*" == *" update" ]]; then
  exit "${UPDATE_EXIT:-0}"
fi
if [[ "${INSTALL_EXIT:-0}" != 0 ]]; then
  exit "$INSTALL_EXIT"
fi
for tool in ffmpeg ffprobe; do
  printf '#!/bin/bash\\nprintf "%%s\\\\n" "%s installed"\\n' "$tool" > "$FFMPEG_SETUP_BIN/$tool"
  /bin/chmod +x "$FFMPEG_SETUP_BIN/$tool"
done
''')
    environment = {**os.environ, "PATH": str(binaries), "FFMPEG_SETUP_CALLS": str(calls),
                   "FFMPEG_SETUP_BIN": str(binaries)}
    return binaries, calls, environment


def run_setup(sandbox, **overrides):
    return subprocess.run(["/bin/bash", str(SCRIPT)], text=True, capture_output=True,
                          env={**sandbox[2], **overrides}, timeout=5, check=False)


def present(sandbox, tool, exit_code=0):
    executable(sandbox[0] / tool,
               f'printf "%s\\n" "{tool} existing $*"\nexit {exit_code}\n')


def test_preinstalled_tools_skip_package_manager_and_validate_both(sandbox):
    present(sandbox, "ffmpeg")
    present(sandbox, "ffprobe")
    result = run_setup(sandbox)
    assert result.returncode == 0, result.stderr
    assert not sandbox[1].exists()
    assert "ffmpeg existing -hide_banner -version" in result.stdout
    assert "ffprobe existing -hide_banner -version" in result.stdout


@pytest.mark.parametrize("existing", [None, "ffmpeg", "ffprobe"])
def test_missing_either_tool_installs_and_validates_both(sandbox, existing):
    if existing:
        present(sandbox, existing)
    result = run_setup(sandbox)
    assert result.returncode == 0, result.stderr
    calls = sandbox[1].read_text().splitlines()
    assert len(calls) == 2
    assert calls[0].startswith("-n apt-get ") and calls[0].endswith(" update")
    assert "APT::Update::Error-Mode=any" in calls[0]
    assert calls[1].endswith(" install --yes --no-install-recommends ffmpeg")
    for call in calls:
        assert "Acquire::Retries=1" in call
        assert "Acquire::http::Timeout=20" in call
        assert "Acquire::https::Timeout=20" in call
        assert "DPkg::Lock::Timeout=30" in call
    assert "ffmpeg installed" in result.stdout and "ffprobe installed" in result.stdout


@pytest.mark.parametrize("failure,expected_calls", [("UPDATE_EXIT", 1), ("INSTALL_EXIT", 2)])
def test_package_manager_failure_preserves_exit_and_stops(sandbox, failure, expected_calls):
    result = run_setup(sandbox, **{failure: "42"})
    assert result.returncode == 42
    assert len(sandbox[1].read_text().splitlines()) == expected_calls
    assert "installed" not in result.stdout


@pytest.mark.parametrize("failing_tool", ["ffmpeg", "ffprobe"])
def test_present_but_broken_executable_fails_without_reinstall(sandbox, failing_tool):
    for tool in ("ffmpeg", "ffprobe"):
        present(sandbox, tool, 43 if tool == failing_tool else 0)
    result = run_setup(sandbox)
    assert result.returncode == 43
    assert not sandbox[1].exists()

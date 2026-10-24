"""Check the image's staged Python files, rather than importing the checkout."""
from pathlib import Path
import shlex
import shutil
import subprocess
import sys


APP = Path(__file__).resolve().parents[1]


def test_image_entry_point_imports_from_only_copied_modules(tmp_path):
    staged = tmp_path / "image-app"
    staged.mkdir()
    for line in (APP / "Dockerfile").read_text().splitlines():
        if not line.lstrip().startswith("COPY "):
            continue
        instruction = shlex.split(line, comments=True)
        if not instruction or instruction[0] != "COPY":
            continue
        for source in instruction[1:-1]:
            if source.startswith("--"):
                continue
            for path in APP.glob(source):
                if path.is_file() and path.suffix == ".py":
                    shutil.copy2(path, staged / path.name)
    assert (staged / "main.py").is_file(), "Image must contain its ASGI entry point"
    result = subprocess.run(
        [sys.executable, "-I", "-c",
         "import sys; sys.path.insert(0, sys.argv[1]); from main import app; "
         "assert any(route.path == '/health' for route in app.routes)", str(staged)],
        cwd=staged, capture_output=True, text=True, timeout=60,
    )
    assert result.returncode == 0, result.stdout + result.stderr

"""Source/provenance contracts; actual MinIO image execution is a G1 check."""
from pathlib import Path
import re

import yaml


ROOT = Path(__file__).resolve().parents[2]
INFRA = ROOT / "infrastructure/minio"


def test_development_object_storage_uses_pinned_official_source_builds():
    services = yaml.safe_load((ROOT / "compose.yaml").read_text())["services"]
    for service, filename, module in [
        ("minio", "Dockerfile", "minio"),
        ("minio-init", "Dockerfile.mc", "mc"),
    ]:
        config = services[service]
        assert config["build"] == {
            "context": "./infrastructure/minio", "dockerfile": filename,
        }
        dockerfile = (INFRA / filename).read_text()
        match = re.search(r"go install -p 2 github\.com/minio/" + module + r"@([0-9a-f]{40})\b", dockerfile)
        assert match, "Build the official module at an immutable commit"
        assert config["image"].endswith(":" + match.group(1))
        assert "GOTOOLCHAIN=local" in dockerfile
        assert "GOSUMDB=off" not in dockerfile and "GOINSECURE=" not in dockerfile
        assert "--mount=type=secret,id=proxy_ca" in dockerfile
        assert "SSL_CERT_FILE=/run/secrets/proxy_ca" in dockerfile
        assert not re.search(r"^COPY\s+.*(?:proxy_ca|proxy-ca)", dockerfile, re.M)
        assert "USER 10001:10001" in dockerfile


def test_nonroot_object_storage_preserves_runtime_directories_and_entrypoints():
    server = (INFRA / "Dockerfile").read_text()
    client = (INFRA / "Dockerfile.mc").read_text()
    assert "chown minio:minio /data" in server
    assert 'ENTRYPOINT ["/usr/local/bin/minio"]' in server
    assert "FROM debian:bookworm-slim" in client, "Client initialization needs /bin/sh"
    assert "ENV HOME=/home/minio" in client
    assert "--create-home minio" in client
    assert 'ENTRYPOINT ["/usr/local/bin/mc"]' in client

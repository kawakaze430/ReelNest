# Development object storage images

The community MinIO project now distributes source only. The former
`quay.io/minio/minio:latest` and `quay.io/minio/mc:latest` references did not resolve
in the cloud development environment. Development Compose therefore builds both
tools from the official upstream Go modules at immutable commits:

| Tool | Official source | Commit |
| --- | --- | --- |
| Server | `github.com/minio/minio`, release `RELEASE.2025-10-15T17-29-55Z` | `9e49d5e7a648f00e26f2246f4dc28e6b07f8c84a` |
| Client | `github.com/minio/mc` | `77f82e18b5401a65958f1619df6ebb994634bd88` |

`docker compose build minio minio-init` uses Go 1.24.9 with automatic toolchain
downloads disabled and normal HTTPS and Go module checksum verification enabled.
These are local development images, not production deployment images. The
upstream community repository is unmaintained; production storage and licensing
decisions remain separate from disposable integration testing.

Managed-cloud builds may supply an optional BuildKit secret named `proxy_ca`
containing the system's combined CA bundle. It is mounted only during the Go
compilation step and never copied into an image layer. Runtime images receive
only the build image's unmodified public CA bundle. Standard Docker/CI builds
need no secret. Both processes run as UID/GID 10001, with writable server data
and client configuration directories. The local CORS XML bind mount must be
readable by that UID; it contains public configuration rather than credentials.

References:

- https://github.com/minio/minio#source-only-distribution
- https://github.com/minio/minio/tree/9e49d5e7a648f00e26f2246f4dc28e6b07f8c84a
- https://github.com/minio/mc/tree/77f82e18b5401a65958f1619df6ebb994634bd88

# syntax=docker/dockerfile:1
# Development-only fallback for the discontinued community binary images.
# Official source: minio/minio RELEASE.2025-10-15T17-29-55Z, pinned below.
FROM golang:1.24.9-bookworm AS build
ENV CGO_ENABLED=0 GOTOOLCHAIN=local
# Go's HTTPS module proxy and checksum database remain enabled. The optional
# secret is a combined CA bundle for managed-cloud builds, never an image layer.
RUN --mount=type=secret,id=proxy_ca \
    --mount=type=cache,id=reelnest-minio-mod,target=/go/pkg/mod,sharing=locked \
    --mount=type=cache,id=reelnest-minio-go,target=/root/.cache/go-build,sharing=locked \
    set -eu; \
    if [ -s /run/secrets/proxy_ca ]; then export SSL_CERT_FILE=/run/secrets/proxy_ca; fi; \
    GOBIN=/out go install -p 2 github.com/minio/minio@9e49d5e7a648f00e26f2246f4dc28e6b07f8c84a

FROM debian:bookworm-slim
COPY --from=build /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/ca-certificates.crt
COPY --from=build /out/minio /usr/local/bin/minio
RUN groupadd --gid 10001 minio \
    && useradd --uid 10001 --gid 10001 --create-home minio \
    && mkdir -p /data && chown minio:minio /data
USER 10001:10001
ENV HOME=/home/minio
WORKDIR /data
EXPOSE 9000 9001
ENTRYPOINT ["/usr/local/bin/minio"]
CMD ["server", "/data", "--console-address", ":9001"]

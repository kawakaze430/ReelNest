# syntax=docker/dockerfile:1
# Development client built from immutable official minio/mc source.
# Observed upstream commit: 77f82e18b5401a65958f1619df6ebb994634bd88.
FROM golang:1.24.9-bookworm AS build
ENV CGO_ENABLED=0 GOTOOLCHAIN=local
RUN --mount=type=secret,id=proxy_ca \
    --mount=type=cache,id=reelnest-minio-mod,target=/go/pkg/mod,sharing=locked \
    --mount=type=cache,id=reelnest-minio-go,target=/root/.cache/go-build,sharing=locked \
    set -eu; \
    if [ -s /run/secrets/proxy_ca ]; then export SSL_CERT_FILE=/run/secrets/proxy_ca; fi; \
    GOBIN=/out go install -p 2 github.com/minio/mc@77f82e18b5401a65958f1619df6ebb994634bd88

# Keep /bin/sh for Compose's bucket/CORS initialization entrypoint.
FROM debian:bookworm-slim
COPY --from=build /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/ca-certificates.crt
COPY --from=build /out/mc /usr/local/bin/mc
RUN groupadd --gid 10001 minio \
    && useradd --uid 10001 --gid 10001 --create-home minio
USER 10001:10001
ENV HOME=/home/minio
WORKDIR /home/minio
ENTRYPOINT ["/usr/local/bin/mc"]
CMD ["--help"]

#!/usr/bin/env bash
# GitHub-hosted runners normally provide both tools. Avoid unnecessary apt work.
set -euo pipefail

if ! command -v ffmpeg >/dev/null 2>&1 || ! command -v ffprobe >/dev/null 2>&1; then
  apt_options=(
    -o Acquire::Retries=1
    -o Acquire::http::Timeout=20
    -o Acquire::https::Timeout=20
    -o DPkg::Lock::Timeout=30
  )
  sudo -n apt-get "${apt_options[@]}" -o APT::Update::Error-Mode=any update
  sudo -n apt-get "${apt_options[@]}" install --yes --no-install-recommends ffmpeg
fi

# Installed does not imply usable: fail if either actual executable fails.
ffmpeg -hide_banner -version
ffprobe -hide_banner -version

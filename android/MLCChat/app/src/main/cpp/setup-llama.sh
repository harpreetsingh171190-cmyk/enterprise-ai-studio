#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
VERSION="$(cat LLAMA_CPP_VERSION.txt)"
if [[ -e llama.cpp ]]; then
  echo "llama.cpp directory already exists; check its version manually."
  exit 1
fi
git clone --depth 1 --branch "$VERSION" https://github.com/ggml-org/llama.cpp.git llama.cpp
echo "Pinned llama.cpp: $VERSION"

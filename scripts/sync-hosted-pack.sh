#!/usr/bin/env bash
# Build local + met a jour hosted/dpmr-pack.zip et hosted/dpmr-pack-manifest.json
# Usage :
#   REPO=ton-user/DPMR bash scripts/sync-hosted-pack.sh
# La branche dans l'URL du manifest = branche git courante (git rev-parse --abbrev-ref HEAD)
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if [[ -z "${REPO:-}" ]]; then
  echo "Definis REPO=user/repo GitHub, ex: REPO=moncompte/DPMR $0"
  exit 1
fi

BRANCH="$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo main)"
bash scripts/build-resourcepack.sh
mkdir -p hosted
cp -f dist/dpmr-pack.zip hosted/dpmr-pack.zip
SHA1=$(shasum -a 1 hosted/dpmr-pack.zip | awk '{print $1}')
URL_ZIP="https://raw.githubusercontent.com/${REPO}/${BRANCH}/hosted/dpmr-pack.zip"
printf '%s\n' "{\"url\":\"${URL_ZIP}\",\"sha1\":\"${SHA1}\"}" > hosted/dpmr-pack-manifest.json
echo "OK -> hosted/dpmr-pack.zip + hosted/dpmr-pack-manifest.json"
cat hosted/dpmr-pack-manifest.json
echo "Ensuite: git add hosted && git commit -m 'resource pack' && git push"

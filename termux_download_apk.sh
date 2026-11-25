#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"
pkg install -y gh
if ! gh auth status >/dev/null 2>&1; then gh auth login; fi
REPO="$(gh repo view --json nameWithOwner -q .nameWithOwner 2>/dev/null || true)"
if [[ -z "$REPO" ]]; then
  read -r -p "Nama repository (username/repo): " REPO
fi
mkdir -p "$ROOT_DIR/apk-hasil"
echo "Mengambil artifact build terbaru dari GitHub Actions..."
RUN_ID="$(gh run list --repo "$REPO" --workflow build_apk.yml --status success --limit 1 --json databaseId -q '.[0].databaseId')"
if [[ -z "$RUN_ID" || "$RUN_ID" == "null" ]]; then
  echo "Belum ada build sukses. Push source dahulu dan tunggu workflow selesai."
  exit 1
fi
gh run download "$RUN_ID" --repo "$REPO" --dir "$ROOT_DIR/apk-hasil"
echo "Selesai. Periksa folder: $ROOT_DIR/apk-hasil"

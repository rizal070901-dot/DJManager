#!/data/data/com.termux/files/usr/bin/bash
# Termux is used to publish the source. GitHub Actions builds the Android APK,
# because Android SDK Linux host tools (aapt2, etc.) are not reliably runnable
# natively inside Android/Termux.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

pkg update -y
pkg install -y git gh

if ! gh auth status >/dev/null 2>&1; then
  echo "Login GitHub terlebih dahulu:"
  gh auth login
fi

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  git init
fi

git branch -M main

echo
read -r -p "Nama repository GitHub (contoh: username/DjalimenManager): " REPO
if [[ -z "$REPO" || "$REPO" != */* ]]; then
  echo "Format harus username/nama-repository."
  exit 1
fi

if ! gh repo view "$REPO" >/dev/null 2>&1; then
  read -r -p "Repository belum ada. Buat private? [y/N]: " PRIVATE
  if [[ "$PRIVATE" =~ ^[Yy]$ ]]; then
    gh repo create "$REPO" --private --source=. --remote=origin --push
  else
    gh repo create "$REPO" --public --source=. --remote=origin --push
  fi
else
  URL="https://github.com/${REPO}.git"
  if git remote get-url origin >/dev/null 2>&1; then
    git remote set-url origin "$URL"
  else
    git remote add origin "$URL"
  fi
  git add -A
  if ! git diff --cached --quiet; then
    git commit -m "Djalimen Manager 1.4.0"
  fi
  git push -u origin main
fi

echo
echo "Sumber berhasil dikirim ke GitHub."
echo "Buka tab Actions pada repository untuk memantau build APK."

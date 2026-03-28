#!/usr/bin/env bash
# Envoie dist/dpmr-pack.zip sur ton VPS (scp).
#
# Usage (une fois) :
#   export DEPLOY_HOST=1.2.3.4
#   export DEPLOY_USER=root          # optionnel
#   export DEPLOY_PATH=/var/www/dpmr # dossier distant (doit exister)
#   export SSH_PORT=22               # ou 2222 si ton VPS n’utilise pas 22
#   export SSH_KEY=~/.ssh/id_ed25519 # optionnel
#   bash scripts/deploy-resourcepack.sh
#
# Erreurs fréquentes :
#   Connection refused → mauvaise IP, SSH pas ouvert, ou mauvais port (SSH_PORT).
#   Permission denied → clé SSH pas sur le VPS, ou mauvais utilisateur (essaye ubuntu).
#   scp: dest open failed → DEPLOY_PATH n’existe pas : mkdir -p sur le VPS.
#   Host key verification failed → ssh-keygen -R "ton.host" puis reconnecte.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ZIP="$ROOT_DIR/dist/dpmr-pack.zip"

if [[ ! -f "$ZIP" ]]; then
  echo "Pas de $ZIP — lance d’abord: bash scripts/build-resourcepack.sh"
  exit 1
fi

: "${DEPLOY_HOST:?Définis DEPLOY_HOST (IP ou domaine du VPS)}"
DEPLOY_USER="${DEPLOY_USER:-root}"
DEPLOY_PATH="${DEPLOY_PATH:-/var/www/dpmr}"
SSH_PORT="${SSH_PORT:-22}"

SCP_OPTS=(-P "$SSH_PORT" -o ConnectTimeout=15 -o BatchMode=yes)
if [[ -n "${SSH_KEY:-}" ]]; then
  SCP_OPTS+=(-i "$SSH_KEY")
fi

REMOTE="${DEPLOY_USER}@${DEPLOY_HOST}:${DEPLOY_PATH}/dpmr-pack.zip"

echo "→ Envoi vers $REMOTE"
if ! scp "${SCP_OPTS[@]}" "$ZIP" "$REMOTE"; then
  echo ""
  echo "Échec scp. Essaie en interactif (sans BatchMode) pour voir le détail :"
  echo "  scp -P $SSH_PORT ${SSH_KEY:+-i $SSH_KEY} \"$ZIP\" \"$REMOTE\""
  echo ""
  echo "Teste SSH :"
  echo "  ssh -p $SSH_PORT ${SSH_KEY:+-i $SSH_KEY} ${DEPLOY_USER}@${DEPLOY_HOST} echo ok"
  exit 1
fi

echo "OK: fichier sur le VPS."
echo ""
SHA1=$(shasum -a 1 "$ZIP" | awk '{print $1}')
echo "SHA1 à coller dans plugins/DiviserPourMieuxRegner/config.yml :"
echo "  resource-pack.sha1: \"$SHA1\""
echo ""
echo "Puis en jeu (admin) : /dpmr warworld reload"
echo "Les joueurs : /ressourcepack ou reconnexion."

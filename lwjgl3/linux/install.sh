#!/bin/sh
set -e
DIR="$(cd "$(dirname "$0")" && pwd)"
APPS="${XDG_DATA_HOME:-$HOME/.local/share}/applications"
mkdir -p "$APPS"
sed "s|@DIR@|$DIR|g" "$DIR/SantaGame.desktop" > "$APPS/santa-game.desktop"
chmod +x "$APPS/santa-game.desktop"
chmod +x "$DIR/bin/SantaGame"
echo "Santa Game was added to your applications menu."

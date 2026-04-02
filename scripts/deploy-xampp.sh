#!/bin/zsh

set -euo pipefail

REPO_ROOT="/Users/eljhon72/Repositorios Githup/Portafolio de proyectos/Smart Vest/Smartvest"
WEBAPP_DIR="$REPO_ROOT"
XAMPP_TARGET="/Applications/XAMPP/xamppfiles/htdocs/Smartvest"

if [[ ! -d "$WEBAPP_DIR" ]]; then
  echo "No existe la carpeta de la web: $WEBAPP_DIR"
  exit 1
fi

mkdir -p "$XAMPP_TARGET"
rsync -a --delete --exclude "node_modules" --exclude ".git" "$WEBAPP_DIR/" "$XAMPP_TARGET/"
rm -f "$XAMPP_TARGET/index.php" "$XAMPP_TARGET/.htaccess"

if [[ -f "$WEBAPP_DIR/dist/index.html" ]]; then
  cp "$WEBAPP_DIR/dist/index.html" "$XAMPP_TARGET/index.html"
fi

if [[ -d "$WEBAPP_DIR/dist/assets" ]]; then
  rm -rf "$XAMPP_TARGET/assets"
  cp -R "$WEBAPP_DIR/dist/assets" "$XAMPP_TARGET/assets"
fi

echo "Publicacion completada en: $XAMPP_TARGET"

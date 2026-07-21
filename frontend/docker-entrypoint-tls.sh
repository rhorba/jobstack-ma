#!/bin/sh
set -e

CERT_DIR=/etc/nginx/certs

if [ ! -f "$CERT_DIR/server.crt" ] || [ ! -f "$CERT_DIR/server.key" ]; then
  echo "No TLS cert found in $CERT_DIR — generating a self-signed one."
  echo "Replace server.crt/server.key in this volume with a real CA-issued cert before going live on a public domain."
  openssl req -x509 -nodes -newkey rsa:2048 -days 365 \
    -keyout "$CERT_DIR/server.key" -out "$CERT_DIR/server.crt" \
    -subj "/CN=${APP_DOMAIN:-jobstack.local}"
fi

exec nginx -g "daemon off;"

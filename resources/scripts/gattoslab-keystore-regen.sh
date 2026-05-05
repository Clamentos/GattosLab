#!/bin/bash

# Put in directory: /etc/letsencrypt/renewal-hooks/deploy
# Make this script permissions: -rwx------ root root

DOMAIN="gattoslab.dev"
TARGET_DIR="/GattosLab/run"
KEYSTORE_NAME="keystore.p12"
KEYSTORE_PASSWORD="the_keystore_password_goes_here..."
OWNER_USER="gattoslab"
OWNER_GROUP="gattoslab-group"
FULLCHAIN="/etc/letsencrypt/live/$DOMAIN/fullchain.pem"
PRIVKEY="/etc/letsencrypt/live/$DOMAIN/privkey.pem"
CHAIN="/etc/letsencrypt/live/$DOMAIN/chain.pem"

echo "Begin keystore-regen..."
mkdir -p "$TARGET_DIR"
openssl pkcs12 -export -in "$FULLCHAIN" -inkey "$PRIVKEY" -out "$TARGET_DIR/$KEYSTORE_NAME" -name undertow -CAfile "$CHAIN" -caname root -password "pass:$KEYSTORE_PASSWORD"

chown $OWNER_USER:$OWNER_GROUP "$TARGET_DIR/$KEYSTORE_NAME"
chmod 400 "$TARGET_DIR/$KEYSTORE_NAME"

# In theory this should require an app-restart because the cert will be different and therefore the keystore
gattoslab-deploy
echo "End keystore-regen"

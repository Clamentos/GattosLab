#!/bin/bash
setsid java -jar $TARGET_DIR/gattoslab*.jar --spring.profiles.active=prod --server.ssl.key-store-password=$KEYSTORE_PASSWORD --app.session.admin.apiKey=$API_KEY --app.database.connectionString=$DB_STRING -Xmx1024m < /dev/null &

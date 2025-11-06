#! /bin/bash
nohup java -jar ./gattoslab*.jar --spring.profiles.active=prod --server.ssl.key-store-password=$0 --app.admin.apiKey=$1 -Xmx2G &
echo $! > ./current_pid.txt

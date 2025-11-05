#! /bin/bash
nohup java -jar *.jar -Dspring.profiles.active=prod --server.ssl.key-store-password=$0 --app.metrics.apiKey=$1 &
echo $! > ./current_pid.txt

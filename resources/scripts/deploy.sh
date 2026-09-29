#!/bin/bash

##
#    directory                                                  permissions
#
#    /GattosLab                                                 drwxr-xr-x root root
#        /run                                                   drwxr-xr-x gattoslab gattoslab-group
#            gattoslab-x.y.z.jar                                -rw-r--r-- gattoslab gattoslab-group
#            pid.txt                                            -rw-r--r-- gattoslab gattoslab-group
#            keystore.p12                                       -r-------- gattoslab gattoslab-group
#            /observability                                     drwxr-xr-x gattoslab gattoslab-group
#               ...                                             drwxr-xr-x gattoslab gattoslab-group , -rw-r--r-- gattoslab gattoslab-group
#
#        /build                                                 drwxr-xr-x root root
#            secrets.env                                        -rw------- root root
#            deploy.sh                                          -rwx------ root root
#            /repository                                        drwxr-xr-x root root
#                [git repo...]                                  drwxr-xr-x root root , -rwxr-xr-x root root , -rw-r--r-- root root

##
function await_process_termination () {

    for i in {1..4}; do

        if ! ps -p "$1" > /dev/null; then return 0; fi

        echo "$2"
        sleep 5s

    done

    return 1
}

function delete_if_present_and_log () {

    if [ -e "$1" ]; then rm -f "$1" > /dev/null;
    else echo "$2"; fi
}

MAIN_CLASS="io.github.clamentos.gattoslab.Application"
SECRETS_FILE_NAME="secrets.env"
PID_FILE_NAME="pid.txt"
JAR_PREFIX="gattoslab"
GIT_URL="https://github.com/Clamentos/GattosLab.git"
BASE_DIR="/GattosLab"
BUILD_DIR="$BASE_DIR"/build
REPO_DIR="$BUILD_DIR"/repository
RUN_DIR="$BASE_DIR"/run
SECRETS_FILE="$BUILD_DIR"/"$SECRETS_FILE_NAME"
APP_USER="gattoslab"
APP_USER_GROUP="gattoslab-group"
BOLD_PRINT="\e[1;37m"
RED_PRINT="\e[1;31m"
YELLOW_PRINT="\e[1;33m"
GREEN_PRINT="\e[1;32m"
RESET_COLORS="\e[0m"

echo -e "$BOLD_PRINT\n=> PROJECT BUILD$RESET_COLORS"
echo -e "--> Cloning the repository\n"
rm -fr "$REPO_DIR"
git clone "$GIT_URL" "$REPO_DIR"

echo -e "\n--> Building the project with mvn\n"
mvn -f "$REPO_DIR"/pom.xml clean package -DskipTests

echo -e "$BOLD_PRINT\n=> PROJECT DEPLOY$RESET_COLORS"
echo "--> Grabbing the generated JAR"
NEW_JAR_FILE="$(find "$REPO_DIR"/target/ -name "$JAR_PREFIX*.jar")"

if [ -z "$NEW_JAR_FILE" ]; then

    echo -e "$RED_PRINT--> JAR not found!$RESET_COLORS"
    exit 1
fi

NEW_JAR_NAME="$(basename "$NEW_JAR_FILE")"

echo "--> Stopping the old application"
OLD_PID=0
PID_FILE="$RUN_DIR"/"$PID_FILE_NAME";

if [ -e "$PID_FILE" ]; then

    OLD_PID="$(cat "$PID_FILE")"

    if [ -n "$OLD_PID" ]; then

        if ps -p "$OLD_PID" > /dev/null; then

            kill -15 "$OLD_PID"

            if ! await_process_termination "$OLD_PID" "---> Waiting for process "$OLD_PID" to terminate gracefully..."; then

                kill -9 "$OLD_PID"

                if ! await_process_termination "$OLD_PID" "---> Waiting for process "$OLD_PID" to terminate..."; then

                    echo -e "$RED_PRINT---> Could not stop "$OLD_PID"!$RESET_COLORS"
                    exit 1
                fi
            fi

            echo "---> Process "$OLD_PID" stopped"

        else

            echo "---> No previous process running"
        fi

    else

        echo -e "$YELLOW_PRINT---> "$PID_FILE_NAME" file was empty, continuing...$RESET_COLORS"
    fi

else

    echo -e "$YELLOW_PRINT---> "$PID_FILE_NAME" file does not exist, continuing...$RESET_COLORS"
fi

echo "--> Deleting the old files"
OLD_JAR_FILE="$(find "$RUN_DIR"/ -name "$JAR_PREFIX*.jar")"
delete_if_present_and_log "$OLD_JAR_FILE" "---> Old JAR was not present"
delete_if_present_and_log "$PID_FILE" "---> Old "$PID_FILE_NAME" was not present"

echo "--> Copying "$NEW_JAR_NAME" into run directory"
cp "$NEW_JAR_FILE" "$RUN_DIR"
chown "$APP_USER":"$APP_USER_GROUP" "$RUN_DIR"/"$NEW_JAR_NAME"
jar --update --file "$RUN_DIR"/"$NEW_JAR_NAME" --main-class "$MAIN_CLASS"

echo "--> Copying $SECRETS_FILE into run directory"
cp "$SECRETS_FILE" "$RUN_DIR"
chown "$APP_USER":"$APP_USER_GROUP" "$RUN_DIR"/"$SECRETS_FILE_NAME"
chmod 400 "$RUN_DIR"/"$SECRETS_FILE_NAME"

echo "--> Starting the new application"
cd "$RUN_DIR"
setcap cap_net_bind_service=+ep "$(readlink -f /usr/bin/java)"

runuser -u "$APP_USER" -- bash -c "

    set -a
    source "$RUN_DIR"/"$SECRETS_FILE_NAME"
    exec java -XX:+UnlockExperimentalVMOptions -Xms128M -Xmx1024M -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+OptimizeStringConcat -XX:+UseCompressedOops -XX:+UseCompactObjectHeaders -XX:+AlwaysPreTouch -XX:+ExitOnOutOfMemoryError -jar "$RUN_DIR"/"$NEW_JAR_NAME" PROD
" &

echo "--> Waiting for the app to start"

NEW_PID=0
PID_FILE_EXISTS=0

for i in {1..4}; do

    if [ -e "$PID_FILE" ]; then

        PID_FILE_EXISTS=1
        break
    fi

    sleep 2s

done

if [ "$PID_FILE_EXISTS" -eq 1 ]; then

    for i in {1..4}; do

        NEW_PID="$(cat "$PID_FILE")"

        if [ -n "$NEW_PID" ]; then

            if ps -p "$NEW_PID" > /dev/null; then

                echo -e "$GREEN_PRINT=> Deploy complete with PID: "$NEW_PID"$RESET_COLORS"
                rm "$RUN_DIR"/"$SECRETS_FILE_NAME"
                exit 0
            fi
        fi

        sleep 2s

    done
fi

if [ "$PID_FILE_EXISTS" -eq 1 ]; then echo -e "$RED_PRINT---> Startup failed, missing new "$PID_FILE_NAME"$RESET_COLORS";
else echo -e "$RED_PRINT---> Startup failed, check logs$RESET_COLORS"; fi

rm "$RUN_DIR"/"$SECRETS_FILE_NAME"
exit 1

##

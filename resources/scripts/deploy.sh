#!/bin/bash

#/root
#    /GattosLab
#        /run
#            gattoslab-x.y.z.jar
#            console_out.log
#            fallback_logs.log
#            pid.txt
#        /build
#            secrets.env
#            deploy.sh
#            /repository
#                [git repo: standard Spring structure...]

function await_process_termination () {

    for i in {1..4}; do

        if ! ps -p "$1" > /dev/null; then return 0; fi

        echo "$2"
        sleep 1s

    done

    return 1
}

function delete_if_present_and_log () {

    if [ -e "$1" ]; then rm -f "$1" > /dev/null;
    else echo "$2"; fi
}

BASE_DIR="/root/GattosLab"
BUILD_DIR="$BASE_DIR/build"
REPO_DIR="$BUILD_DIR/repository"
RUN_DIR="$BASE_DIR/run"
SECRETS_FILE="$BUILD_DIR/secrets.env"
APP_USER="gattoslab"
BOLD_PRINT="\e[1;37m"
RED_PRINT="\e[1;31m"
YELLOW_PRINT="\e[1;33m"
GREEN_PRINT="\e[1;32m"
RESET_COLORS="\e[0m"

echo -e "$BOLD_PRINT=> PROJECT BUILD$RESET_COLORS"
echo "--> Cloning the repository"
rm -fr "$REPO_DIR"
git clone https://github.com/Clamentos/GattosLab.git "$REPO_DIR"

echo "--> Building the project with mvn"
echo ""
mvn -f "$REPO_DIR"/pom.xml clean package -DskipTests

echo ""
echo -e "$BOLD_PRINT=> PROJECT DEPLOY$RESET_COLORS"
echo "--> Grabbing the generated JAR"
NEW_JAR_FILE="$(find "$REPO_DIR"/target/ -name "gattoslab*.jar")"

if [ -z "$NEW_JAR_FILE" ]; then

    echo -e "$RED_PRINT--> JAR not found!$RESET_COLORS"
    exit 1
fi

NEW_JAR_NAME="$(basename "$NEW_JAR_FILE")"

echo "--> Stopping the old application"
OLD_PID=0
PID_FILE="$RUN_DIR"/pid.txt;

if [ -e "$PID_FILE" ]; then

    OLD_PID="$(cat "$PID_FILE")"

    if [ -n "$OLD_PID" ]; then

        if ps -p "$OLD_PID" > /dev/null; then

            kill -15 "$OLD_PID"

            if ! await_process_termination "$OLD_PID" "---> Waiting for process $OLD_PID to terminate gracefully..."; then

                kill -9 "$OLD_PID"

                if ! await_process_termination "$OLD_PID" "---> Waiting for process $OLD_PID to terminate..."; then

                    echo -e "$RED_PRINT---> Could not stop $OLD_PID!$RESET_COLORS"
                    exit 1
                fi
            fi

            echo "---> Process $OLD_PID stopped"

        else

            echo "---> No previous process running"
        fi

    else

        echo -e "$YELLOW_PRINT---> pid.txt file was empty, continuing...$RESET_COLORS"
    fi

else

    echo -e "$YELLOW_PRINT---> pid.txt file does not exist, continuing...$RESET_COLORS"
fi

echo "--> Deleting the old files"
OLD_JAR_FILE="$(find "$RUN_DIR"/ -name "gattoslab*.jar")"
delete_if_present_and_log "$OLD_JAR_FILE" "---> Old JAR was not present"
delete_if_present_and_log "$PID_FILE" "---> Old pid.txt was not present"

echo "--> Copying $NEW_JAR_NAME into run directory"
cp "$NEW_JAR_FILE" "$RUN_DIR"
chown "$APP_USER:$APP_USER" "$RUN_DIR/$NEW_JAR_NAME"

# Launch the new application and wait for a successful start
echo "--> Starting the new application"
cd "$RUN_DIR"

(
    set -a
    source "$SECRETS_FILE"
    runuser "$APP_USER" -c 'java -jar "$RUN_DIR/$NEW_JAR_NAME" --spring.profiles.active=prod -XX:+UnlockExperimentalVMOptions -Xmx1024M -XX:+UseZGC -XX:+ZGenerational -XX:+UseStringDeduplication -XX:+OptimizeStringConcat -XX:+UseCompressedOops -XX:+UseCompressedClassPointers -XX:+UseCompactObjectHeaders -XX:+AlwaysPreTouch -XX:+UseHugeTLBFS -XX:+UseSuperWord -XX:+ExitOnOutOfMemoryError -XX:-FlightRecorder' &
)

echo "--> Waiting for the app to start"

NEW_PID=0
PID_FILE_EXISTS=0

for i in {1..4}; do

    if [ -e "$PID_FILE" ]; then

        PID_FILE_EXISTS=1
        break
    fi

    sleep 1s

done

if [ "$PID_FILE_EXISTS" -eq 1 ]; then

    for i in {1..4}; do

        NEW_PID="$(cat "$PID_FILE")"

        if [ -n "$NEW_PID" ]; then

            if ps -p "$NEW_PID" > /dev/null; then

                echo -e "$GREEN_PRINT=> Deploy complete with PID: $NEW_PID$RESET_COLORS"
                exit 0
            fi
        fi

        sleep 1s

    done
fi

if [ "$PID_FILE_EXISTS" -eq 1 ]; then echo -e "$RED_PRINT---> Startup failed, missing new pid.txt$RESET_COLORS";
else echo -e "$RED_PRINT---> Startup failed, check logs$RESET_COLORS"; fi

exit 1

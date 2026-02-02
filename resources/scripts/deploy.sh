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

#BASE_DIR="/root/GattosLab"
BASE_DIR="/home/enrico/Desktop/Prova/GattosLab"
BUILD_DIR="$BASE_DIR/build"
REPO_DIR="$BUILD_DIR/repository"
RUN_DIR="$BASE_DIR/run"
SECRETS_FILE="$BUILD_DIR/secrets.env"
APP_USER="enrico"
BOLD_PRINT="\e[1;37m"
RED_PRINT="\e[1;31m"
YELLOW_PRINT="\e[1;33m"
GREEN_PRINT="\e[1;32m"
RESET_COLORS="\e[0m"

# Get the project source code
echo -e "$BOLD_PRINT=> PROJECT BUILD$RESET_COLORS"
echo "-> Cloning the repository"
cd "$BUILD_DIR"
rm -fr ./repository
git clone https://github.com/Clamentos/GattosLab.git repository

# Build the project
echo "-> Building the project"
cd "$REPO_DIR"
mvn clean package -DskipTests
echo "-> Grabbing the generated JAR"
JAR_FILE="$(ls target/gattoslab*.jar | head -n 1)"

if [ -z "$JAR_FILE" ]; then

    echo -e "$RED_PRINT    -> JAR not found!$RESET_COLORS"
    exit 1
fi

JAR_NAME="$(basename "$JAR_FILE")"

# Stop currently running application
echo -e "$BOLD_PRINT=> PROJECT DEPLOY$RESET_COLORS"
echo "-> Stopping the old application"

OLD_PID=0

if [ -e "$RUN_DIR/pid.txt" ]; then

    OLD_PID="$(cat $RUN_DIR/pid.txt)"

    if [ -n "$OLD_PID" ]; then

        if ps -p "$OLD_PID" > /dev/null; then

            kill -15 "$OLD_PID"

            if ! await_process_termination "$OLD_PID" "-> Waiting for process $OLD_PID to terminate gracefully..."; then

                kill -9 "$OLD_PID"

                if ! await_process_termination "$OLD_PID" "-> Waiting for process $OLD_PID to terminate..."; then

                    echo -e "$RED_PRINT-> Could not stop $OLD_PID!$RESET_COLORS"
                    exit 1
                fi
            fi

            echo "-> Process $OLD_PID stopped"

        else

            echo "-> No previous process running"
        fi

    else

        echo -e "$YELLOW_PRINT-> pid.txt file was empty, continuing...$RESET_COLORS"
    fi

else

    echo -e "$YELLOW_PRINT-> pid.txt file does not exist, continuing...$RESET_COLORS"
fi

# Deploy the new JAR
echo "-> Deleting the old JAR"
OLD_JAR="$RUN_DIR/gattoslab*.jar"

if [ -e "$OLD_JAR" ]; then

    rm -f "OLD_JAR" > /dev/null

else

    echo "-> Old JAR was not present"
fi

echo "-> Copying $JAR_NAME into run directory"
cp "$JAR_FILE" "$RUN_DIR/"
chown "$APP_USER:$APP_USER" "$RUN_DIR/$JAR_NAME"

# Launch the new application and wait for a successful start
echo "-> Starting the new application"
cd "$RUN_DIR"
java -jar "$RUN_DIR/$JAR_NAME" --spring.profiles.active=dev &

#(
#    set -a
#    source "$SECRETS_FILE"
#    sudo -u "$APP_USER" java -jar "$RUN_DIR/$JAR_NAME" --spring.profiles.active=prod -Xmx1024M &
#)

echo "-> Waiting for the app to start"

NEW_PID=0
PID_FILE_EXISTS=0

for i in {1..4}; do

    if [ ! -e "$RUN_DIR/pid.txt" ]; then

        PID_FILE_EXISTS=1
        break
    fi

    sleep 1s

done

if [ "$PID_FILE_EXISTS" -eq 1 ]; then

    for i in {1..4}; do

        NEW_PID="$(cat $RUN_DIR/pid.txt)"

        if [ -n "$NEW_PID" ]; then

            if ps -p "$NEW_PID" > /dev/null; then

                echo -e "$GREEN_PRINT=> Deploy complete with PID: $NEW_PID$RESET_COLORS"
                exit 0
            fi
        fi

        sleep 1s

    done
fi

echo -e "$RED_PRINT-> Startup failed, check logs$RESET_COLORS"
exit 1




function await_process_termination () {

    for i in {1..4}; do

        if !ps -p "$1" > /dev/null; then

            return 0
        fi

        echo "$2"
        sleep 1s

    done

    return 1
}

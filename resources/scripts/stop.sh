#!/bin/bash
if test -e $TARGET_DIR/pid.txt; then

    pid=$(cat $TARGET_DIR/pid.txt)

    if [ ! -z "$pid" ]; then

        kill -15 $pid

        while ps -p $pid > /dev/null; do

            echo "Waiting for process " $pid " to terminate..."
            sleep 1s

        done;

        echo "Process " $pid " stopped"

    else

        echo "pid.txt file was empty"
    fi
fi

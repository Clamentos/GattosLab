#! /bin/bash
if test -e current_pid.txt; then

    pid=$(cat current_pid.txt)

    if [ ! -z "$pid" ]; then

        kill -15 $pid

        while ps -p $pid > /dev/null; do

            echo "Waiting for process " $pid " to terminate..."
            sleep 1s

        done;

        echo "Process " $pid " stopped"

    else

        echo "current_pid.txt file was empty"
    fi
fi

rm -f -- ./nohup.out
rm -f -- ./current_pid.txt

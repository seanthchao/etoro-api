#!/bin/bash

# turn on bash's job control
set -m

# Start the primary process and put it in the background
#echo "wait..."
#sleep 30
#echo "start..."
# ls -altr
# ls -altr /bin
# ls -altr /home/browser
# pwd
java -jar /bin/runner/run.jar &
brave-browser --remote-debugging-port=9224 --incognito https://www.etoro.com/login



# Start the helper process
# ./my_helper_process

#!/bin/bash
# for i in $(seq 71 80)
# do
#   echo "$i"
#   podman build --build-arg SCRAPER_NAME=browser_"$i" -t etoro_scraper_"$i" . &
# done
port_base=8046
port=$(expr $port_base + $1)
echo $port
podman run --name etoro_trader_1 -p 8129:8130 --rm -it --cap-add SYS_ADMIN -e DISPLAY=:80 -v /tmp/.X11-unix:/tmp/.X11-unix -v brave_browser_home:/home/browser_trader_1 --shm-size 1GB --tmpfs /tmp:size=20M etoro_trader_1:latest &
sleep 15

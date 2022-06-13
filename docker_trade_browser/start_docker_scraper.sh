#!/bin/bash
# for i in $(seq 1 3)
# do
#   echo "$i"
#   podman build --build-arg SCRAPER_NAME=trade_browser_"$i" -t etoro_trade_"$i" . &
# done
port_base=8129
xvfb_port_base=80
port=$(expr $port_base + $1)
xvfb_port=$(expr $xvfb_port_base + $1)
echo $port
podman run --name etoro_trade_"$1" -p "$port":8130 --rm -it --cap-add SYS_ADMIN -e DISPLAY=:"$xvfb_port" -v /tmp/.X11-unix:/tmp/.X11-unix -v brave_browser_home:/home/browser_"$1" --shm-size 1GB --tmpfs /tmp:size=20M etoro_trade_"$1":latest &
sleep 5

#!/bin/bash
# snap.sh out.jpg [crop]  - grab the game area (window at 0,60 800x480)
export DISPLAY=:99
import -window root /tmp/snap.png; convert /tmp/snap.png -crop ${2:-854x480+85+144} -quality 88 "$1"

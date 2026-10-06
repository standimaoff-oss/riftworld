#!/usr/bin/env bash
# Capture the headless client display. Usage: snap.sh out.png [WxH+X+Y crop]
set -euo pipefail
OUT=${1:?usage: snap.sh out.png [WxH+X+Y crop]}
DISPLAY_NUM=${DISPLAY_NUM:-99}
export DISPLAY=":$DISPLAY_NUM"
if [[ $# -ge 2 ]]; then
  import -window root -crop "$2" +repage "$OUT"
else
  import -window root "$OUT"
fi
identify "$OUT"

#!/usr/bin/env bash
# v0.50.0: поставить X-пакеты для headless-клиента Minecraft.
#
# На CI runner'е apt периодически отваливается: DNS не резолвит зеркала,
# и шаг «Install the packages» падает, хотя сам мод тут ни при чём.
# Раньше это выглядело как «проба закона отменена», хотя код был в порядке.
#
# Поэтому ставим с ретраями и с apt-get update перед каждой попыткой:
# сеть успевает восстановиться, а лишние пакеты не считаются ошибкой.
set -uo pipefail

PACKAGES=(
  xvfb openbox x11-utils xdotool imagemagick
  libgl1 libglx-mesa0 libxi6 libxxf86vm1 libxfixes3 libxrender1
  libxkbcommon0 libsm6 libopenal1
)

# Что должно быть на месте после установки — по нему и судим, что пакеты встали.
REQUIRED_BINS=(Xvfb openbox xdotool import convert)

install_once() {
  sudo apt-get update -qq >/dev/null 2>&1 || true
  sudo DEBIAN_FRONTEND=noninteractive apt-get install -y -qq \
    "${PACKAGES[@]}" >/dev/null 2>&1 || true
}

missing_bins() {
  local missing=()
  for bin in "${REQUIRED_BINS[@]}"; do
    command -v "$bin" >/dev/null 2>&1 || missing+=("$bin")
  done
  printf '%s\n' "${missing[@]:-}"
}

for attempt in 1 2 3 4 5; do
  install_once
  missing=$(missing_bins)
  if [ -z "$missing" ]; then
    echo "::notice title=CI packages::все пакеты для headless-клиента на месте"
    exit 0
  fi
  echo "::warning title=CI packages::попытка $attempt: не хватает ${missing} — повторяем"
  sleep $((attempt * 15))
done

missing=$(missing_bins)
if [ -n "$missing" ]; then
  echo "::error title=CI packages::не удалось поставить ${missing} после пяти попыток (сеть apt)"
  exit 1
fi
exit 0

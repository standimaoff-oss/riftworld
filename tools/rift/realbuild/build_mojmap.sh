#!/bin/bash
# Собирает minecraft-mojmap.jar (официальные имена классов + mojmap имена членов) из серверной установки Forge.
# Ванильный server-...-srg.jar даёт базу, forge-...-server.jar — патчи Forge (оверлей поверх базы).
# ART применяет маппинги в два прохода: артефакт с маппингом "класс в себя" члены не переименовывает.
set -euo pipefail
cd /tmp/mcserver
J=${JAVA:-/usr/local/lib/python3.11/dist-packages/jdk4py/java-runtime/bin/java}
A=libraries/net/minecraftforge/ForgeAutoRenamingTool/0.1.22/ForgeAutoRenamingTool-0.1.22-all.jar
VAN=libraries/net/minecraft/server/1.20.1-20230612.114412/server-1.20.1-20230612.114412-srg.jar
FORGE=libraries/net/minecraftforge/forge/1.20.1-47.4.3/forge-1.20.1-47.4.3-server.jar
# srg-jar обязателен в --lib: иерархия оверлея без него неполна и ART падает с NPE на Transformation.class
LIBS=$(find libraries -name "*.jar" ! -name "*ForgeAutoRenamingTool*" ! -name "forge-*-server.jar" \
        | sed 's/^/--lib /' | tr '\n' ' ')

python3 make_mappings.py

for pair in "van:$VAN" "forge:$FORGE"; do
  name=${pair%%:*}; jar=${pair#*:}
  $J -jar $A --input "$jar" --output "${name}_step1.jar" --map compile_pass1.tsrg $LIBS --threads 1 >"/tmp/art_${name}1.log" 2>&1
  $J -jar $A --input "${name}_step1.jar" --output "${name}_mojmap.jar" --map compile_pass2.tsrg $LIBS --threads 1 >"/tmp/art_${name}2.log" 2>&1
  echo "$name: $(unzip -l ${name}_mojmap.jar | grep -c '\.class$') классов"
done

python3 - <<'PY'
import zipfile
out = zipfile.ZipFile("minecraft-mojmap.jar", "w", zipfile.ZIP_DEFLATED)
seen, counts = set(), {}
for src, tag in (("forge_mojmap.jar", "оверлей"), ("van_mojmap.jar", "ваниль")):
    with zipfile.ZipFile(src) as z:
        for it in z.infolist():
            if it.is_dir() or it.filename in seen:
                continue
            seen.add(it.filename)
            out.writestr(it, z.read(it.filename))
            counts[tag] = counts.get(tag, 0) + 1
out.close()
print("слито:", counts, "всего:", len(seen))
PY

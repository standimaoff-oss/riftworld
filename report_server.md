# CI, шаг 1: сборка jar + запуск Forge-сервера

- jar: `mod/build/libs/riftworld-0.40.3.jar`
- сервер: `[05Nov2026 02:40:15.096] [Server thread/INFO] [net.minecraft.server.dedicated.DedicatedServer/]: Done (2.692s)! For help, type "help"`
- провалов проверок: **0**
- строк Exception/ERROR в latest.log: **0**
- сервер оставлен живым для шага клиента: **1**

## forge mods
```
Mod List: 
• minecraft server-1.20.1-20230612.114412-srg.jar : minecraft (1.20.1) - 1
• minecraft forge-1.20.1-47.4.23-universal.jar : forge (47.4.23) - 1
• mods_folder dynamictrees.jar : dynamictrees (1.20.1-1.4.10) - 1
• mods_folder riftworld-0.40.3.jar : riftworld (0.40.3) - 1
• mods_folder riftworld-dt-0.40.3.jar : riftworlddt (0.40.3) - 1

```
## tps
```
Dim minecraft:overworld (minecraft:overworld): Mean tick time: 4.627 ms. Mean TPS: 20.000
Dim riftworld:elisar (riftworld:elisar): Mean tick time: 0.093 ms. Mean TPS: 20.000
Dim riftworld:transit (riftworld:transit): Mean tick time: 0.020 ms. Mean TPS: 20.000
Dim minecraft:the_nether (minecraft:the_nether): Mean tick time: 0.016 ms. Mean TPS: 20.000
Dim minecraft:the_end (minecraft:the_end): Mean tick time: 0.025 ms. Mean TPS: 20.000
Overall: Mean tick time: 4.819 ms. Mean TPS: 20.000

```
## аддон DT
```
riftdt handler=invalid ok=21 items=11 wrong_ns=0 missing=0 item_have=sizo_seed,ambra_seed,garden_oak_seed,lilac_seed,giant_sizo_seed,crystal_birch_seed,sizo_branch,ambra_branch,garden_oak_branch fam=sizo,ambra,garden_oak,lilac,crystal_birch sp=crystal_birch,sizo,ambra,garden_oak,lilac,giant_sizo lv=sizo,ambra,garden_oak,lilac,crystal_birch diag=crystal_birch[sp=1 fam=1 br=riftworlddt:crystal_birch_branch brf=true lv=1 dyn=riftworlddt:crystal_birch_leaves] sizo[sp=1 fam=1 br=riftworlddt:sizo_branch brf=true lv=1 dyn=riftworlddt:sizo_leaves] ambra[sp=1 fam=1 br=riftworlddt:ambra_branch brf=true lv=1 dyn=riftworlddt:ambra_leaves] garden_oak[sp=1 fam=1 br=riftworlddt:garden_oak_branch brf=true lv=1 dyn=riftworlddt:garden_oak_leaves] lilac[sp=1 fam=1 br=riftworlddt:lilac_branch brf=true lv=1 dyn=riftworlddt:lilac_leaves] giant_sizo[sp=1 fam=1 br=riftworlddt:sizo_branch brf=true lv=1 dyn=riftworlddt:sizo_leaves]

```
## обычный мир (level-type=minecraft:normal)
```
normal world: server BOOTED (rc=0), no feature order cycle
```
## ошибки
```
```

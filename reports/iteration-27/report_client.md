# CI, шаг 2: настоящий клиент Minecraft в Элисаре

- клиент: ForgeGradle runClient под Xvfb (llvmpipe, без GPU)
- воды в колонке (696,y,696): **57** блоков
- море: дно y=10, вода 51 блоков, глубочайшее дно y=1
- пещеры: 8491 колонок, воды 202346 / воздуха 11597 блоков (у свода 6140, висячего 915)
- провалов проб: **0**
- строк Exception/ERROR в логе клиента (без звука/наратора): **0**

## пробы
```
  /riftsea raw: riftsea floor=10 water=51 sea=62 min=1 max=15 cols=9409 caves=8491 wet=202346 air=11597 reef=1981 plants=1768 corals=141 float=915 ceilair=6140 bottom=-62 caveat=625,5,570  
  /riftsea raw: riftsea floor=10 water=51 sea=62 min=1 max=15 cols=9409 caves=8491 wet=202346 air=11597 reef=1981 plants=1768 corals=141 float=915 ceilair=6140 bottom=-62 caveat=625,5,570  
  OK   вода на глубине (696,56,696) = elisar_water
  /riftsea raw: riftsea floor=5 water=56 sea=62 min=-8 max=14 cols=9409 caves=8497 wet=164874 air=13311 reef=2239 plants=1406 corals=168 float=1040 ceilair=7498 bottom=-62 caveat=650,3,600  
  OK   донный блок: elisar_seabed на 696,5,696
  OK   кромка дна из /riftsea: y=10
  OK   толщина воды под рифом: 51 блоков (нужно >=44)
  OK   колонок с пещерами в радиусе проб: 8491
  OK   пещеры ЗАТОПЛЕНЫ: воды 94% объёма полостей (остальное — воздушные карманы)
  OK   воздух в пещерах прижат к своду: висячих пузырей 7% (нужно <=12%)
  OK   воздух под сводами (под камнем): 52% всего воздуха пещер
  OK   вода внутри пещеры (625,5,570) = elisar_water
  OK   пещера найдена на y=5 (в 57 блоках под уровнем моря)
  OK   рифовый сад: coral_brain at 600,11,669
  OK   вода над рифом (600,14,666) = elisar_water
  /riftcoral raw: riftcoral at=528,11,668 h=6 blocks=107 cols=49 tips=46 floor=10 sea=62 cam=534,20,674 camlow=538,12,678  
  коралл: at=528,11,668 h=6 блоков=107 колонок=49 украшений=46
  OK   коралл — ВЫСОКОЕ растение: 6 блоков в высоту
  OK   коралл из МНОГИХ блоков: 107 блоков, 49 колонок
  OK   у растения есть ветви/плита: 49 колонок
  OK   на ветвях есть кораллы-украшения: 46
  камера коралла: cam=534,20,674 camlow=538,12,678
  воды в колонке: 57 блоков
  OK   рыбы заспавнились сами (SeaLifeKeeper)
  summon ^-2 ^0 ^4 {NoAI:1b,Silent:1b,sf_variant:0} -> Summoned new Elisar Sea Fish
  summon ^-0.6 ^0.6 ^5 {NoAI:1b,Silent:1b,sf_variant:0} -> Summoned new Elisar Sea Fish
  summon ^0.8 ^-0.6 ^6 {NoAI:1b,Silent:1b,sf_variant:1} -> Summoned new Elisar Sea Fish
  summon ^2.2 ^0.2 ^7 {NoAI:1b,Silent:1b,sf_variant:2} -> Summoned new Elisar Sea Fish
  summon ^-3.2 ^1.2 ^8 {NoAI:1b,Silent:1b,sf_variant:3} -> Summoned new Elisar Sea Fish
  summon ^3.4 ^1.6 ^9 {NoAI:1b,Silent:1b,sf_variant:4} -> Summoned new Elisar Sea Fish
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,sf_variant:5} -> Summoned new Elisar Sea Fish
  summon ^1.1 ^0.2 ^3 {NoAI:1b,Silent:1b,sf_variant:6} -> Summoned new Elisar Sea Fish
  summon ^-1.2 ^0.1 ^2.4 {NoAI:1b,Silent:1b,sf_variant:5} -> Summoned new Elisar Sea Fish
  OK   рыбы в мире есть и тикают
  summon ^0 ^0 ^3 {NoAI:1b,Silent:1b,sf_variant:0} -> Summoned new Elisar Sea Fish
  summon ^0.8 ^0.7 ^3.4 {NoAI:1b,Silent:1b,sf_variant:1} -> Summoned new Elisar Sea Fish
  summon ^-0.9 ^-0.3 ^4.2 {NoAI:1b,Silent:1b,sf_variant:2} -> Summoned new Elisar Sea Fish
  summon ^0.4 ^0.9 ^4.8 {NoAI:1b,Silent:1b,sf_variant:3} -> Summoned new Elisar Sea Fish
  summon ^0 ^0 ^3 {NoAI:1b,Silent:1b,sf_variant:7} -> Summoned new Elisar Sea Fish
  summon ^1.0 ^0.9 ^3.6 {NoAI:1b,Silent:1b,sf_variant:8} -> Summoned new Elisar Sea Fish
  summon ^-0.9 ^-0.5 ^4.4 {NoAI:1b,Silent:1b,sf_variant:0} -> Summoned new Elisar Sea Fish
  summon ^0 ^0 ^2.8 {NoAI:1b,Silent:1b,sf_variant:9} -> Summoned new Elisar Sea Fish
  summon ^1.1 ^0.6 ^3.4 {NoAI:1b,Silent:1b,sf_variant:10} -> Summoned new Elisar Sea Fish
  summon ^-1.0 ^-0.4 ^4.0 {NoAI:1b,Silent:1b,sf_variant:5} -> Summoned new Elisar Sea Fish
  summon ^0 ^0 ^3 {NoAI:1b,Silent:1b,sf_variant:0} -> Summoned new Elisar Sea Fish
  summon ^0.7 ^0.9 ^3.6 {NoAI:1b,Silent:1b,sf_variant:1} -> Summoned new Elisar Sea Fish
  summon ^-0.9 ^-0.5 ^4.4 {NoAI:1b,Silent:1b,sf_variant:4} -> Summoned new Elisar Sea Fish
  OK   у всех 11 видов есть JSON-модель и текстура
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[90f,0f],sf_variant:0} -> Summoned new Elisar Sea Fish
  рыба silver: Elisar Sea Fish has the following entity data: [696.4903145620211d, 39.90928345322609d, 693.9113092637284d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[90f,0f],sf_variant:1} -> Summoned new Elisar Sea Fish
  рыба gold: Elisar Sea Fish has the following entity data: [696.4903145620211d, 39.90928345322609d, 693.9113092637284d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[90f,0f],sf_variant:2} -> Summoned new Elisar Sea Fish
  рыба striped: Elisar Sea Fish has the following entity data: [696.4903145620211d, 39.90928345322609d, 693.9113092637284d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[45f,0f],sf_variant:3} -> Summoned new Elisar Sea Fish
  рыба jelly: Elisar Sea Fish has the following entity data: [696.5d, 39.90928345322609d, 693.9015917181969d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[45f,0f],sf_variant:4} -> Summoned new Elisar Sea Fish
  рыба ray: Elisar Sea Fish has the following entity data: [696.4903145620211d, 39.90928345322609d, 693.9113092637284d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[90f,0f],sf_variant:5} -> Summoned new Elisar Sea Fish
  рыба angler: Elisar Sea Fish has the following entity data: [696.4903145620211d, 39.90928345322609d, 693.9113092637284d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[90f,0f],sf_variant:6} -> Summoned new Elisar Sea Fish
  рыба eel: Elisar Sea Fish has the following entity data: [696.5d, 39.90928345322609d, 693.9015917181969d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[90f,0f],sf_variant:7} -> Summoned new Elisar Sea Fish
  рыба surgeon: Elisar Sea Fish has the following entity data: [696.5d, 39.90928345322609d, 693.9015917181969d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[90f,0f],sf_variant:8} -> Summoned new Elisar Sea Fish
  рыба turtle: Elisar Sea Fish has the following entity data: [696.4903145620211d, 39.90928345322609d, 693.9113092637284d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[90f,0f],sf_variant:9} -> Summoned new Elisar Sea Fish
  рыба gloom: Elisar Sea Fish has the following entity data: [696.4903145620211d, 39.90928345322609d, 693.9113092637284d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[90f,0f],sf_variant:10} -> Summoned new Elisar Sea Fish
  рыба sting: Elisar Sea Fish has the following entity data: [696.5d, 39.90928345322609d, 693.9015917181969d]
  OK   клиент не жаловался на geo-модели
  /rifttree raw: rifttree at=-25,105,148 kind=sizo h=13 logs=100 branches=59 leaves=337 dist=90 loaded=true cam=-14,124,148 camtop=-14,134,148
  OK   дерево найдено: -25,105,148 (ветвей: 59)
  OK   у дерева есть ветки: 59 блоков вне ствола
  /rifttree повтор: rifttree at=27,107,129 kind=sizo h=10 logs=56 branches=35 leaves=406 dist=34 loaded=true cam=38,123,129 camtop=38,133,129
  OK   дерево с ветками и кроной: ветвей 35, листвы 406
  камера дерева: cam=38,123,129 camtop=38,133,129
  give riftworlddt:sizo_seed -> Gave 1 [Sizo Seed] to TestPlayer
  OK   DT видит семена аддона (riftworlddt:sizo_seed)
  OK   DT видит коралловые семена (riftworlddt:coral_staghorn_seed)
  упоминаний riftworlddt:sizo в логе: 0
0
  /dynamictrees registry family list -> dynamictrees:null dynamictrees:warped dynamictrees:spruce dynamictrees:oak dynamictrees:mangrove dynamictrees:jungle dynamictrees:dark_oak dynamictrees:crimson dynamictrees:cherry dynamictrees:birch dynamictrees:acacia r
  /dynamictrees registry species list -> dynamictrees:null dynamictrees:red_mushroom dynamictrees:brown_mushroom dynamictrees:warped dynamictrees:tall_birch dynamictrees:swamp_oak dynamictrees:spruce dynamictrees:oak_undergrowth dynamictrees:oak dynamictrees:me
  OK   у DT есть семья riftworlddt:sizo
  OK   у DT есть семья riftworlddt:ambra
  OK   у DT есть семья riftworlddt:coral
  OK   у DT есть вид riftworlddt:sizo
  OK   у DT есть вид riftworlddt:ambra
  OK   у DT есть вид riftworlddt:giant_sizo
  OK   у DT есть вид riftworlddt:coral_staghorn
  OK   у DT есть вид riftworlddt:coral_table
  OK   у DT есть вид riftworlddt:coral_whip
  [DT] секция дерева
  /riftdt diag -> coral_whip[sp=1 fam=1 br=riftworlddt:coral_branch brf=true lv=1 dyn=riftworlddt:coral_whip_leaves] coral_staghorn[sp=1 fam=1 br=riftworlddt:coral_branch brf=true lv=1 dyn=riftworlddt:coral_staghorn_leaves] coral_table[sp=1 fam=1 br=riftworlddt:coral_branch brf=true lv=1 dyn=riftworlddt:coral_table_leaves] sizo[sp=1 fam=1 br=riftworlddt:sizo_branch brf=true lv=1 dyn=riftworlddt:sizo_leaves] ambra[sp=1 fam=1 br=riftwor
  dynamictrees settree 9,105,151 riftworlddt:sizo -> Successfully set tree at position 9, 105, 151 to species species.riftworlddt.sizo with JoCode JJJ0JP1JJ+ZJOOPvnRJOOPvn.  
  /riftdtat у посадки дерева -> riftdtat at=9,105,151 dt=120 dy=0 lv=86 air=853 water=0 top=117:riftworlddt:sizo_leaves col=rooty_elisar_soil,sizo_branchx6,airx4,sizo_leavesx4,air blocks=rooty_elisar_soil=1 sizo_branch=33 sizo_leaves=86 near=elisar_stone=147 elisar_crust=80 elisar_stone_mossy=15
  /dynamictrees gettree -> Species: species.riftworlddt.sizo JoCode: JJJzJJxx98+RJOOPvn1JJ8JP  
  контрольный дуб: Successfully set tree at position -9, 105, 161 to species Oak with JoCode JJJ0JR7Jxx+nXv.  
  /riftdtat у контрольного дуба -> riftdtat at=-9,105,161 dt=1 dy=156 lv=137 air=816 water=0 top=117:dynamictrees:oak_leaves col=rooty_elisar_soil,oak_branchx6,air,oak_leaves,oak_branch,oak_leavesx3,airx3 blocks=rooty_elisar_soil=1 near=elisar_crust=81 el
  саженец + костная мука -> riftgrow at=25,106,143 block=riftworlddt:sizo_sapling grew=8 after=riftworlddt:sizo_branch  
  /riftdtat у саженца -> riftdtat at=25,105,143 dt=75 dy=0 lv=67 air=898 water=0 top=111:riftworlddt:sizo_leaves col=rooty_elisar_soil,sizo_branchx4,sizo_leaves,airx10 blocks=rooty_elisar_soil=1 sizo_branch=7 sizo_leaves=67 near=elisar_stone=168
  OK   саженец аддона вырос в дерево DT: блоков 75 (листвы 67)
  OK   DT вырастил дерево аддона: блоков 120 (ветвей 33, листвы 86)
  [DT] запускаю секцию коралла
  [DT] секция коралла
  OK   площадка коралла расчищена (над грунтом воздух)
  координаты DT-коралла: 606,10,672 (риф 600,10,666)
  dynamictrees settree 606,10,672 riftworlddt:coral_staghorn -> Successfully set tree at position 606, 10, 672 to species species.riftworlddt.coral_staghorn with JoCode JJJ0Jxx769JxPX.  
  /riftdtat у посадки коралла -> riftdtat at=606,10,672 dt=209 dy=0 lv=189 air=282 water=548 top=22:riftworlddt:coral_staghorn_leaves col=rooty_elisar_soil,coral_branchx6,coral_staghorn_leavesx6,elisar_waterx3 blocks=coral_branch=19 coral_staghorn_leaves=189 rooty_elisar_soil=1 near=reef_stone=122 coral_brain=19 coral_cap_pink=13 c
  контроль коралла на суше -> Successfully set tree at position 1, 105, 167 to species species.riftworlddt.coral_staghorn with JoCode JJJ0Jxx769JxPX.  
  /riftdtat коралла на суше -> riftdtat at=1,105,167 dt=213 dy=0 lv=193 air=760 water=2 top=117:riftworlddt:coral_staghorn_leaves col=rooty_elisar_soil,coral_branchx6,coral_staghorn_leavesx6,airx3 blocks=coral_branch=19 coral_stagh
  коралловый саженец на суше -> riftgrow at=5,106,167 block=riftworlddt:coral_staghorn_sapling grew=1 after=minecraft:air  
  OK   DT вырастил ПОДВОДНЫЙ коралл с кроной: блоков 209 (ветвей 19, листвы 189)
  /riftcoral -> riftcoral at=30,9,61 h=6 blocks=220 cols=91 tips=273 floor=8 sea=62 cam=36,18,67 camlow=40,10,71
  [БИОМ] секция садов и рощи
  режим игрока -> Set TestPlayer's game mode to Spectator Mode  
  /riftbiome gardens (поиск) -> riftbiome name=gardens at=-1167,-1191 r=16 top=-2147483648 water=0 garden_grass=0 garden_leaves=0 garden_logs=0 ivy=0 planks=0 lantern=0 struct=- fall=-1202,-1225 wall=-1161,74,-1212:-1161,76,-1201 bl
  /riftbiome gardens (счёт) -> riftbiome name=gardens at=-1167,-1191 r=16 top=101 water=3516 garden_grass=594 garden_leaves=1077 garden_logs=134 ivy=950 planks=0 lantern=0 struct=- fall=-1202,-1225 wall=-1161,74,-1212:-1161,76,-1201 blocks=garden_limestone=8803 elisar_stone=2393 garden_limestone_mossy=1451 elisar_soil=927 elisar_stone_mossy=339 ashlar=134  
  /locate biome waterfall_gardens -> The nearest riftworld:waterfall_gardens is at [-2607, 130, -871] (1475 blocks away)  
  OK   /locate biome waterfall_gardens привёл в сады, не в воду (-2607,130,-871)
  OK   сады: зелёной травы 594 блоков
  OK   сады: воды 3516 блоков (пруды и водопады, без затопления)
  OK   сады: плюща на стенах 950 блоков
  OK   сады: рукотворного нет (досок 0, фонарей 0)
  OK   сады: листвы садовых дубов 1077 блоков
  кадр обрыва: камера -1161 74 -1212 -> взгляд -1161 76 -1201
  игрок после кадра обрыва -> TestPlayer has the following entity data: [-1160.5d, 74.0d, -1211.5d]  
  /riftbiome sizo (поиск) -> riftbiome name=sizo at=-240,841 r=80 top=-2147483648 water=0 garden_grass=0 garden_leaves=0 garden_logs=0 ivy=0 planks=0 lantern=0 struct=carts=0 shrines=0 temples=0 pillar_pairs=1 fall=- wall=- block
  /riftbiome sizo (счёт) -> riftbiome name=sizo at=-240,841 r=80 top=130 water=19683 garden_grass=0 garden_leaves=0 garden_logs=0 ivy=0 planks=2 lantern=0 struct=carts=0 shrines=0 temples=0 pillar_pairs=1 fall=- wall=- blocks=elisar_stone=230801 elisar_stone_mossy=23931 elisar_crust=6436 elisar_grass=4108 elisar_moss=2479 elisar_tall_grass=1228  
  /riftbiome sizo: структуры -> carts=0 shrines=0 temples=0 pillar_pairs=1 fall= (по ТЗ: тележка ~1/3, памятник и храм ~1/7, 1-2 пары столбов)
  /rifttree у рощи -> rifttree at=105,112,-69 kind=sizo h=11 logs=58 branches=35 leaves=327 dist=126 loaded=true cam=116,129,-69 camtop=116,139,-69  
  игрок: TestPlayer has the following entity data: [-229.5d, 144.0d, 871.5d]
  рыбы:  Elisar Sea Fish has the following entity data: [499.86173123146483d, 52.565558580331995d, 662.4569330118267d]
```
## колонка (y; W=вода, S=донный песок, a=воздух/пещера, .=прочее)
```
WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW....
```
## скриншоты
- ci-shots/01_boot.jpg
- ci-shots/02_reef_from_above.jpg
- ci-shots/03_reef_underwater.jpg
- ci-shots/03b_reef_hills.jpg
- ci-shots/03c_reef_level.jpg
- ci-shots/03d_corals_3d.jpg
- ci-shots/04_kelp_forest.jpg
- ci-shots/05_open_sea.jpg
- ci-shots/06_trench.jpg
- ci-shots/07_blue_hole.jpg
- ci-shots/08_cave.jpg
- ci-shots/09_cave_back.jpg
- ci-shots/10_fish_reef.jpg
- ci-shots/10b_fish_tropical.jpg
- ci-shots/11_fish_cave.jpg
- ci-shots/11b_fish_cave_rare.jpg
- ci-shots/12_fish_closeup.jpg
- ci-shots/13_reef_floor.jpg
- ci-shots/14_tree_branches.jpg
- ci-shots/14b_tree_top.jpg
- ci-shots/15_dt_tree.jpg
- ci-shots/15b_dt_tree_top.jpg
- ci-shots/15c_dt_tree_side.jpg
- ci-shots/16_coral_tree.jpg
- ci-shots/16b_coral_tree_low.jpg
- ci-shots/16c_coral_grove.jpg
- ci-shots/16d_dt_coral.jpg
- ci-shots/16e_dt_coral_low.jpg
- ci-shots/40_garden_overview.jpg
- ci-shots/41_garden_terraces.jpg
- ci-shots/42_garden_walls.jpg
- ci-shots/43_garden_ponds.jpg
- ci-shots/44_garden_waterfall.jpg
- ci-shots/45_sizo_grove.jpg
- ci-shots/46_sizo_trunks.jpg
- ci-shots/47_sizo_canopy.jpg
- ci-shots/48_garden_cliff.jpg
- ci-shots/model_00_silver.jpg
- ci-shots/model_01_gold.jpg
- ci-shots/model_02_striped.jpg
- ci-shots/model_03_jelly.jpg
- ci-shots/model_04_ray.jpg
- ci-shots/model_05_angler.jpg
- ci-shots/model_06_eel.jpg
- ci-shots/model_07_surgeon.jpg
- ci-shots/model_08_turtle.jpg
- ci-shots/model_09_gloom.jpg
- ci-shots/model_10_sting.jpg

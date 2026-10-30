# CI, шаг 2: настоящий клиент Minecraft в Элисаре

- клиент: ForgeGradle runClient под Xvfb (llvmpipe, без GPU)
- воды в колонке (696,y,696): **57** блоков
- море: дно y=10, вода 51 блоков, глубочайшее дно y=1
- пещеры: 8491 колонок, воды 202346 / воздуха 11604 блоков (у свода 6146, висячего 915)
- провалов проб: **0**
- строк Exception/ERROR в логе клиента (без звука/наратора): **1**

## пробы
```
  /riftsea raw: riftsea floor=10 water=51 sea=62 min=1 max=15 cols=9409 caves=8491 wet=202346 air=11604 reef=1981 plants=1786 corals=82 float=915 ceilair=6146 bottom=-62 caveat=625,5,570  
  /riftsea raw: riftsea floor=10 water=51 sea=62 min=1 max=15 cols=9409 caves=8491 wet=202346 air=11604 reef=1981 plants=1786 corals=82 float=915 ceilair=6146 bottom=-62 caveat=625,5,570  
  OK   вода на глубине (696,56,696) = elisar_water
  /riftsea raw: riftsea floor=5 water=56 sea=62 min=-8 max=14 cols=9409 caves=8497 wet=164874 air=13321 reef=2239 plants=1395 corals=114 float=1040 ceilair=7502 bottom=-62 caveat=650,3,600  
  OK   донный блок: elisar_seabed на 696,5,696
  OK   кромка дна из /riftsea: y=10
  OK   толщина воды под рифом: 51 блоков (нужно >=44)
  OK   колонок с пещерами в радиусе проб: 8491
  OK   пещеры ЗАТОПЛЕНЫ: воды 94% объёма полостей (остальное — воздушные карманы)
  OK   воздух в пещерах прижат к своду: висячих пузырей 7% (нужно <=12%)
  OK   воздух под сводами (под камнем): 52% всего воздуха пещер
  OK   вода внутри пещеры (625,5,570) = elisar_water
  OK   пещера найдена на y=5 (в 57 блоках под уровнем моря)
  OK   рифовый сад: coral_brain at 600,11,665
  OK   вода над рифом (600,14,666) = elisar_water
  /riftcoral raw: riftcoral at=602,10,668 h=5 blocks=135 cols=84 tips=60 floor=9 sea=62 cam=619,18,675 camlow=612,11,678  
  коралл: at=602,10,668 h=5 блоков=135 колонок=84 украшений=60
  OK   коралл — ВЫСОКОЕ растение: 5 блоков в высоту
  OK   коралл из МНОГИХ блоков: 135 блоков, 84 колонок
  OK   у растения есть ветви/плита: 84 колонок
  OK   на ветвях есть кораллы-украшения: 60
  камера коралла: cam=619,18,675 camlow=612,11,678
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
  рыба silver: Elisar Sea Fish has the following entity data: [696.4806291240423d, 39.91497949801478d, 693.92102680926d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[90f,0f],sf_variant:1} -> Summoned new Elisar Sea Fish
  рыба gold: Elisar Sea Fish has the following entity data: [696.4903145620211d, 39.90928345322609d, 693.9113092637284d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[90f,0f],sf_variant:2} -> Summoned new Elisar Sea Fish
  рыба striped: Elisar Sea Fish has the following entity data: [696.5d, 39.90928345322609d, 693.9015917181969d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[45f,0f],sf_variant:3} -> Summoned new Elisar Sea Fish
  рыба jelly: Elisar Sea Fish has the following entity data: [696.4903145620211d, 39.90928345322609d, 693.9113092637284d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[45f,0f],sf_variant:4} -> Summoned new Elisar Sea Fish
  рыба ray: Elisar Sea Fish has the following entity data: [696.4903145620211d, 39.90928345322609d, 693.9113092637284d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[90f,0f],sf_variant:5} -> Summoned new Elisar Sea Fish
  рыба angler: Elisar Sea Fish has the following entity data: [696.4903145620211d, 39.90928345322609d, 693.9113092637284d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[90f,0f],sf_variant:6} -> Summoned new Elisar Sea Fish
  рыба eel: Elisar Sea Fish has the following entity data: [696.4903145620211d, 39.90928345322609d, 693.9113092637284d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[90f,0f],sf_variant:7} -> Summoned new Elisar Sea Fish
  рыба surgeon: Elisar Sea Fish has the following entity data: [696.4806291240423d, 39.91497949801478d, 693.92102680926d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[90f,0f],sf_variant:8} -> Summoned new Elisar Sea Fish
  рыба turtle: Elisar Sea Fish has the following entity data: [696.4903145620211d, 39.90928345322609d, 693.9113092637284d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[90f,0f],sf_variant:9} -> Summoned new Elisar Sea Fish
  рыба gloom: Elisar Sea Fish has the following entity data: [696.4903145620211d, 39.90928345322609d, 693.9113092637284d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[90f,0f],sf_variant:10} -> Summoned new Elisar Sea Fish
  рыба sting: Elisar Sea Fish has the following entity data: [696.5d, 39.90928345322609d, 693.9015917181969d]
  OK   клиент не жаловался на geo-модели
  /rifttree raw: rifttree at=-25,105,148 kind=sizo h=13 logs=27 branches=9 leaves=0 dist=90 loaded=true cam=-14,124,148 camtop=-14,134,148
  OK   дерево найдено: -25,105,148 (ветвей: 9)
  OK   у дерева есть ветки: 9 блоков вне ствола
  /rifttree повтор: rifttree at=27,107,129 kind=sizo h=10 logs=0 branches=0 leaves=0 dist=34 loaded=true cam=38,123,129 camtop=38,133,129
  OK   обычных деревьев генератора нет: деревья ставит только аддон DT (требование рев 16)
  камера дерева: cam=38,123,129 camtop=38,133,129
  give riftworlddt:sizo_seed -> Gave 1 [Sizo Seed] to TestPlayer
  OK   DT видит семена аддона (riftworlddt:sizo_seed)
  OK   DT видит семена лилового хлыста (riftworlddt:lilac_seed)
  OK   DT видит жёлудь садового дуба (riftworlddt:garden_oak_seed)
  упоминаний riftworlddt:sizo в логе: 0
0
  /dynamictrees registry family list -> dynamictrees:null dynamictrees:warped dynamictrees:spruce dynamictrees:oak dynamictrees:mangrove dynamictrees:jungle dynamictrees:dark_oak dynamictrees:crimson dynamictrees:cherry dynamictrees:birch dynamictrees:acacia r
  /dynamictrees registry species list -> dynamictrees:null dynamictrees:red_mushroom dynamictrees:brown_mushroom dynamictrees:warped dynamictrees:tall_birch dynamictrees:swamp_oak dynamictrees:spruce dynamictrees:oak_undergrowth dynamictrees:oak dynamictrees:me
  OK   у DT есть семья riftworlddt:sizo
  OK   у DT есть семья riftworlddt:ambra
  OK   у DT есть семья riftworlddt:lilac
  OK   у DT есть семья riftworlddt:garden_oak
  OK   у DT есть вид riftworlddt:sizo
  OK   у DT есть вид riftworlddt:ambra
  OK   у DT есть вид riftworlddt:lilac
  OK   у DT есть вид riftworlddt:giant_sizo
  OK   у DT есть вид riftworlddt:garden_oak
  [DT] секция дерева
  /riftdt diag -> sizo[sp=1 fam=1 br=riftworlddt:sizo_branch brf=true lv=1 dyn=riftworlddt:sizo_leaves] ambra[sp=1 fam=1 br=riftworlddt:ambra_branch brf=true lv=1 dyn=riftworlddt:ambra_leaves] garden_oak[sp=1 fam=1 br=riftworlddt:garden_oak_branch brf=true lv=1 dyn=riftworlddt:garden_oak_leaves] lilac[sp=1 fam=1 br=riftworlddt:lilac_branch brf=true lv=1 dyn=riftworlddt:lilac_leaves] giant_sizo[sp=1 fam=1 br=riftworlddt:sizo_branch brf
  dynamictrees settree 9,105,151 riftworlddt:sizo -> Successfully set tree at position 9, 105, 151 to species species.riftworlddt.sizo with JoCode JJJ0JP1JJ+ZJOOPvnRJOOPvn.  
  /riftdtat у посадки дерева -> riftdtat at=9,105,151 dt=120 dy=0 lv=86 air=853 water=0 top=117:riftworlddt:sizo_leaves col=rooty_elisar_soil,sizo_branchx6,airx4,sizo_leavesx4,air blocks=rooty_elisar_soil=1 sizo_branch=33 sizo_leaves=86 near=elisar_stone=147 elisar_crust=80 elisar_stone_mossy=15
  /dynamictrees gettree -> Species: species.riftworlddt.sizo JoCode: JJJzJJxx98+RJOOPvn1JJ8JP  
  контрольный дуб: Successfully set tree at position -9, 105, 161 to species Oak with JoCode JJJ0JR7Jxx+nXv.  
  /riftdtat у контрольного дуба -> riftdtat at=-9,105,161 dt=1 dy=155 lv=136 air=817 water=0 top=117:dynamictrees:oak_leaves col=rooty_elisar_soil,oak_branchx6,airx2,oak_branch,oak_leavesx3,airx3 blocks=rooty_elisar_soil=1 near=elisar_crust=81 elisar_ston
  саженец + костная мука -> riftgrow at=25,106,143 block=riftworlddt:sizo_sapling grew=8 after=riftworlddt:sizo_branch  
  /riftdtat у саженца -> riftdtat at=25,105,143 dt=116 dy=0 lv=108 air=857 water=0 top=111:riftworlddt:sizo_leaves col=rooty_elisar_soil,sizo_branchx4,sizo_leavesx2,airx9 blocks=rooty_elisar_soil=1 sizo_branch=7 sizo_leaves=108 near=elisar_stone
  OK   саженец аддона вырос в дерево DT: блоков 116 (листвы 108)
  OK   DT вырастил дерево аддона: блоков 120 (ветвей 33, листвы 86)
  [БИОМ] секция садов и рощи
  режим игрока -> Set TestPlayer's game mode to Spectator Mode  
  /riftbiome gardens (поиск) -> riftbiome name=gardens at=-1248,-1308 bio=riftworld:waterfall_gardens r=51 top=-2147483648 water=0 garden_grass=0 garden_leaves=0 garden_logs=0 ivy=0 planks=0 lantern=0 struct=- fall=-1060,-1219 wall=
  /riftbiome gardens (счёт) -> riftbiome name=gardens at=-1248,-1308 bio=riftworld:waterfall_gardens r=51 top=112 water=8102 garden_grass=6179 garden_leaves=0 garden_logs=0 ivy=3316 planks=0 lantern=0 struct=- fall=-1060,-1219 wall=-1256,92,-1288:-1256,94,-1299 isles=6 isl_r=47..52 sea=62 falls_water=178 falls_src=20 garden_oak=1808 sizo_blocks=0 below_sea_cols=0 blocks=garden_limestone=83546 elisar_stone=52138 garden_limestone_mossy=15614 elisar_
  OK   сады: биом острова riftworld:waterfall_gardens (биом-сурс совпадает с рельефом)
  /locate biome waterfall_gardens -> The nearest riftworld:waterfall_gardens is at [-1248, 130, -1308] (0 blocks away)  
  /riftwhere у найденного биома -> riftwhere at=-1248,-1308 biome=riftworld:waterfall_gardens top=110:riftworlddt:garden_oak_leaves ground=110:riftworlddt:garden_oak_leaves sea=62 garden=true  
  OK   /locate biome waterfall_gardens привёл на землю садов (-1248,130,-1308, верх: riftworlddt:garden_oak_leaves, земля 110:riftworlddt:garden_oak_leaves)
  /riftaround у точки /locate -> riftaround at=-1248,132,-1308 r=24 cols=2401 land=2091 garden_limestone=2792 garden_grass=386 lavender=0 elisar_stone=0 holes=0 gaps=52  
  OK   /locate привёл на настоящий садовый остров: известняка полок 2792 блоков в r=24
  OK   под островом садов нет дыр до бедрока (holes=0, трава садов 386)
  OK   сады: зелёной травы 6179 блоков
  OK   сады: воды 8102 блоков (пруды и водопады, без затопления)
  OK   сады: плюща на стенах 3316 блоков
  OK   сады: рукотворного нет (досок 0, фонарей 0)
  OK   сады: островов 6 (3–6 больших)
  OK   сады: радиусы островов 47..52 (нужно 40–55 — крупные острова)
  OK   сады: садового дуба аддона DT 1808 блоков
  OK   сады: сизоствола нет (sizo_blocks=0)
  OK   сады: водопады текут — 178 блоков струй, у 20 есть исток на кромке
  OK   сады: подводных ярусов почти нет (колонок ниже моря 0)
  OK   сады: обычных (stock) дубов нет (garden_leaves=0)
  кадр обрыва: камера -1256 92 -1288 -> взгляд -1256 94 -1299
  игрок после кадра обрыва -> TestPlayer has the following entity data: [-1255.5d, 92.0d, -1287.5d]  
  /locate biome waterfall_gardens -> The nearest riftworld:waterfall_gardens is at [-1256, 92, -1288] (0 blocks away)  
  /riftwhere у waterfall_gardens -> riftwhere at=-1256,-1288 biome=riftworld:waterfall_gardens top=90:riftworld:garden_grass ground=90:riftworld:garden_grass sea=62 garden=true  
  OK   /locate biome waterfall_gardens привёл на землю биома (90:riftworld:garden_grass)
  /riftbiome sizo (поиск) -> riftbiome name=sizo at=-240,841 bio=riftworld:sizo_grove r=80 top=-2147483648 water=0 garden_grass=0 garden_leaves=0 garden_logs=0 ivy=0 planks=0 lantern=0 struct=carts=0 shrines=0 temples=0 pillar_pa
  /riftbiome sizo (счёт) -> riftbiome name=sizo at=-240,841 bio=riftworld:sizo_grove r=80 top=136 water=38307 garden_grass=0 garden_leaves=0 garden_logs=0 ivy=0 planks=2 lantern=0 struct=carts=0 shrines=0 temples=0 pillar_pairs=1 fall=- wall=- falls_water=0 falls_src=0 garden_oak=0 sizo_blocks=0 below_sea_cols=0 blocks=elisar_stone=408666 sizo_leaves=44290 elisar_stone_mossy=43412 elisar_crust=12596 elisar_grass=8039 sizo_branch=7435  
  /riftbiome sizo: структуры -> carts=0 shrines=0 temples=0 pillar_pairs=1 fall= (по ТЗ: тележка ~1/3, памятник и храм ~1/7, 1-2 пары столбов)
  /rifttree у рощи -> rifttree at=105,112,-69 kind=sizo h=11 logs=0 branches=0 leaves=0 dist=126 loaded=true cam=116,129,-69 camtop=116,139,-69  
  /riftbiome violet (поиск) -> riftbiome name=violet at=1374,240 bio=riftworld:violet_hills r=108 top=-2147483648 water=0 garden_grass=0 garden_leaves=0 garden_logs=0 ivy=0 planks=0 lantern=0 struct=- fall=- wall=- violet_blooms=0 
  /riftbiome violet (счёт) -> riftbiome name=violet at=1374,240 bio=riftworld:violet_hills r=108 top=95 water=39129 garden_grass=375 garden_leaves=0 garden_logs=0 ivy=0 planks=0 lantern=0 struct=- fall=- wall=- violet_blooms=10392 violet_leaves=0 lavender=10275 fences=0 lamps=0 falls_water=0 falls_src=0 garden_oak=0 sizo_blocks=0 below_sea_cols=0 blocks=elisar_stone=76475 garden_limestone=35723 garden_limestone_mossy=15318 elisar_soil=12805 grass
  OK   холмы: лаванды всего 20667 блоков (гряды 10275 + цветы 10392)
  OK   холмы: гряд лаванды 10275 блоков (сплошные поля, как на фото)
  /riftdtwild (холмы, лиловый хлыст) -> riftdtwild at=1374,240 r=40 dt=2518 rooty=0 branch=119 leaves=2399 blocks=lilac_leaves=2399 lilac_branch=119  
  OK   холмы: динамических деревьев аддона 2518 блоков (лиловый хлыст)
  OK   роща: биом острова riftworld:sizo_grove
  /locate biome sizo_grove -> The nearest riftworld:sizo_grove is at [-162, 98, 837] (1670 blocks away)  
  /riftwhere у sizo_grove -> riftwhere at=-162,837 biome=riftworld:abyss top=62:riftworld:elisar_water ground=9:riftworld:reef_stone sea=62 garden=false  
  /riftwhere рядом с sizo_grove -> riftwhere at=-166,837 biome=riftworld:sizo_grove top=130:riftworlddt:sizo_leaves ground=130:riftworlddt:sizo_leaves sea=62 garden=false  
  OK   /locate biome sizo_grove привёл на землю биома (130:riftworlddt:sizo_leaves)
  OK   Фиолетовые холмы: биом луга riftworld:violet_hills
  /locate biome violet_hills -> The nearest riftworld:violet_hills is at [1406, 98, 261] (0 blocks away)  
  /riftwhere у violet_hills -> riftwhere at=1406,261 biome=riftworld:violet_hills top=82:riftworld:lilac_bloom ground=82:riftworld:lilac_bloom sea=62 garden=false  
  OK   /locate biome violet_hills привёл на землю биома (82:riftworld:lilac_bloom)
  игрок поставлен в центр луга: 1374,98,240
  /riftdtwhy (холмы) -> riftdtwhy at=1374,98,240 biome_surface=riftworld:violet_hills biome_top=riftworld:violet_hills biome_y0=riftworld:violet_hills dt_features=2 has_dt=true dt_species=handled:Species{registryName=riftworlddt:lilac} db_entries=7 db_populated=true plains_? sel=StaticSpeciesSelector gr
  OK   DT/холмы: грунт найден
  OK   DT/холмы: почва приемлема для DT
  OK   DT/холмы: база DT ответила по виду
  /riftdtwild (холмы) -> riftdtwild at=1374,240 r=32 dt=2715 rooty=0 branch=345 leaves=2370 blocks=lilac_leaves=2370 lilac_branch=345  
  /riftdtdbg (холмы) -> riftdtdbg at=1374,98,240 discs=1 disc0_r=8 real=true ground=- handled=true sp=Species{registryName=riftworlddt:lilac} codes=7[2:12 3:12 4:12 5:12 6:12 7:12 8:12 ] code_for_r8=ok  
  OK   DT растёт в мире сам: 2715 блоков аддона вокруг точки
  игрок: TestPlayer has the following entity data: [1374.5d, 98.0d, 240.5d]
  рыбы:  Elisar Sea Fish has the following entity data: [679.3570654634784d, 20.547711527108056d, 559.9269880583698d]
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
- ci-shots/40_garden_overview.jpg
- ci-shots/41_garden_terraces.jpg
- ci-shots/42_garden_walls.jpg
- ci-shots/43_garden_ponds.jpg
- ci-shots/44_garden_waterfall.jpg
- ci-shots/45_sizo_grove.jpg
- ci-shots/46_sizo_trunks.jpg
- ci-shots/47_sizo_canopy.jpg
- ci-shots/48_garden_cliff.jpg
- ci-shots/49_violet_hills.jpg
- ci-shots/50_violet_fields.jpg
- ci-shots/51_violet_path.jpg
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

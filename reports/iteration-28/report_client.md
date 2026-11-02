# CI, шаг 2: настоящий клиент Minecraft в Элисаре

- клиент: ForgeGradle runClient под Xvfb (llvmpipe, без GPU)
- воды в колонке (696,y,696): **58** блоков
- море: дно y=10, вода 51 блоков, глубочайшее дно y=1
- пещеры: 8497 колонок, воды 202732 / воздуха 11560 блоков (у свода 6121, висячего 916)
- провалов проб: **2**
- строк Exception/ERROR в логе клиента (без звука/наратора/authlib): **0**

## пробы
```
  /riftsea raw: riftsea floor=10 water=51 sea=62 min=1 max=15 cols=9409 caves=8497 wet=202732 air=11560 reef=1981 plants=1773 corals=64 float=916 ceilair=6121 bottom=-62 caveat=625,5,570 
  /riftsea raw: riftsea floor=10 water=51 sea=62 min=1 max=15 cols=9409 caves=8497 wet=202732 air=11560 reef=1981 plants=1773 corals=64 float=916 ceilair=6121 bottom=-62 caveat=625,5,570 
  OK   вода на глубине (696,56,696) = elisar_water
  /riftsea raw: riftsea floor=5 water=56 sea=62 min=-8 max=14 cols=9409 caves=8497 wet=164874 air=13311 reef=2239 plants=1402 corals=86 float=1040 ceilair=7498 bottom=-62 caveat=650,3,600 
  OK   донный блок: elisar_seabed на 696,5,696
  OK   кромка дна из /riftsea: y=10
  OK   толщина воды под рифом: 51 блоков (нужно >=44)
  OK   колонок с пещерами в радиусе проб: 8497
  OK   пещеры ЗАТОПЛЕНЫ: воды 94% объёма полостей (остальное — воздушные карманы)
  OK   воздух в пещерах прижат к своду: висячих пузырей 7% (нужно <=12%)
  OK   воздух под сводами (под камнем): 52% всего воздуха пещер
  OK   вода внутри пещеры (625,5,570) = elisar_water
  OK   пещера найдена на y=5 (в 57 блоках под уровнем моря)
  OK   рифовый сад: coral_brain at 600,11,665
  OK   вода над рифом (600,14,666) = elisar_water
  /riftcoral raw: riftcoral at=597,11,709 h=8 blocks=240 cols=109 tips=224 floor=10 sea=62 cam=591,22,715 camlow=597,12,700 
  коралл: at=597,11,709 h=8 блоков=240 колонок=109 украшений=224
  OK   коралл — ВЫСОКОЕ растение: 8 блоков в высоту
  OK   коралл из МНОГИХ блоков: 240 блоков, 109 колонок
  OK   у растения есть ветви/плита: 109 колонок
  OK   на ветвях есть кораллы-украшения: 224
  камера коралла: cam=591,22,715 camlow=597,12,700
  воды в колонке: 58 блоков
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
  рыба jelly: Elisar Sea Fish has the following entity data: [696.4903145620211d, 39.90928345322609d, 693.9113092637284d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[45f,0f],sf_variant:4} -> Summoned new Elisar Sea Fish
  рыба ray: Elisar Sea Fish has the following entity data: [696.4903145620211d, 39.90928345322609d, 693.9113092637284d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[90f,0f],sf_variant:5} -> Summoned new Elisar Sea Fish
  рыба angler: Elisar Sea Fish has the following entity data: [696.4903145620211d, 39.90928345322609d, 693.9113092637284d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[90f,0f],sf_variant:6} -> Summoned new Elisar Sea Fish
  рыба eel: Elisar Sea Fish has the following entity data: [696.4806291240423d, 39.91497949801478d, 693.92102680926d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[90f,0f],sf_variant:7} -> Summoned new Elisar Sea Fish
  рыба surgeon: Elisar Sea Fish has the following entity data: [696.4903145620211d, 39.90928345322609d, 693.9113092637284d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[90f,0f],sf_variant:8} -> Summoned new Elisar Sea Fish
  рыба turtle: Elisar Sea Fish has the following entity data: [696.4903145620211d, 39.90928345322609d, 693.9113092637284d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[90f,0f],sf_variant:9} -> Summoned new Elisar Sea Fish
  рыба gloom: Elisar Sea Fish has the following entity data: [696.4903145620211d, 39.90928345322609d, 693.9113092637284d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[90f,0f],sf_variant:10} -> Summoned new Elisar Sea Fish
  рыба sting: Elisar Sea Fish has the following entity data: [696.4806291240423d, 39.91505675198468d, 693.92102680926d]
  OK   клиент не жаловался на geo-модели
  /rifttree raw: rifttree at=-76,104,6 kind=ambra h=7 logs=16 branches=0 leaves=12 dist=69 loaded=true cam=-65,117,6 camtop=-65,127,6
  OK   дерево найдено: -76,104,6 (ветвей: 0)
  OK   обычных деревьев генератора в плане нет (активен аддон DT)
  /rifttree повтор: rifttree at=85,103,-13 kind=ambra h=6 logs=23 branches=15 leaves=153 dist=80 loaded=true cam=96,115,-13 camtop=96,125,-13
  OK   дерево с ветками и кроной: ветвей 15, листвы 153
  камера дерева: cam=96,115,-13 camtop=96,125,-13
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
  /riftdt diag -> crystal_birch[sp=1 fam=1 br=riftworlddt:crystal_birch_branch brf=true lv=1 dyn=riftworlddt:crystal_birch_leaves] sizo[sp=1 fam=1 br=riftworlddt:sizo_branch brf=true lv=1 dyn=riftworlddt:sizo_leaves] ambra[sp=1 fam=1 br=riftworlddt:ambra_branch brf=true lv=1 dyn=riftworlddt:ambra_leaves] garden_oak[sp=1 fam=1 br=riftworlddt:garden_oak_branch brf=true lv=1 dyn=riftworlddt:garden_oak_leaves] lilac[sp=1 fam=1 br=riftworl
  dynamictrees settree 9,104,9 riftworlddt:sizo -> Successfully set tree at position 9, 104, 9 to species Sizo Tree with JoCode JJJ0JP1JJ+ZJOOPvnRJOOPvn. 
  /riftdtat у посадки дерева -> riftdtat at=9,104,9 dt=120 dy=0 lv=86 air=825 water=0 top=116:riftworlddt:sizo_leaves col=rooty_elisar_soil,sizo_branchx6,airx4,sizo_leavesx4,air blocks=rooty_elisar_soil=1 sizo_branch=33 sizo_leaves=86 near=elisar_bricks=171 temple_floor=61 garden_water=28 ashlar=4 temple_mosaic=4 elisar_cobble=2
  /dynamictrees gettree -> Species: Sizo Tree JoCode: JJJzJJxx98+RJOOPvn1JJ8JP 
  контрольный дуб: Successfully set tree at position -9, 104, 19 to species Oak with JoCode JJJ0JR7Jxx+nXv. 
  /riftdtat у контрольного дуба -> riftdtat at=-9,104,19 dt=1 dy=148 lv=129 air=824 water=0 top=116:dynamictrees:oak_leaves col=rooty_elisar_soil,oak_branchx6,air,oak_leaves,oak_branch,oak_leavesx3,airx3 blocks=rooty_elisar_soil=1 near=elisar_bricks=156 k
  саженец + костная мука -> riftgrow at=25,105,1 block=riftworlddt:sizo_sapling grew=8 after=riftworlddt:sizo_branch 
  /riftdtat у саженца -> riftdtat at=25,104,1 dt=73 dy=0 lv=64 air=900 water=0 top=112:riftworlddt:sizo_leaves col=rooty_elisar_soil,sizo_branchx7,airx7,temple_bricks blocks=rooty_elisar_soil=1 sizo_branch=8 sizo_leaves=64 near=elisar_bricks=94 
  OK   саженец аддона вырос в дерево DT: блоков 73 (листвы 64)
  OK   DT вырастил дерево аддона: блоков 120 (ветвей 33, листвы 86)
  [БИОМ] секция садов и рощи
  режим игрока -> Set TestPlayer's game mode to Spectator Mode 
  /riftbiome gardens (поиск) -> riftbiome name=gardens at=406,-650 bio=riftworld:waterfall_gardens r=82 top=-2147483648 water=0 garden_grass=0 garden_leaves=0 garden_logs=0 ivy=0 planks=0 lantern=0 struct=- fall=399,-675 wall=411,86
  /riftbiome gardens (счёт) -> riftbiome name=gardens at=406,-650 bio=riftworld:waterfall_gardens r=82 top=104 water=7791 garden_grass=8142 garden_leaves=0 garden_logs=0 ivy=2784 planks=0 lantern=0 struct=- fall=399,-675 wall=411,86,-628:411,87,-639 river=287,92,-677:243,68,-650 isles=5 isl_r=37..82 sea=62 falls_water=1246 falls_src=81 garden_oak=10379 sizo_blocks=0 below_sea_cols=985 submerged_air=0 blocks=garden_limestone=114976 elisar_stone=48416 garden_limestone_mossy=20547 elisar_stone_mossy=12080 elisar_soil=11241 garden_oak_leaves=9035 
  OK   сады: биом острова riftworld:waterfall_gardens (биом-сурс совпадает с рельефом)
  /locate biome waterfall_gardens -> The nearest riftworld:waterfall_gardens is at [407, 90, -651] (40 blocks away) 
  /riftwhere у найденного биома -> riftwhere at=407,-651 biome=riftworld:waterfall_gardens top=104:riftworlddt:garden_oak_leaves ground=104:riftworlddt:garden_oak_leaves sea=62 garden=true 
  OK   /locate biome waterfall_gardens привёл на землю садов (407,90,-651, верх: riftworlddt:garden_oak_leaves, земля 104:riftworlddt:garden_oak_leaves)
  /riftaround у точки /locate -> riftaround at=407,106,-651 r=24 cols=2401 land=2108 garden_limestone=27037 garden_grass=1469 lavender=0 elisar_stone=13069 holes=0 gaps=0 
  OK   /locate привёл на настоящий садовый остров: известняка полок 27037 блоков в r=24
  OK   под островом садов нет дыр до бедрока (holes=0, трава садов 1469)
  OK   сады: зелёной травы 8142 блоков
  OK   сады: воды 7791 блоков (пруды и водопады, без затопления)
  OK   сады: плюща на стенах 2784 блоков
  OK   сады: рукотворного нет (досок 0, фонарей 0)
  OK   сады: островов 5 (увеличенный архипелаг 5–8 больших островов)
  OK   сады: разброс радиусов увеличенных островов 37..82
  OK   сады: садового дуба аддона DT 10379 блоков
  OK   сады: сизоствола нет (sizo_blocks=0)
  OK   сады: водопады текут — 1246 блоков струй, у 81 есть исток на кромке
  OK   сады: подводные каменные ярусы есть (985 колонок) и вода над ними не удалена (submerged_air=0)
  OK   сады: разбросанной листвы на земле нет (garden_leaves=0)
  кадр обрыва: камера 411 86 -628 -> взгляд 411 87 -639
  игрок после кадра обрыва -> TestPlayer has the following entity data: [411.5d, 86.0d, -627.5d] 
  /locate biome waterfall_gardens -> The nearest riftworld:waterfall_gardens is at [407, 90, -651] (23 blocks away) 
  /riftwhere у waterfall_gardens -> riftwhere at=407,-651 biome=riftworld:waterfall_gardens top=104:riftworlddt:garden_oak_leaves ground=104:riftworlddt:garden_oak_leaves sea=62 garden=true 
  OK   /locate biome waterfall_gardens привёл на землю биома (104:riftworlddt:garden_oak_leaves)
  /locate biome ashen_lands -> The nearest riftworld:ashen_lands is at [1089, 66, -889] (726 blocks away) 
  /riftbiome ashen -> riftbiome name=ashen at=-431,916 bio=riftworld:ashen_lands r=430 combo=7 top=104 castle=-469,882 
  OK   /locate biome ashen_lands нашёл случайный биом адского острова с замком (-431, 916)
  /riftbiome sizo (поиск) -> riftbiome name=sizo at=-342,2413 bio=riftworld:sizo_grove r=166 top=-2147483648 water=0 garden_grass=0 garden_leaves=0 garden_logs=0 ivy=0 planks=0 lantern=0 struct=carts=1 shrines=0 temples=1 pillar_
  /riftbiome sizo (счёт) -> riftbiome name=sizo at=-342,2413 bio=riftworld:sizo_grove r=166 top=128 water=38307 garden_grass=0 garden_leaves=0 garden_logs=0 ivy=0 planks=92 lantern=2 struct=carts=1 shrines=0 temples=1 pillar_pairs=2 fall=- wall=- river=- sizo_trail=1378 sizo_flowers=725 sizo_pillars=34 sview1=-426,112,2377:-305,113,2423 sview2=-402,111,2396:-286,111,2440 sview3=-360,113,2402:-282,114,2444 falls_water=0 falls_src=0 garden_oak=0 sizo_blocks=0 below_sea_cols=0 submerged_air=0 blocks=elisar_stone=440190 elisar_stone_mossy=45709 sizo_leaves=22732 elisar_crust=12104 elis
  /riftbiome sizo: структуры -> carts=1 shrines=0 temples=1 pillar_pairs=2 fall= trail=1378 flowers=725 pillars=34
  OK   роща сизо: мощёная тропа (1378), парные столбы с янтарём (34) и васильковые цветы (725)
  /riftdtwild (роща сизо) -> riftdtwild at=-342,2413 r=44 dt=15057 rooty=30 branch=1489 leaves=13538 blocks=sizo_leaves=13538 sizo_branch=1489 rooty_elisar_grass=27 rooty_elisar_moss=3 
  OK   роща сизо: гигантские динамические деревья сизо выросли (15057 блоков DT, кроны 13538)
  /riftbiome violet (поиск) -> riftbiome name=violet at=-989,-1202 bio=riftworld:violet_hills r=131 top=-2147483648 water=0 garden_grass=0 garden_leaves=0 garden_logs=0 ivy=0 planks=0 lantern=0 struct=- fall=- wall=- river=- vislan
  /riftbiome violet (счёт) -> riftbiome name=violet at=-989,-1202 bio=riftworld:violet_hills r=131 top=110 water=34344 garden_grass=0 garden_leaves=0 garden_logs=0 ivy=0 planks=23 lantern=9 struct=- fall=- wall=- river=- vislands=6 visl_r=82..132 violet_blooms=1975 violet_leaves=0 lavender=3647 accent_flowers=603 walls=92 fences=113 lamps=33 vground=82 road=-1077,88,-1223:-947,85,-1179 varch=-953,116,-1213:-796,86,-1109 vfields=-1031,99,-1216:-951,84,-1182 vpath=-994,84,-1197:-819,85,-1124 vtrees=-988,86,-1203:-980,92,-1211 falls_water=0 falls_src=0 garden_oak=0 sizo_blocks=0 below_s
  OK   холмы: увеличенный архипелаг из 6 огромных парящих островов
  OK   холмы: сизоствола нет (sizo_blocks=0)
  OK   холмы: воздушных цветов всего 5622 блоков (лаванда 3647 + соцветия 1975)
  OK   холмы: гряд лаванды 3647 блоков (воздушные ряды)
  OK   холмы: вдоль дороги каменные стенки (92), деревянные заборы (113) и столбы с факелами (33)
  OK   холмы: других цветов в малом количестве (603 на фоне 5622 фиолетовых)
  /riftdtwild (холмы, лиловый ветвецвет) -> riftdtwild at=-989,-1202 r=40 dt=1297 rooty=0 branch=157 leaves=1140 blocks=lilac_leaves=1140 lilac_branch=157 
  OK   холмы: динамических деревьев аддона 1297 блоков (лиловый ветвецвет)
  OK   роща: биом острова riftworld:sizo_grove
  /locate biome sizo_grove -> The nearest riftworld:sizo_grove is at [787, 103, -2247] (2060 blocks away) 
  /riftwhere у sizo_grove -> riftwhere at=787,-2247 biome=riftworld:sizo_grove top=101:riftworld:elisar_moss ground=101:riftworld:elisar_moss sea=62 garden=false 
  OK   /locate biome sizo_grove привёл на землю биома (101:riftworld:elisar_moss)
  OK   Фиолетовые холмы: биом луга riftworld:violet_hills
  /locate biome violet_hills -> The nearest riftworld:violet_hills is at [-989, 86, -1202] (0 blocks away) 
  /riftwhere у violet_hills -> riftwhere at=-989,-1202 biome=riftworld:violet_hills top=83:minecraft:grass ground=83:minecraft:grass sea=62 garden=false 
  OK   /locate biome violet_hills привёл на землю биома (83:minecraft:grass)
  /riftdtwhy (холмы) -> riftdtwhy at=-989,86,-1202 biome_surface=riftworld:violet_hills biome_top=riftworld:violet_hills biome_y0=riftworld:violet_hills dt_features=2 has_dt=true dt_species=handled:Species{registryName=riftworlddt:lilac} db_entries=7 db_populated=true plains_? sel=StaticSpeciesSelector 
  OK   DT/холмы: грунт найден
  OK   DT/холмы: почва приемлема для DT
  OK   DT/холмы: база DT ответила по виду
  /riftdtwild (холмы) -> riftdtwild at=-989,-1202 r=32 dt=460 rooty=0 branch=63 leaves=397 blocks=lilac_leaves=397 lilac_branch=63 
  /riftdtdbg (холмы) -> riftdtdbg at=-989,86,-1202 discs=1 disc0_r=6 real=true ground=82:Block{minecraft:grass_block} handled=true sp=Species{registryName=riftworlddt:lilac} codes=7[2:12 3:12 4:12 5:12 6:12 7:12 8:12 ] code_for_r6=ok gen=true 
  OK   DT растёт в мире сам: 460 блоков аддона вокруг точки
  /locate biome crystal_rifts -> The nearest riftworld:crystal_rifts is at [-2441, 89, -2825] (2177 blocks away) 
  /riftwhere у crystal_rifts -> riftwhere at=-2441,-2825 biome=riftworld:crystal_rifts top=87:riftworld:crystal_block ground=87:riftworld:crystal_block sea=62 garden=false 
  OK   /locate biome crystal_rifts привёл на землю биома (87:riftworld:crystal_block)
  /riftbiome crystal -> riftbiome name=crystal at=1181,-2622 isl1=1078,-2473 bio=riftworld:crystal_rifts cislands=6 cbridges=5 bridge_walkable=1 crystal_blocks=3175 marble_blocks=475925 rift_turf=8927 rift_grass=1908 vanilla_grass=0 isl1_trees=0 crystal_flora=2484 water=531 ivy=19 sizo_blocks=0 cview1=1183,90,-2673:1180,90,-2595 cview2=1142,91,-2571:1135,92,-2554 cview3=1101,99,-25
  OK   Хрустальные разломы: биом святилища riftworld:crystal_rifts
  OK   Хрустальные разломы: архипелаг из 6 островов и 5 арочных мостов
  OK   Хрустальные разломы: каменный арочный мост непрерывен и полностью проходим пешком
  OK   Хрустальные разломы: многоблочные кристаллы на всех островах (3175), мрамор/сланец (475925), чаши/водопады (531)
  OK   Хрустальные разломы: собственный шалфейный дёрн (8927) и трава разлома (1908), ванильной травы=0, деревьев на кристальном острове=0
  OK   Хрустальные разломы: чужих деревьев сизо нет (sizo_blocks=0)
  /riftdtwild (хрустальные разломы, мини-бонсай серебрица) -> riftdtwild at=1181,-2622 r=44 dt=45 rooty=4 branch=26 leaves=15 blocks=crystal_birch_branch=26 crystal_birch_leaves=15 rooty_rift_turf=4 
  OK   Хрустальные разломы: миниатюрные редкие деревца DT вдоль аллеи (45 блоков, без лесов и без ambra)
  /riftstruct crashed_ship -> riftstruct kind=crashed_ship at=937,66,155 variant=0 blocks=1393 rock=617 sub_air=0 cam=920,74,138:934,71,161 
  OK   структура crashed_ship построена: blocks=1393, sub_air=0
  /riftstruct sunken_ship -> riftstruct kind=sunken_ship at=819,14,221 variant=0 blocks=1185 sub_air=0 cam=817,20,235:821,15,221 
  OK   структура sunken_ship построена: blocks=1185, sub_air=0
  /riftstruct ocean_building -> riftstruct kind=ocean_building at=-474,15,-778 variant=3 blocks=323 sub_air=0 cam=-462,21,-766:-474,16,-778 
  OK   структура ocean_building построена: blocks=323, sub_air=0
  /riftstruct white_village -> riftstruct kind=white_village at=-810,97,-365 variant=0 houses=5 blocks=637 sub_air=0 cam=-802,108,-341:-810,100,-369 
  OK   структура white_village построена: blocks=637, sub_air=0
  OK   деревня white_village содержит ровно 5 небольших домов (лимит <= 6 соблюдён)
  /riftstruct burned_village -> riftstruct kind=burned_village at=-489,64,1105 bio=riftworld:ashen_lands variant=1 houses=5 blocks=793 fires=11 sub_air=0 cam=-482,74,1128:-489,67,1102 
  OK   структура burned_village построена: blocks=793, sub_air=0
  OK   деревня burned_village содержит ровно 5 небольших домов (лимит <= 6 соблюдён)
  /riftstruct stilt_village -> riftstruct kind=stilt_village at=382,65,-567 variant=0 houses=6 blocks=3732 sub_air=0 cam=368,78,-542:383,68,-573 
  OK   структура stilt_village построена: blocks=3732, sub_air=0
  OK   деревня stilt_village содержит ровно 6 небольших домов (лимит <= 6 соблюдён)
  /riftstruct city_castle_front -> riftstruct kind=city_castle_front at=0,112,36 main_plots=48 north_plots=11 blocks=4710 sub_air=0 cam=14,122,58:0,121,6 
  OK   структура city_castle_front построена: blocks=4710, sub_air=0
  /riftstruct city_castle_aerial -> riftstruct kind=city_castle_aerial at=0,112,0 main_plots=48 north_plots=11 blocks=4710 sub_air=0 cam=48,154,54:0,118,-6 
  OK   структура city_castle_aerial построена: blocks=4710, sub_air=0
  /riftstruct city_castle_north -> riftstruct kind=city_castle_north at=0,112,-54 main_plots=48 north_plots=11 blocks=4710 sub_air=0 cam=-18,132,-78:2,112,-30 
  OK   структура city_castle_north построена: blocks=4710, sub_air=0
  /riftstruct colonnade_boss -> riftstruct kind=colonnade_boss at=1180,86,-2607 bosses=1 minions=2 attack_ok=0 blocks=120 sub_air=0 cam=1181,90,-2618:1180,92,-2607 
  OK   структура colonnade_boss построена: blocks=120, sub_air=0
  /riftstruct colonnade_boss_minion -> riftstruct kind=colonnade_boss_minion at=1180,86,-2607 bosses=1 minions=2 attack_ok=0 blocks=120 sub_air=0 cam=1184,88,-2614:1184,89,-2611 
  OK   структура colonnade_boss_minion построена: blocks=120, sub_air=0
  OK   3D-оружие Колоннады (crystal_katar, crystal_barrage, crystal_aegis, astral_prism) выдано и отснято
  /riftstruct ocean_island_waves -> riftstruct kind=ocean_island_waves at=-405,63,-118 wake_islands=176 blocks=1080 sub_air=0 cam=-323,92,-94:-405,65,-118 
  OK   структура ocean_island_waves построена: blocks=1080, sub_air=0
  /riftstruct waterfall_ocean_waves -> riftstruct kind=waterfall_ocean_waves at=1116,63,-2564 blocks=200 sub_air=0 cam=1134,76,-2580:1117,64,-2567 
  OK   структура waterfall_ocean_waves построена: blocks=200, sub_air=0
  /riftstruct harbor_port -> riftstruct kind=harbor_port at=-362,64,-544 island_top=68 blocks=42489 sub_air=0 cam=-336,81,-516:-364,68,-554 
  OK   структура harbor_port построена: blocks=42489, sub_air=0
  /riftstruct grand_theater -> riftstruct kind=grand_theater at=349,97,625 blocks=8061 sub_air=0 cam=359,113,647:349,101,619 
  OK   структура grand_theater построена: blocks=8061, sub_air=0
  /riftstruct sea_lighthouse -> riftstruct kind=sea_lighthouse at=172,69,621 blocks=37936 sub_air=0 cam=196,78,647:172,85,621 
  OK   структура sea_lighthouse построена: blocks=37936, sub_air=0
  /riftstruct ancient_tree_altar -> riftstruct kind=ancient_tree_altar at=821,102,-2292 blocks=2755 sub_air=0 cam=836,109,-2277:821,108,-2292 
  OK   структура ancient_tree_altar построена: blocks=2755, sub_air=0
  /riftstruct ashen_belfry_barrow -> riftstruct kind=ashen_belfry_barrow at=-338,64,1081 blocks=1424 sub_air=0 cam=-342,75,1107:-336,74,1081 
  OK   структура ashen_belfry_barrow построена: blocks=1424, sub_air=0
  /riftstruct shattered_shard_forge -> riftstruct kind=shattered_shard_forge at=1295,91,-2467 blocks=3970 sub_air=0 cam=1299,99,-2451:1295,96,-2467 
  OK   структура shattered_shard_forge построена: blocks=3970, sub_air=0
  /riftstruct violet_wind_observatory -> riftstruct kind=violet_wind_observatory at=-993,98,-880 blocks=2602 sub_air=0 cam=-977,108,-861:-994,104,-874 
  OK   структура violet_wind_observatory построена: blocks=2602, sub_air=0
  /riftstruct elisar_eye_occlusion -> riftstruct kind=elisar_eye_occlusion at=170,73,605 blocks=250 sub_air=0 cam=170,73,605:179,93,629 
  OK   структура elisar_eye_occlusion построена: blocks=250, sub_air=0
  /riftstruct colonnade_boss_pillars -> riftstruct kind=colonnade_boss_pillars at=1180,86,-2607 bosses=1 minions=2 attack_ok=1 blocks=170 sub_air=0 cam=1176,95,-2634:1181,90,-2616 
  OK   структура colonnade_boss_pillars построена: blocks=170, sub_air=0
  /riftstruct colonnade_boss_hail -> riftstruct kind=colonnade_boss_hail at=1180,86,-2607 bosses=1 minions=2 attack_ok=1 blocks=170 sub_air=0 cam=1176,95,-2638:1181,96,-2617 
  OK   структура colonnade_boss_hail построена: blocks=170, sub_air=0
  /riftstruct origami_dragon_closeup -> riftstruct kind=origami_dragon_closeup at=-971,127,-29 city_safe=true dragons=1 blocks=220 sub_air=0 cam=-959,131,-13:-964,127,-37 
  OK   структура origami_dragon_closeup построена: blocks=220, sub_air=0
  /riftstruct origami_dragon_under_release -> riftstruct kind=origami_dragon_under_release at=-1002,78,-57 city_safe=true dragons=1 blocks=220 sub_air=0 cam=-986,73,-24:-999,79,-60 
  OK   структура origami_dragon_under_release построена: blocks=220, sub_air=0
  /riftstruct origami_dragon_over_release -> riftstruct kind=origami_dragon_over_release at=-974,131,-32 city_safe=true dragons=1 blocks=220 sub_air=0 cam=-960,139,-1:-970,129,-35 
  OK   структура origami_dragon_over_release построена: blocks=220, sub_air=0
  /riftstruct blacktide_bridge_shore -> riftstruct kind=blacktide_bridge_shore at=-431,86,1371 bridge_land=true castle_sea=true water_ring=4 land_blocks=6536 blocks=147743 sub_air=0 cam=-441,92,1302:-431,104,1390 
  OK   структура blacktide_bridge_shore построена: blocks=147743, sub_air=0
  /riftstruct blacktide_citadel -> riftstruct kind=blacktide_citadel at=-431,94,1406 bridge_land=true castle_sea=true water_ring=6 land_blocks=6536 blocks=147743 sub_air=0 cam=-499,124,1330:-431,106,1410 
  OK   структура blacktide_citadel построена: blocks=147743, sub_air=0
  /riftstruct blacktide_cathedral_hall -> riftstruct kind=blacktide_cathedral_hall at=-431,101,1405 carpets=307 thrones=1 chandeliers=9 pillars=1801 gilded=0 blocks=2338 sub_air=0 cam=-431,101,1405:-431,102,1442 
  OK   структура blacktide_cathedral_hall построена: blocks=2338, sub_air=0
  /riftstruct blacktide_rotunda_sanctum -> riftstruct kind=blacktide_rotunda_sanctum at=-431,102,1450 carpets=154 altars=1 books=344 gilded=0 blocks=1364 sub_air=0 cam=-431,102,1450:-431,100,1471 
  OK   структура blacktide_rotunda_sanctum построена: blocks=1364, sub_air=0
  /riftstruct blacktide_tower_walkability -> riftstruct kind=blacktide_tower_walkability at=-431,100,1378 climbed_towers=22 total_stairs=846 blocks=846 sub_air=0 cam=-431,100,1378:-431,116,1416 
  OK   структура blacktide_tower_walkability построена: blocks=846, sub_air=0
  /riftstruct ashen_burned_church -> riftstruct kind=ashen_burned_church at=-413,64,706 blocks=2307 sub_air=0 cam=-438,75,724:-415,72,706 
  OK   структура ashen_burned_church построена: blocks=2307, sub_air=0
  /riftstruct ashen_combo_matrix -> riftstruct kind=ashen_combo_matrix at=2938,64,829 empty=7 partial=29 all_three=5 ash_r=408 blocks=995 sub_air=0 cam=2890,88,877:2980,66,787 
  OK   структура ashen_combo_matrix построена: blocks=995, sub_air=0
  /riftstruct enlarged_biomes_audit -> riftstruct kind=enlarged_biomes_audit at=-989,84,-1202 ash_r=434 violet_r=146 crystal_r=83 sizo_r=176 gardens_r=82 mist_r=99 blocks=600 sub_air=0 cam=-1031,111,-1160:-965,96,-1226 
  OK   структура enlarged_biomes_audit построена: blocks=600, sub_air=0
  /riftstruct connected_carpets_showcase -> riftstruct kind=connected_carpets_showcase at=768,137,768 carpets=68 blocks=340 sub_air=0 cam=768,147,778:768,137,768 
  OK   структура connected_carpets_showcase построена: blocks=340, sub_air=0
  /riftstruct sea_cave_exit -> riftstruct kind=sea_cave_exit at=-15,14,36 mouth_cols=41 cave_water=622 blocks=1032 sub_air=0 cam=-10,20,38:-14,7,37 
  OK   структура sea_cave_exit построена: blocks=1032, sub_air=0
  /riftstruct locate_all_audit -> riftstruct kind=locate_all_audit at=0,112,0 structs_found=23/24 biomes_found=9/9 blocks=0 sub_air=0 cam=24,154,68:0,116,0 
  FAIL locate_all_audit: riftstruct kind=locate_all_audit at=0,112,0 structs_found=23/24 biomes_found=9/9 blocks=0 sub_air=0 cam=24,154,68:0,116,0 
  /locate structure riftworld:blacktide_citadel -> The nearest riftworld:blacktide_citadel is at [1505, ~, -889] (1574 blocks away) 
  OK   ванильная команда /locate structure riftworld:blacktide_citadel мгновенно вернула координаты
  /locate structure riftworld:grand_theater -> The nearest riftworld:grand_theater is at [349, ~, 625] (798 blocks away) 
  OK   ванильная команда /locate structure riftworld:grand_theater мгновенно вернула координаты
  /locate structure riftworld:sea_cave_exit -> The nearest riftworld:sea_cave_exit is at [260, ~, -139] (140 blocks away) 
  OK   ванильная команда /locate structure riftworld:sea_cave_exit мгновенно вернула координаты
  /locate biome riftworld:shallow_reef -> The nearest riftworld:shallow_reef is at [72, 58, -188] (144 blocks away) 
  OK   ванильная команда /locate biome riftworld:shallow_reef мгновенно вернула координаты
  /locate biome riftworld:abyss -> The nearest riftworld:abyss is at [120, 58, -140] (127 blocks away) 
  OK   ванильная команда /locate biome riftworld:abyss мгновенно вернула координаты
  /locate biome riftworld:violet_hills -> The nearest riftworld:violet_hills is at [-989, 86, -1202] (1538 blocks away) 
  OK   ванильная команда /locate biome riftworld:violet_hills мгновенно вернула координаты
  /locate biome riftworld:crystal_rifts -> The nearest riftworld:crystal_rifts is at [1180, 88, -2607] (2686 blocks away) 
  OK   ванильная команда /locate biome riftworld:crystal_rifts мгновенно вернула координаты
  [Overworld] /locate structure riftworld:overgrown_temple -> Could not find a structure of type "riftworld:overgrown_temple" nearby 
  FAIL /locate structure riftworld:overgrown_temple: Could not find a structure of type "riftworld:overgrown_temple" nearby 
  /riftstruct spawn_eggs_audit -> riftstruct kind=spawn_eggs_audit at=540,98,-420 eggs_ok=16/16 dragon_egg_spawn=1 blocks=400 sub_air=0 cam=514,106,-394:540,102,-420 
  OK   все 16/16 яиц призыва мобов зарегистрированы, Бумажный Дракон успешно призван яйцом (SPAWN_EGG)!
  /riftportal observatory_find -> riftportal observatory_find natural=fallback at=96,71,96 
  /riftportal observatory_verify -> riftportal observatory_verify seal=96,66,96 candles=8 chests=2 lecterns=1 rugs=25 cam=98,70,108:96,69,94 
  OK   Обсерватория Обычного мира построена со всем ритуальным набором: candles=8, chests=2
  /riftportal observatory_awaken -> riftportal observatory_awaken rift=true lit_candles=8 lit_runes=72 cam=95,70,106:96,73,96 
  OK   Обсерватория пробудила все руны, свечи и Белый 3D-Пролом в Элисар!
  /riftportal setup -> riftportal setup center=24,-60,0 snapshot=2435 cam=28,-57,24:24,-49,0 
  /riftportal trigger -> riftportal trigger started=true 
  /riftportal walkin -> riftportal walkin started=true 
  после захода в белый пролом и катсцены: Dimension=TestPlayer has the following entity data: "riftworld:elisar"  | riftportal verify mutated=0 total=2435 
  OK   белый 3D-Пролом при входе бесшовно перенёс в Белый Город с катсценой, изменённых блоков: 0
  игрок: TestPlayer has the following entity data: [0.5d, 105.0d, 22.5d]
  рыбы:  Elisar Sea Fish has the following entity data: [588.9483819351958d, 38.43747112111211d, 832.042499956784d]
```
## колонка (y; W=вода, S=донный песок, a=воздух/пещера, .=прочее)
```
WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW...
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
- ci-shots/100_cutscene_glide_to_player.jpg
- ci-shots/101_harbor_port.jpg
- ci-shots/102_grand_theater.jpg
- ci-shots/103_sea_lighthouse.jpg
- ci-shots/104_ancient_tree_altar.jpg
- ci-shots/105_ashen_belfry_barrow.jpg
- ci-shots/106_shattered_shard_forge.jpg
- ci-shots/107_violet_wind_observatory.jpg
- ci-shots/108_elisar_eye_block_occlusion.jpg
- ci-shots/109_boss_ice_pillars_circle.jpg
- ci-shots/10_fish_reef.jpg
- ci-shots/10b_fish_tropical.jpg
- ci-shots/110_boss_sky_ice_hail.jpg
- ci-shots/111_origami_dragon_closeup.jpg
- ci-shots/112_origami_dragon_under_island.jpg
- ci-shots/113_origami_dragon_over_island.jpg
- ci-shots/114_blacktide_citadel_sea_and_shore.jpg
- ci-shots/115_blacktide_bridge_from_ashen_land.jpg
- ci-shots/116_ashen_burned_church.jpg
- ci-shots/117_ashen_empty_biome_panorama.jpg
- ci-shots/118_enlarged_violet_hills_panorama.jpg
- ci-shots/119_blacktide_cathedral_throne_hall.jpg
- ci-shots/11_fish_cave.jpg
- ci-shots/11b_fish_cave_rare.jpg
- ci-shots/120_blacktide_rotunda_sanctum.jpg
- ci-shots/121_blacktide_tower_stairs_panorama.jpg
- ci-shots/122_connected_carpets_shapes.jpg
- ci-shots/123_overworld_portal_observatory.jpg
- ci-shots/124_overworld_portal_observatory_awakened.jpg
- ci-shots/125_open_rift_50m_white_aura.jpg
- ci-shots/126_underwater_cave_sea_exit.jpg
- ci-shots/127_paper_dragon_spawn_egg_test.jpg
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
- ci-shots/40b_garden_archipelago_rivers.jpg
- ci-shots/41_garden_terraces.jpg
- ci-shots/42_garden_walls.jpg
- ci-shots/43_garden_ponds.jpg
- ci-shots/44_garden_waterfall.jpg
- ci-shots/45_sizo_grove.jpg
- ci-shots/46_sizo_trunks.jpg
- ci-shots/47_sizo_canopy.jpg
- ci-shots/48_garden_cliff.jpg
- ci-shots/49_violet_hills.jpg
- ci-shots/49b_violet_archipelago.jpg
- ci-shots/50_violet_fields.jpg
- ci-shots/51_violet_path.jpg
- ci-shots/52_violet_trees_closeup.jpg
- ci-shots/60_ashen_castle_biome.jpg
- ci-shots/61_ashen_castle_closeup.jpg
- ci-shots/70_crystal_sanctum_overview.jpg
- ci-shots/71_crystal_bridge_passage.jpg
- ci-shots/72_crystal_giant_spire_pool.jpg
- ci-shots/73_crystal_astrolabe_avenue.jpg
- ci-shots/74_crystal_archipelago_falls.jpg
- ci-shots/80_crashed_ship_cliff.jpg
- ci-shots/81_sunken_ship_seabed.jpg
- ci-shots/82_ocean_floor_building.jpg
- ci-shots/83_white_village_hamlet.jpg
- ci-shots/84_burned_village_ashen.jpg
- ci-shots/85_stilt_village_gardens.jpg
- ci-shots/86_city_castle_barbican.jpg
- ci-shots/87_city_castle_aerial.jpg
- ci-shots/88_city_castle_rear_district.jpg
- ci-shots/89_colonnade_boss_and_minions.jpg
- ci-shots/90_crystal_katar_and_shield.jpg
- ci-shots/91_astral_prism_and_barrage.jpg
- ci-shots/92_crystal_minion_closeup.jpg
- ci-shots/95_ocean_island_waves.jpg
- ci-shots/96_waterfall_ocean_ripples.jpg
- ci-shots/97_portal_opening_pattern_shade.jpg
- ci-shots/98_white_spatial_breach_skyward.jpg
- ci-shots/98a_walkin_soft_veil_no_hud.jpg
- ci-shots/99_walkin_city_cutscene_titles.jpg
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

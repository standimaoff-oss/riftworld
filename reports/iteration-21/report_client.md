# CI, шаг 2: настоящий клиент Minecraft в Элисаре

- клиент: ForgeGradle runClient под Xvfb (llvmpipe, без GPU)
- воды в колонке (696,y,696): **58** блоков
- море: дно y=10, вода 51 блоков, глубочайшее дно y=1
- пещеры: 8497 колонок, воды 202460 / воздуха 11606 блоков (у свода 6145, висячего 916)
- провалов проб: **2**
- строк Exception/ERROR в логе клиента (без звука/наратора/authlib): **0**

## пробы
```
  /riftsea raw: riftsea floor=10 water=51 sea=62 min=1 max=15 cols=9409 caves=8497 wet=202460 air=11606 reef=1981 plants=1801 corals=64 float=916 ceilair=6145 bottom=-62 caveat=625,5,570 
  /riftsea raw: riftsea floor=10 water=51 sea=62 min=1 max=15 cols=9409 caves=8497 wet=202460 air=11606 reef=1981 plants=1801 corals=64 float=916 ceilair=6145 bottom=-62 caveat=625,5,570 
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
  рыба silver: Elisar Sea Fish has the following entity data: [696.4806291240423d, 39.91505675198468d, 693.92102680926d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[90f,0f],sf_variant:1} -> Summoned new Elisar Sea Fish
  рыба gold: Elisar Sea Fish has the following entity data: [696.4903145620211d, 39.90928345322609d, 693.9113092637284d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[90f,0f],sf_variant:2} -> Summoned new Elisar Sea Fish
  рыба striped: Elisar Sea Fish has the following entity data: [696.4903145620211d, 39.90928345322609d, 693.9113092637284d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[45f,0f],sf_variant:3} -> Summoned new Elisar Sea Fish
  рыба jelly: Elisar Sea Fish has the following entity data: [696.4806291240423d, 39.91497949801478d, 693.92102680926d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[45f,0f],sf_variant:4} -> Summoned new Elisar Sea Fish
  рыба ray: Elisar Sea Fish has the following entity data: [696.4903145620211d, 39.90928345322609d, 693.9113092637284d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[90f,0f],sf_variant:5} -> Summoned new Elisar Sea Fish
  рыба angler: Elisar Sea Fish has the following entity data: [696.4903145620211d, 39.90928345322609d, 693.9113092637284d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[90f,0f],sf_variant:6} -> Summoned new Elisar Sea Fish
  рыба eel: Elisar Sea Fish has the following entity data: [696.4903145620211d, 39.90928345322609d, 693.9113092637284d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[90f,0f],sf_variant:7} -> Summoned new Elisar Sea Fish
  рыба surgeon: Elisar Sea Fish has the following entity data: [696.4903145620211d, 39.90928345322609d, 693.9113092637284d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[90f,0f],sf_variant:8} -> Summoned new Elisar Sea Fish
  рыба turtle: Elisar Sea Fish has the following entity data: [696.4806291240423d, 39.91505675198468d, 693.92102680926d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[90f,0f],sf_variant:9} -> Summoned new Elisar Sea Fish
  рыба gloom: Elisar Sea Fish has the following entity data: [696.4806291240423d, 39.91497949801478d, 693.92102680926d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[90f,0f],sf_variant:10} -> Summoned new Elisar Sea Fish
  рыба sting: Elisar Sea Fish has the following entity data: [696.4806291240423d, 39.91497949801478d, 693.92102680926d]
  OK   клиент не жаловался на geo-модели
  /rifttree raw: rifttree at=-76,104,6 kind=ambra h=7 logs=31 branches=15 leaves=110 dist=69 loaded=true cam=-65,117,6 camtop=-65,127,6
  OK   дерево найдено: -76,104,6 (ветвей: 15)
  OK   у дерева есть ветки: 15 блоков вне ствола
  /rifttree повтор: rifttree at=85,103,-13 kind=ambra h=6 logs=39 branches=31 leaves=228 dist=80 loaded=true cam=96,115,-13 camtop=96,125,-13
  OK   дерево с ветками и кроной: ветвей 31, листвы 228
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
  /riftdtat у контрольного дуба -> riftdtat at=-9,104,19 dt=1 dy=153 lv=134 air=819 water=0 top=116:dynamictrees:oak_leaves col=rooty_elisar_soil,oak_branchx6,airx2,oak_branch,oak_leavesx3,airx3 blocks=rooty_elisar_soil=1 near=elisar_bricks=156 keep_stone
  саженец + костная мука -> riftgrow at=25,105,1 block=riftworlddt:sizo_sapling grew=8 after=riftworlddt:sizo_branch 
  /riftdtat у саженца -> riftdtat at=25,104,1 dt=77 dy=0 lv=68 air=896 water=0 top=112:riftworlddt:sizo_leaves col=rooty_elisar_soil,sizo_branchx6,sizo_leaves,airx7,temple_bricks blocks=rooty_elisar_soil=1 sizo_branch=8 sizo_leaves=68 near=elisa
  OK   саженец аддона вырос в дерево DT: блоков 77 (листвы 68)
  OK   DT вырастил дерево аддона: блоков 120 (ветвей 33, листвы 86)
  [БИОМ] секция садов и рощи
  режим игрока -> Set TestPlayer's game mode to Spectator Mode 
  /riftbiome gardens (поиск) -> riftbiome name=gardens at=406,-650 bio=riftworld:waterfall_gardens r=82 top=-2147483648 water=0 garden_grass=0 garden_leaves=0 garden_logs=0 ivy=0 planks=0 lantern=0 struct=- fall=399,-675 wall=411,86
  /riftbiome gardens (счёт) -> riftbiome name=gardens at=406,-650 bio=riftworld:waterfall_gardens r=82 top=104 water=7753 garden_grass=8147 garden_leaves=0 garden_logs=0 ivy=2784 planks=0 lantern=0 struct=- fall=399,-675 wall=411,86,-628:411,87,-639 river=287,92,-677:243,68,-650 isles=5 isl_r=37..82 sea=62 falls_water=1219 falls_src=81 garden_oak=10613 sizo_blocks=0 below_sea_cols=985 submerged_air=0 blocks=garden_limestone=114976 elisar_stone=48416 garden_limestone_mossy=20547 elisar_stone_mossy=12080 elisar_soil=11241 garden_oak_leaves=9246 
  OK   сады: биом острова riftworld:waterfall_gardens (биом-сурс совпадает с рельефом)
  /locate biome waterfall_gardens -> The nearest riftworld:waterfall_gardens is at [406, 130, -650] (0 blocks away) 
  /riftwhere у найденного биома -> riftwhere at=406,-650 biome=riftworld:waterfall_gardens top=104:riftworlddt:garden_oak_leaves ground=104:riftworlddt:garden_oak_leaves sea=62 garden=true 
  OK   /locate biome waterfall_gardens привёл на землю садов (406,130,-650, верх: riftworlddt:garden_oak_leaves, земля 104:riftworlddt:garden_oak_leaves)
  /riftaround у точки /locate -> riftaround at=406,106,-650 r=24 cols=2401 land=2097 garden_limestone=26875 garden_grass=1481 lavender=0 elisar_stone=12951 holes=0 gaps=0 
  OK   /locate привёл на настоящий садовый остров: известняка полок 26875 блоков в r=24
  OK   под островом садов нет дыр до бедрока (holes=0, трава садов 1481)
  OK   сады: зелёной травы 8147 блоков
  OK   сады: воды 7753 блоков (пруды и водопады, без затопления)
  OK   сады: плюща на стенах 2784 блоков
  OK   сады: рукотворного нет (досок 0, фонарей 0)
  OK   сады: островов 5 (увеличенный архипелаг 5–8 больших островов)
  OK   сады: разброс радиусов увеличенных островов 37..82
  OK   сады: садового дуба аддона DT 10613 блоков
  OK   сады: сизоствола нет (sizo_blocks=0)
  OK   сады: водопады текут — 1219 блоков струй, у 81 есть исток на кромке
  OK   сады: подводные каменные ярусы есть (985 колонок) и вода над ними не удалена (submerged_air=0)
  OK   сады: разбросанной листвы на земле нет (garden_leaves=0)
  кадр обрыва: камера 411 86 -628 -> взгляд 411 87 -639
  игрок после кадра обрыва -> TestPlayer has the following entity data: [411.5d, 86.0d, -627.5d] 
  /locate biome waterfall_gardens -> The nearest riftworld:waterfall_gardens is at [411, 86, -628] (0 blocks away) 
  /riftwhere у waterfall_gardens -> riftwhere at=411,-628 biome=riftworld:waterfall_gardens top=84:riftworld:elisar_moss ground=84:riftworld:elisar_moss sea=62 garden=true 
  OK   /locate biome waterfall_gardens привёл на землю биома (84:riftworld:elisar_moss)
  /locate biome ashen_lands -> The nearest riftworld:ashen_lands is at [731, 86, -916] (430 blocks away) 
  /riftbiome ashen -> riftbiome name=ashen at=-431,916 bio=riftworld:ashen_lands r=430 combo=7 top=104 castle=-469,882 
  OK   /locate biome ashen_lands нашёл случайный биом адского острова с замком (-431, 916)
  /riftbiome sizo (поиск) -> riftbiome name=sizo at=-342,2413 bio=riftworld:sizo_grove r=166 top=-2147483648 water=0 garden_grass=0 garden_leaves=0 garden_logs=0 ivy=0 planks=0 lantern=0 struct=carts=1 shrines=0 temples=1 pillar_
  /riftbiome sizo (счёт) -> riftbiome name=sizo at=-342,2413 bio=riftworld:sizo_grove r=166 top=130 water=38274 garden_grass=0 garden_leaves=0 garden_logs=0 ivy=0 planks=51 lantern=12 struct=carts=1 shrines=0 temples=1 pillar_pairs=2 fall=- wall=- river=- sizo_trail=1377 sizo_flowers=744 sizo_pillars=48 sview1=-426,112,2377:-305,113,2423 sview2=-402,111,2396:-286,111,2440 sview3=-360,113,2402:-282,114,2444 falls_water=0 falls_src=0 garden_oak=0 sizo_blocks=0 below_sea_cols=0 submerged_air=0 blocks=elisar_stone=439539 elisar_stone_mossy=46497 sizo_leaves=24935 elisar_crust=12078 eli
  /riftbiome sizo: структуры -> carts=1 shrines=0 temples=1 pillar_pairs=2 fall= trail=1377 flowers=744 pillars=48
  OK   роща сизо: мощёная тропа (1377), парные столбы с янтарём (48) и васильковые цветы (744)
  /riftdtwild (роща сизо) -> riftdtwild at=-342,2413 r=44 dt=18519 rooty=35 branch=1820 leaves=16664 blocks=sizo_leaves=16664 sizo_branch=1820 rooty_elisar_grass=33 rooty_elisar_moss=2 
  OK   роща сизо: гигантские динамические деревья сизо выросли (18519 блоков DT, кроны 16664)
  /riftbiome violet (поиск) -> riftbiome name=violet at=-989,-1202 bio=riftworld:violet_hills r=131 top=-2147483648 water=0 garden_grass=0 garden_leaves=0 garden_logs=0 ivy=0 planks=0 lantern=0 struct=- fall=- wall=- river=- vislan
  /riftbiome violet (счёт) -> riftbiome name=violet at=-989,-1202 bio=riftworld:violet_hills r=131 top=110 water=34344 garden_grass=0 garden_leaves=0 garden_logs=0 ivy=0 planks=23 lantern=9 struct=- fall=- wall=- river=- vislands=6 visl_r=82..132 violet_blooms=1978 violet_leaves=0 lavender=3648 accent_flowers=603 walls=92 fences=113 lamps=33 vground=82 road=-1077,88,-1223:-947,85,-1179 varch=-953,116,-1213:-796,86,-1109 vfields=-1031,99,-1216:-951,84,-1182 vpath=-994,84,-1197:-819,85,-1124 vtrees=-990,85,-1200:-993,86,-1190 falls_water=0 falls_src=0 garden_oak=0 sizo_blocks=0 below_s
  OK   холмы: увеличенный архипелаг из 6 огромных парящих островов
  OK   холмы: сизоствола нет (sizo_blocks=0)
  OK   холмы: воздушных цветов всего 5626 блоков (лаванда 3648 + соцветия 1978)
  OK   холмы: гряд лаванды 3648 блоков (воздушные ряды)
  OK   холмы: вдоль дороги каменные стенки (92), деревянные заборы (113) и столбы с факелами (33)
  OK   холмы: других цветов в малом количестве (603 на фоне 5626 фиолетовых)
  /riftdtwild (холмы, лиловый ветвецвет) -> riftdtwild at=-989,-1202 r=40 dt=2291 rooty=0 branch=293 leaves=1998 blocks=lilac_leaves=1998 lilac_branch=293 
  OK   холмы: динамических деревьев аддона 2291 блоков (лиловый ветвецвет)
  OK   роща: биом острова riftworld:sizo_grove
  /locate biome sizo_grove -> The nearest riftworld:sizo_grove is at [579, 86, -2194] (1855 blocks away) 
  /riftwhere у sizo_grove -> riftwhere at=579,-2194 biome=riftworld:sizo_grove top=109:riftworlddt:sizo_leaves ground=109:riftworlddt:sizo_leaves sea=62 garden=false 
  OK   /locate biome sizo_grove привёл на землю биома (109:riftworlddt:sizo_leaves)
  OK   Фиолетовые холмы: биом луга riftworld:violet_hills
  /locate biome violet_hills -> The nearest riftworld:violet_hills is at [-989, 86, -1202] (0 blocks away) 
  /riftwhere у violet_hills -> riftwhere at=-989,-1202 biome=riftworld:violet_hills top=94:riftworlddt:lilac_leaves ground=94:riftworlddt:lilac_leaves sea=62 garden=false 
  OK   /locate biome violet_hills привёл на землю биома (94:riftworlddt:lilac_leaves)
  /riftdtwhy (холмы) -> riftdtwhy at=-989,86,-1202 biome_surface=riftworld:violet_hills biome_top=riftworld:violet_hills biome_y0=riftworld:violet_hills dt_features=2 has_dt=true dt_species=handled:Species{registryName=riftworlddt:lilac} db_entries=7 db_populated=true plains_? sel=StaticSpeciesSelector 
  OK   DT/холмы: грунт найден
  OK   DT/холмы: почва приемлема для DT
  OK   DT/холмы: база DT ответила по виду
  /riftdtwild (холмы) -> riftdtwild at=-989,-1202 r=32 dt=1161 rooty=0 branch=150 leaves=1011 blocks=lilac_leaves=1011 lilac_branch=150 
  /riftdtdbg (холмы) -> riftdtdbg at=-989,86,-1202 discs=2 disc0_r=7 real=true ground=82:Block{minecraft:grass_block} handled=true sp=Species{registryName=riftworlddt:lilac} codes=7[2:12 3:12 4:12 5:12 6:12 7:12 8:12 ] code_for_r7=ok gen=false 
  OK   DT растёт в мире сам: 1161 блоков аддона вокруг точки
  /locate biome crystal_rifts -> The nearest riftworld:crystal_rifts is at [-2461, 86, -2738] (2127 blocks away) 
  /riftwhere у crystal_rifts -> riftwhere at=-2461,-2738 biome=riftworld:crystal_rifts top=87:riftworld:rift_turf ground=87:riftworld:rift_turf sea=62 garden=false 
  OK   /locate biome crystal_rifts привёл на землю биома (87:riftworld:rift_turf)
  /riftbiome crystal -> riftbiome name=crystal at=-1187,781 isl1=-1156,970 bio=riftworld:crystal_rifts cislands=6 cbridges=7 bridge_walkable=1 crystal_blocks=3203 marble_blocks=436633 rift_turf=8624 rift_grass=1943 vanilla_grass=0 isl1_trees=0 crystal_flora=2548 water=473 ivy=53 sizo_blocks=0 cview1=-1225,91,740:-1168,91,802 cview2=-1177,92,860:-1169,91,878 cview3=-1156,96,941:-115
  OK   Хрустальные разломы: биом святилища riftworld:crystal_rifts
  OK   Хрустальные разломы: архипелаг из 6 островов и 7 арочных мостов
  OK   Хрустальные разломы: каменный арочный мост непрерывен и полностью проходим пешком
  OK   Хрустальные разломы: многоблочные кристаллы на всех островах (3203), мрамор/сланец (436633), чаши/водопады (473)
  OK   Хрустальные разломы: собственный шалфейный дёрн (8624) и трава разлома (1943), ванильной травы=0, деревьев на кристальном острове=0
  OK   Хрустальные разломы: чужих деревьев сизо нет (sizo_blocks=0)
  /riftdtwild (хрустальные разломы, мини-бонсай серебрица) -> riftdtwild at=-1187,781 r=44 dt=39 rooty=4 branch=24 leaves=11 blocks=crystal_birch_branch=24 crystal_birch_leaves=11 rooty_rift_turf=4 
  OK   Хрустальные разломы: миниатюрные редкие деревца DT вдоль аллеи (39 блоков, без лесов и без ambra)
  /riftstruct crashed_ship -> riftstruct kind=crashed_ship at=937,66,155 variant=0 blocks=1393 rock=617 sub_air=0 cam=920,74,138:934,71,161 
  OK   структура crashed_ship построена: blocks=1393, sub_air=0
  /riftstruct sunken_ship -> riftstruct kind=sunken_ship at=-775,15,264 variant=0 blocks=1163 sub_air=0 cam=-775,21,278:-773,16,263 
  OK   структура sunken_ship построена: blocks=1163, sub_air=0
  /riftstruct ocean_building -> riftstruct kind=ocean_building at=-551,15,303 variant=2 blocks=162 sub_air=0 cam=-539,21,315:-551,16,303 
  OK   структура ocean_building построена: blocks=162, sub_air=0
  /riftstruct white_village -> riftstruct kind=white_village at=-810,97,-365 variant=0 houses=5 blocks=637 sub_air=0 cam=-802,108,-341:-810,100,-369 
  OK   структура white_village построена: blocks=637, sub_air=0
  OK   деревня white_village содержит ровно 5 небольших домов (лимит <= 6 соблюдён)
  /riftstruct burned_village -> riftstruct kind=burned_village at=-489,64,1105 bio=riftworld:ashen_lands variant=1 houses=5 blocks=793 fires=11 sub_air=0 cam=-482,74,1128:-489,67,1102 
  OK   структура burned_village построена: blocks=793, sub_air=0
  OK   деревня burned_village содержит ровно 5 небольших домов (лимит <= 6 соблюдён)
  /riftstruct stilt_village -> riftstruct kind=stilt_village at=382,65,-567 variant=0 houses=6 blocks=3732 sub_air=0 cam=368,78,-542:383,68,-573 
  OK   структура stilt_village построена: blocks=3732, sub_air=0
  OK   деревня stilt_village содержит ровно 6 небольших домов (лимит <= 6 соблюдён)
  /riftstruct city_castle_front -> riftstruct kind=city_castle_front at=0,112,36 main_plots=48 north_plots=11 blocks=4712 sub_air=0 cam=14,122,58:0,121,6 
  OK   структура city_castle_front построена: blocks=4712, sub_air=0
  /riftstruct city_castle_aerial -> riftstruct kind=city_castle_aerial at=0,112,0 main_plots=48 north_plots=11 blocks=4712 sub_air=0 cam=48,154,54:0,118,-6 
  OK   структура city_castle_aerial построена: blocks=4712, sub_air=0
  /riftstruct city_castle_north -> riftstruct kind=city_castle_north at=0,112,-54 main_plots=48 north_plots=11 blocks=4712 sub_air=0 cam=-18,132,-78:2,112,-30 
  OK   структура city_castle_north построена: blocks=4712, sub_air=0
  /riftstruct colonnade_boss -> riftstruct kind=colonnade_boss at=-1176,87,793 bosses=1 minions=2 attack_ok=0 blocks=120 sub_air=0 cam=-1183,91,785:-1176,93,793 
  OK   структура colonnade_boss построена: blocks=120, sub_air=0
  /riftstruct colonnade_boss_minion -> riftstruct kind=colonnade_boss_minion at=-1176,87,793 bosses=1 minions=2 attack_ok=0 blocks=120 sub_air=0 cam=-1178,89,785:-1176,90,787 
  OK   структура colonnade_boss_minion построена: blocks=120, sub_air=0
  OK   3D-оружие Колоннады (crystal_katar, crystal_barrage, crystal_aegis, astral_prism) выдано и отснято
  /riftstruct ocean_island_waves -> riftstruct kind=ocean_island_waves at=227,63,-261 blocks=220 sub_air=0 cam=263,82,-303:227,64,-261 
  OK   структура ocean_island_waves построена: blocks=220, sub_air=0
  /riftstruct waterfall_ocean_waves -> riftstruct kind=waterfall_ocean_waves at=-1191,63,880 blocks=200 sub_air=0 cam=-1190,76,856:-1192,64,877 
  OK   структура waterfall_ocean_waves построена: blocks=200, sub_air=0
  /riftstruct harbor_port -> riftstruct kind=harbor_port at=-362,64,-544 island_top=68 blocks=44232 sub_air=0 cam=-336,81,-516:-364,68,-554 
  OK   структура harbor_port построена: blocks=44232, sub_air=0
  /riftstruct grand_theater -> riftstruct kind=grand_theater at=-612,99,246 blocks=6045 sub_air=0 cam=-602,115,268:-612,103,240 
  OK   структура grand_theater построена: blocks=6045, sub_air=0
  /riftstruct sea_lighthouse -> riftstruct kind=sea_lighthouse at=172,69,621 blocks=30154 sub_air=0 cam=196,78,647:172,85,621 
  OK   структура sea_lighthouse построена: blocks=30154, sub_air=0
  /riftstruct ancient_tree_altar -> riftstruct kind=ancient_tree_altar at=821,102,-2292 blocks=2857 sub_air=0 cam=836,109,-2277:821,108,-2292 
  OK   структура ancient_tree_altar построена: blocks=2857, sub_air=0
  /riftstruct ashen_belfry_barrow -> riftstruct kind=ashen_belfry_barrow at=-338,64,1081 blocks=1616 sub_air=0 cam=-342,75,1107:-336,74,1081 
  OK   структура ashen_belfry_barrow построена: blocks=1616, sub_air=0
  /riftstruct shattered_shard_forge -> riftstruct kind=shattered_shard_forge at=-1012,93,819 blocks=3068 sub_air=0 cam=-1005,101,829:-1013,97,817 
  OK   структура shattered_shard_forge построена: blocks=3068, sub_air=0
  /riftstruct violet_wind_observatory -> riftstruct kind=violet_wind_observatory at=-993,98,-880 blocks=2024 sub_air=0 cam=-977,108,-861:-994,104,-874 
  OK   структура violet_wind_observatory построена: blocks=2024, sub_air=0
  /riftstruct elisar_eye_occlusion -> riftstruct kind=elisar_eye_occlusion at=170,73,605 blocks=250 sub_air=0 cam=170,73,605:179,93,629 
  OK   структура elisar_eye_occlusion построена: blocks=250, sub_air=0
  /riftstruct colonnade_boss_pillars -> riftstruct kind=colonnade_boss_pillars at=-1176,87,793 bosses=1 minions=2 attack_ok=1 blocks=170 sub_air=0 cam=-1199,96,777:-1182,91,787 
  OK   структура colonnade_boss_pillars построена: blocks=170, sub_air=0
  /riftstruct colonnade_boss_hail -> riftstruct kind=colonnade_boss_hail at=-1176,87,793 bosses=1 minions=2 attack_ok=1 blocks=170 sub_air=0 cam=-1201,96,774:-1183,97,786 
  OK   структура colonnade_boss_hail построена: blocks=170, sub_air=0
  /riftstruct origami_dragon_closeup -> riftstruct kind=origami_dragon_closeup at=-59,107,454 dragons=1 blocks=220 sub_air=0 cam=-56,109,465:-53,106,451 
  OK   структура origami_dragon_closeup построена: blocks=220, sub_air=0
  /riftstruct origami_dragon_under_release -> riftstruct kind=origami_dragon_under_release at=-73,76,425 dragons=1 blocks=220 sub_air=0 cam=-69,72,456:-76,78,427 
  OK   структура origami_dragon_under_release построена: blocks=220, sub_air=0
  /riftstruct origami_dragon_over_release -> riftstruct kind=origami_dragon_over_release at=-82,116,407 dragons=1 blocks=220 sub_air=0 cam=-82,122,431:-85,113,409 
  OK   структура origami_dragon_over_release построена: blocks=220, sub_air=0
  /riftstruct blacktide_citadel -> riftstruct kind=blacktide_citadel at=-841,84,1264 bridge_land=true castle_sea=true water_ring=0 land_blocks=0 blocks=0 sub_air=0 cam=-863,114,1135:-832,100,1256 
  FAIL структура blacktide_citadel: blocks=0 (ожидалось >=6000), sub_air=0
  /riftstruct blacktide_bridge_shore -> riftstruct kind=blacktide_bridge_shore at=-776,84,1229 bridge_land=true castle_sea=true water_ring=1 land_blocks=8562 blocks=0 sub_air=0 cam=-731,90,1146:-790,94,1241 
  FAIL структура blacktide_bridge_shore: blocks=0 (ожидалось >=6000), sub_air=0
  /riftstruct ashen_burned_church -> riftstruct kind=ashen_burned_church at=-413,64,706 blocks=2211 sub_air=0 cam=-438,75,724:-415,72,706 
  OK   структура ashen_burned_church построена: blocks=2211, sub_air=0
  /riftstruct ashen_combo_matrix -> riftstruct kind=ashen_combo_matrix at=2938,64,829 empty=7 partial=29 all_three=5 ash_r=408 blocks=995 sub_air=0 cam=2890,88,877:2980,66,787 
  OK   структура ashen_combo_matrix построена: blocks=995, sub_air=0
  /riftstruct enlarged_biomes_audit -> riftstruct kind=enlarged_biomes_audit at=-3910,117,1925 ash_r=434 violet_r=146 crystal_r=83 sizo_r=176 gardens_r=82 mist_r=99 blocks=600 sub_air=0 cam=-4005,145,2020:-3880,99,1895 
  OK   структура enlarged_biomes_audit построена: blocks=600, sub_air=0
  /riftportal setup -> riftportal setup center=24,-60,0 snapshot=2435 cam=28,-57,24:24,-49,0 
  /riftportal trigger -> riftportal trigger started=true 
  /riftportal walkin -> riftportal walkin started=true 
  после захода в белый пролом и катсцены: Dimension=TestPlayer has the following entity data: "riftworld:elisar"  | riftportal verify mutated=0 total=2435 
  OK   белый 3D-Пролом при входе бесшовно перенёс в Белый Город с катсценой, изменённых блоков: 0
  игрок: TestPlayer has the following entity data: [0.5d, 105.0d, 22.5d]
  рыбы:  Elisar Sea Fish has the following entity data: [644.3743852474523d, 38.27537781787736d, 559.9877100379534d]
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

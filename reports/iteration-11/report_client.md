# CI, шаг 2: настоящий клиент Minecraft в Элисаре

- клиент: ForgeGradle runClient под Xvfb (llvmpipe, без GPU)
- воды в колонке (696,y,696): **57** блоков
- море: дно y=10, вода 51 блоков, глубочайшее дно y=1
- пещеры: 8497 колонок, воды 202460 / воздуха 11606 блоков (у свода 6145, висячего 916)
- провалов проб: **0**
- строк Exception/ERROR в логе клиента (без звука/наратора): **0**

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
  рыба jelly: Elisar Sea Fish has the following entity data: [696.4903145620211d, 39.90928345322609d, 693.9113092637284d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[45f,0f],sf_variant:4} -> Summoned new Elisar Sea Fish
  рыба ray: Elisar Sea Fish has the following entity data: [696.4903145620211d, 39.90928345322609d, 693.9113092637284d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[90f,0f],sf_variant:5} -> Summoned new Elisar Sea Fish
  рыба angler: Elisar Sea Fish has the following entity data: [696.4903145620211d, 39.90928345322609d, 693.9113092637284d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[90f,0f],sf_variant:6} -> Summoned new Elisar Sea Fish
  рыба eel: Elisar Sea Fish has the following entity data: [696.4903145620211d, 39.90928345322609d, 693.9113092637284d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[90f,0f],sf_variant:7} -> Summoned new Elisar Sea Fish
  рыба surgeon: Elisar Sea Fish has the following entity data: [696.4903145620211d, 39.90928345322609d, 693.9113092637284d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[90f,0f],sf_variant:8} -> Summoned new Elisar Sea Fish
  рыба turtle: Elisar Sea Fish has the following entity data: [696.4903145620211d, 39.90928345322609d, 693.9113092637284d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[90f,0f],sf_variant:9} -> Summoned new Elisar Sea Fish
  рыба gloom: Elisar Sea Fish has the following entity data: [696.4903145620211d, 39.90928345322609d, 693.9113092637284d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[90f,0f],sf_variant:10} -> Summoned new Elisar Sea Fish
  рыба sting: Elisar Sea Fish has the following entity data: [696.4806291240423d, 39.91497949801478d, 693.92102680926d]
  OK   клиент не жаловался на geo-модели
  /rifttree raw: rifttree at=-76,104,6 kind=ambra h=7 logs=16 branches=0 leaves=0 dist=69 loaded=true cam=-65,117,6 camtop=-65,127,6
  OK   дерево найдено: -76,104,6 (ветвей: 0)
  OK   обычных деревьев генератора в плане нет (активен аддон DT)
  /rifttree повтор: rifttree at=85,103,-13 kind=ambra h=6 logs=23 branches=15 leaves=123 dist=80 loaded=true cam=96,115,-13 camtop=96,125,-13
  OK   дерево с ветками и кроной: ветвей 15, листвы 123
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
  /riftdtat у посадки дерева -> riftdtat at=9,104,9 dt=191 dy=0 lv=157 air=754 water=0 top=116:riftworlddt:sizo_leaves col=rooty_elisar_soil,sizo_branchx6,air,sizo_leavesx7,air blocks=rooty_elisar_soil=1 sizo_branch=33 sizo_leaves=157 near=elisar_bricks=171 temple_floor=61 garden_water=28 ashlar=4 temple_mosaic=4 elisar_cobble=2
  /dynamictrees gettree -> Species: Sizo Tree JoCode: JJJzJJxx98+RJOOPvn1JJ8JP  
  контрольный дуб: Successfully set tree at position -9, 104, 19 to species Oak with JoCode JJJ0JR7Jxx+nXv.  
  /riftdtat у контрольного дуба -> riftdtat at=-9,104,19 dt=1 dy=155 lv=136 air=817 water=0 top=116:dynamictrees:oak_leaves col=rooty_elisar_soil,oak_branchx6,airx2,oak_branch,oak_leavesx3,airx3 blocks=rooty_elisar_soil=1 near=elisar_bricks=156 keep_stone
  саженец + костная мука -> riftgrow at=25,105,1 block=riftworlddt:sizo_sapling grew=8 after=riftworlddt:sizo_branch  
  /riftdtat у саженца -> riftdtat at=25,104,1 dt=49 dy=0 lv=41 air=924 water=0 top=113:riftworlddt:sizo_leaves col=rooty_elisar_soil,sizo_branchx7,sizo_leavesx2,airx5,temple_bricks blocks=rooty_elisar_soil=1 sizo_branch=7 sizo_leaves=41 near=eli
  OK   саженец аддона вырос в дерево DT: блоков 49 (листвы 41)
  OK   DT вырастил дерево аддона: блоков 191 (ветвей 33, листвы 157)
  [БИОМ] секция садов и рощи
  режим игрока -> Set TestPlayer's game mode to Spectator Mode  
  /riftbiome gardens (поиск) -> riftbiome name=gardens at=-470,-1349 bio=riftworld:waterfall_gardens r=46 top=-2147483648 water=0 garden_grass=0 garden_leaves=0 garden_logs=0 ivy=0 planks=0 lantern=0 struct=- fall=-471,-1331 wall=-4
  /riftbiome gardens (счёт) -> riftbiome name=gardens at=-470,-1349 bio=riftworld:waterfall_gardens r=46 top=104 water=14417 garden_grass=2962 garden_leaves=0 garden_logs=0 ivy=3279 planks=0 lantern=0 struct=- fall=-471,-1331 wall=-457,79,-1318:-457,80,-1329 river=-412,92,-1339:-440,68,-1383 isles=4 isl_r=28..46 sea=62 falls_water=931 falls_src=70 garden_oak=2867 sizo_blocks=0 below_sea_cols=1474 submerged_air=0 blocks=garden_limestone=46102 elisar_stone=22780 garden_limestone_mossy=8086 elisar_stone_mossy=4863 elisar_soil=4552 garden_oak_leaves=2500  
  OK   сады: биом острова riftworld:waterfall_gardens (биом-сурс совпадает с рельефом)
  /locate biome waterfall_gardens -> The nearest riftworld:waterfall_gardens is at [-470, 130, -1349] (0 blocks away)  
  /riftwhere у найденного биома -> riftwhere at=-470,-1349 biome=riftworld:waterfall_gardens top=98:riftworld:garden_grass ground=98:riftworld:garden_grass sea=62 garden=true  
  OK   /locate biome waterfall_gardens привёл на землю садов (-470,130,-1349, верх: riftworld:garden_grass, земля 98:riftworld:garden_grass)
  /riftaround у точки /locate -> riftaround at=-470,100,-1349 r=24 cols=2401 land=1933 garden_limestone=23604 garden_grass=1420 lavender=0 elisar_stone=15959 holes=0 gaps=0  
  OK   /locate привёл на настоящий садовый остров: известняка полок 23604 блоков в r=24
  OK   под островом садов нет дыр до бедрока (holes=0, трава садов 1420)
  OK   сады: зелёной травы 2962 блоков
  OK   сады: воды 14417 блоков (пруды и водопады, без затопления)
  OK   сады: плюща на стенах 3279 блоков
  OK   сады: рукотворного нет (досок 0, фонарей 0)
  OK   сады: островов 4 (3–6 больших)
  OK   сады: разброс радиусов островов 28..46 (от 22..38 до 43..55)
  OK   сады: садового дуба аддона DT 2867 блоков
  OK   сады: сизоствола нет (sizo_blocks=0)
  OK   сады: водопады текут — 931 блоков струй, у 70 есть исток на кромке
  OK   сады: подводные каменные ярусы есть (1474 колонок) и вода над ними не удалена (submerged_air=0)
  OK   сады: разбросанной листвы на земле нет (garden_leaves=0)
  кадр обрыва: камера -457 79 -1318 -> взгляд -457 80 -1329
  игрок после кадра обрыва -> TestPlayer has the following entity data: [-456.5d, 79.0d, -1317.5d]  
  /locate biome waterfall_gardens -> The nearest riftworld:waterfall_gardens is at [-457, 79, -1318] (0 blocks away)  
  /riftwhere у waterfall_gardens -> riftwhere at=-457,-1318 biome=riftworld:waterfall_gardens top=77:riftworld:garden_grass ground=77:riftworld:garden_grass sea=62 garden=true  
  OK   /locate biome waterfall_gardens привёл на землю биома (77:riftworld:garden_grass)
  /locate biome ashen_lands -> The nearest riftworld:ashen_lands is at [2295, 79, -2758] (3105 blocks away)  
  /riftbiome ashen -> riftbiome name=ashen at=-832,2167 bio=riftworld:ashen_lands r=74 top=104 castle=-870,2133  
  OK   /locate biome ashen_lands нашёл случайный биом адского острова с замком (-832, 2167)
  /riftbiome sizo (поиск) -> riftbiome name=sizo at=-240,841 bio=riftworld:sizo_grove r=89 top=-2147483648 water=0 garden_grass=0 garden_leaves=0 garden_logs=0 ivy=0 planks=0 lantern=0 struct=carts=1 shrines=0 temples=1 pillar_pa
  /riftbiome sizo (счёт) -> riftbiome name=sizo at=-240,841 bio=riftworld:sizo_grove r=89 top=128 water=38307 garden_grass=0 garden_leaves=0 garden_logs=0 ivy=0 planks=49 lantern=0 struct=carts=1 shrines=0 temples=1 pillar_pairs=2 fall=- wall=- river=- sizo_trail=1677 sizo_flowers=901 sizo_pillars=8 sview1=-280,110,812:-220,110,850 sview2=-270,108,823:-210,110,859 sview3=-247,110,832:-208,113,861 falls_water=0 falls_src=0 garden_oak=0 sizo_blocks=0 below_sea_cols=0 submerged_air=0 blocks=elisar_stone=394761 elisar_stone_mossy=42334 sizo_leaves=24914 elisar_crust=12082 elisar_grass=
  /riftbiome sizo: структуры -> carts=1 shrines=0 temples=1 pillar_pairs=2 fall= trail=1677 flowers=901 pillars=8
  OK   роща сизо: мощёная тропа (1677), парные столбы с янтарём (8) и васильковые цветы (901)
  /riftdtwild (роща сизо) -> riftdtwild at=-240,841 r=44 dt=16712 rooty=26 branch=1348 leaves=15338 blocks=sizo_leaves=15338 sizo_branch=1348 rooty_elisar_grass=25 rooty_elisar_moss=1  
  OK   роща сизо: гигантские динамические деревья сизо выросли (16712 блоков DT, кроны 15338)
  /riftbiome violet (поиск) -> riftbiome name=violet at=2893,-311 bio=riftworld:violet_hills r=88 top=-2147483648 water=0 garden_grass=0 garden_leaves=0 garden_logs=0 ivy=0 planks=0 lantern=0 struct=- fall=- wall=- river=- vislands
  /riftbiome violet (счёт) -> riftbiome name=violet at=2893,-311 bio=riftworld:violet_hills r=88 top=107 water=38307 garden_grass=0 garden_leaves=0 garden_logs=0 ivy=0 planks=39 lantern=10 struct=- fall=- wall=- river=- vislands=5 visl_r=56..88 violet_blooms=2095 violet_leaves=0 lavender=3978 accent_flowers=617 walls=97 fences=108 lamps=33 vground=83 road=2894,89,-361:2921,86,-273 varch=2935,115,-298:2923,85,-145 vfields=2912,96,-332:2920,85,-277 vpath=2903,85,-305:2914,84,-162 vtrees=2897,87,-313:2907,88,-317 falls_water=0 falls_src=0 garden_oak=0 sizo_blocks=0 below_sea_cols=0 subm
  OK   холмы: архипелаг из 5 огромных парящих островов
  OK   холмы: сизоствола нет (sizo_blocks=0)
  OK   холмы: воздушных цветов всего 6073 блоков (лаванда 3978 + соцветия 2095)
  OK   холмы: гряд лаванды 3978 блоков (воздушные ряды)
  OK   холмы: вдоль дороги каменные стенки (97), деревянные заборы (108) и столбы с факелами (33)
  OK   холмы: других цветов в малом количестве (617 на фоне 6073 фиолетовых)
  /riftdtwild (холмы, лиловый ветвецвет) -> riftdtwild at=2893,-311 r=40 dt=2242 rooty=0 branch=286 leaves=1956 blocks=lilac_leaves=1956 lilac_branch=286  
  OK   холмы: динамических деревьев аддона 2242 блоков (лиловый ветвецвет)
  OK   роща: биом острова riftworld:sizo_grove
  /locate biome sizo_grove -> The nearest riftworld:sizo_grove is at [909, 87, -2295] (2805 blocks away)  
  /riftwhere у sizo_grove -> riftwhere at=909,-2295 biome=riftworld:sizo_grove top=112:riftworlddt:sizo_leaves ground=112:riftworlddt:sizo_leaves sea=62 garden=false  
  OK   /locate biome sizo_grove привёл на землю биома (112:riftworlddt:sizo_leaves)
  OK   Фиолетовые холмы: биом луга riftworld:violet_hills
  /locate biome violet_hills -> The nearest riftworld:violet_hills is at [2893, 87, -311] (0 blocks away)  
  /riftwhere у violet_hills -> riftwhere at=2893,-311 biome=riftworld:violet_hills top=84:riftworld:lavender_bed ground=84:riftworld:lavender_bed sea=62 garden=false  
  OK   /locate biome violet_hills привёл на землю биома (84:riftworld:lavender_bed)
  /riftdtwhy (холмы) -> riftdtwhy at=2893,87,-311 biome_surface=riftworld:violet_hills biome_top=riftworld:violet_hills biome_y0=riftworld:violet_hills dt_features=2 has_dt=true dt_species=handled:Species{registryName=riftworlddt:lilac} db_entries=8 db_populated=true plains_? sel=StaticSpeciesSelector g
  OK   DT/холмы: грунт найден
  OK   DT/холмы: почва приемлема для DT
  OK   DT/холмы: база DT ответила по виду
  /riftdtwild (холмы) -> riftdtwild at=2893,-311 r=32 dt=1447 rooty=0 branch=176 leaves=1271 blocks=lilac_leaves=1271 lilac_branch=176  
  /riftdtdbg (холмы) -> riftdtdbg at=2893,87,-311 discs=2 disc0_r=6 real=true ground=83:Block{minecraft:grass_block} handled=true sp=Species{registryName=riftworlddt:lilac} codes=7[2:12 3:12 4:12 5:12 6:12 7:12 8:12 ] code_for_r6=ok gen=true  
  OK   DT растёт в мире сам: 1447 блоков аддона вокруг точки
  /locate biome crystal_rifts -> The nearest riftworld:crystal_rifts is at [3693, 87, -663] (874 blocks away)  
  /riftwhere у crystal_rifts -> riftwhere at=3693,-663 biome=riftworld:crystal_rifts top=91:riftworld:rift_turf ground=91:riftworld:rift_turf sea=62 garden=false  
  OK   /locate biome crystal_rifts привёл на землю биома (91:riftworld:rift_turf)
  /riftbiome crystal -> riftbiome name=crystal at=-647,473 isl1=-709,351 bio=riftworld:crystal_rifts cislands=6 cbridges=6 bridge_walkable=1 crystal_blocks=4182 marble_blocks=386589 rift_turf=9626 rift_grass=2267 vanilla_grass=0 isl1_trees=0 crystal_flora=2917 water=1090 ivy=1200 sizo_blocks=0 cview1=-618,91,490:-667,91,462 cview2=-666,92,428:-678,92,411 cview3=-697,99,377:-701,100
  OK   Хрустальные разломы: биом святилища riftworld:crystal_rifts
  OK   Хрустальные разломы: архипелаг из 6 островов и 6 арочных мостов
  OK   Хрустальные разломы: каменный арочный мост непрерывен и полностью проходим пешком
  OK   Хрустальные разломы: многоблочные кристаллы на всех островах (4182), мрамор/сланец (386589), чаши/водопады (1090)
  OK   Хрустальные разломы: собственный шалфейный дёрн (9626) и трава разлома (2267), ванильной травы=0, деревьев на кристальном острове=0
  OK   Хрустальные разломы: чужих деревьев сизо нет (sizo_blocks=0)
  /riftdtwild (хрустальные разломы, мини-бонсай серебрица) -> riftdtwild at=-647,473 r=44 dt=70 rooty=6 branch=40 leaves=24 blocks=crystal_birch_branch=40 crystal_birch_leaves=24 rooty_rift_turf=6  
  OK   Хрустальные разломы: миниатюрные редкие деревца DT вдоль аллеи (70 блоков, без лесов и без ambra)
  /riftstruct crashed_ship -> riftstruct kind=crashed_ship at=295,66,-804 variant=0 blocks=1381 rock=286 sub_air=0 cam=283,74,-824:290,71,-799  
  OK   структура crashed_ship построена: blocks=1381, sub_air=0
  /riftstruct sunken_ship -> riftstruct kind=sunken_ship at=468,15,-452 variant=2 blocks=1064 sub_air=0 cam=455,21,-459:468,16,-450  
  OK   структура sunken_ship построена: blocks=1064, sub_air=0
  /riftstruct ocean_building -> riftstruct kind=ocean_building at=520,14,-351 variant=1 blocks=509 sub_air=0 cam=532,20,-339:520,15,-351  
  OK   структура ocean_building построена: blocks=509, sub_air=0
  /riftstruct white_village -> riftstruct kind=white_village at=-810,97,-365 variant=0 houses=5 blocks=637 sub_air=0 cam=-802,108,-341:-810,100,-369  
  OK   структура white_village построена: blocks=637, sub_air=0
  OK   деревня white_village содержит ровно 5 небольших домов (лимит <= 6 соблюдён)
  /riftstruct burned_village -> riftstruct kind=burned_village at=-712,64,2055 bio=riftworld:shallow_reef variant=1 houses=4 blocks=669 fires=9 sub_air=0 cam=-705,74,2078:-712,67,2052  
  OK   структура burned_village построена: blocks=669, sub_air=0
  OK   деревня burned_village содержит ровно 4 небольших домов (лимит <= 6 соблюдён)
  /riftstruct stilt_village -> riftstruct kind=stilt_village at=-456,65,-1260 variant=2 houses=4 blocks=2755 sub_air=0 cam=-459,78,-1231:-457,68,-1266  
  OK   структура stilt_village построена: blocks=2755, sub_air=0
  OK   деревня stilt_village содержит ровно 4 небольших домов (лимит <= 6 соблюдён)
  /riftstruct city_castle_front -> riftstruct kind=city_castle_front at=0,112,36 main_plots=48 north_plots=11 blocks=4725 sub_air=0 cam=14,122,58:0,121,6  
  OK   структура city_castle_front построена: blocks=4725, sub_air=0
  /riftstruct city_castle_aerial -> riftstruct kind=city_castle_aerial at=0,112,0 main_plots=48 north_plots=11 blocks=4725 sub_air=0 cam=48,154,54:0,118,-6  
  OK   структура city_castle_aerial построена: blocks=4725, sub_air=0
  /riftstruct city_castle_north -> riftstruct kind=city_castle_north at=0,112,-54 main_plots=48 north_plots=11 blocks=4725 sub_air=0 cam=-18,132,-78:2,112,-30  
  OK   структура city_castle_north построена: blocks=4725, sub_air=0
  /riftstruct colonnade_boss -> riftstruct kind=colonnade_boss at=-656,87,468 bosses=1 minions=2 blocks=120 sub_air=0 cam=-651,90,471:-656,90,468  
  OK   структура colonnade_boss построена: blocks=120, sub_air=0
  OK   3D-оружие Колоннады (crystal_katar, crystal_barrage, crystal_aegis, astral_prism) выдано и отснято
  игрок: TestPlayer has the following entity data: [-650.5d, 89.0d, 471.5d]
  рыбы:  Elisar Sea Fish has the following entity data: [621.7848062713748d, 51.56339316912559d, 538.1119765814547d]
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

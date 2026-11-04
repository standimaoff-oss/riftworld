# CI, шаг 2: настоящий клиент Minecraft в Элисаре

- клиент: ForgeGradle runClient под Xvfb (llvmpipe, без GPU)
- воды в колонке (696,y,696): **58** блоков
- море: дно y=10, вода 51 блоков, глубочайшее дно y=1
- пещеры: 8497 колонок, воды 202732 / воздуха 11560 блоков (у свода 6121, висячего 916)
- **клиент потерян посреди прогона**: оставшиеся кадры и пробы не снимались (см. маркер в client.log)
- провалов проб: **53**
- строк Exception/ERROR в логе клиента (без звука/наратора/authlib): **5**

## пробы
```
  /riftsea raw: riftsea floor=10 water=51 sea=62 min=1 max=15 cols=9409 caves=8497 wet=202732 air=11560 reef=1981 plants=1773 corals=64 float=916 ceilair=6121 bottom=-62 caveat=625,5,570 
  /riftsea raw: riftsea floor=10 water=51 sea=62 min=1 max=15 cols=9409 caves=8497 wet=202732 air=11560 reef=1981 plants=1773 corals=64 float=916 ceilair=6121 bottom=-62 caveat=625,5,570 
  OK   вода на глубине (696,56,696) = elisar_water
  /riftsea raw: riftsea floor=5 water=56 sea=62 min=-8 max=14 cols=9133 caves=8240 wet=158344 air=13016 reef=2186 plants=1402 corals=86 float=1021 ceilair=7334 bottom=-62 caveat=650,3,600 
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
  /riftstruct v34_pegasus -> riftstruct kind=v34_pegasus at=-6822,99,4271 alive=1 tamed=0 where=morok_hollow spawned=true saddleable=true saddle_seat=1.15 blocks=900 sub_air=0 cam=-6822.225,99.1,4267.156:-6821.5,98.2,4271.5 stage=13x13 
  OK   структура v34_pegasus построена: blocks=900, sub_air=0
  /riftstruct v35_wendigo -> riftstruct kind=v35_wendigo at=-6822,101,4271 alive=1 hp=64 darkness_on_hit=true antlers=6 blocks=900 sub_air=0 cam=-6823.037,101.25,4267.535:-6821.5,100.25,4271.5 stage=13x13 
  OK   структура v35_wendigo построена: blocks=900, sub_air=0
  /riftstruct v36_morok_biome -> riftstruct kind=v36_morok_biome at=-6822,102,4271 biome=riftworld:morok_hollow morok_weight=1.00 natural=true fog=true trees=5 lantern=true blocks=900 sub_air=0 cam=-6811,109,4284:-6822,103,4271 cam_vantage=true 
  OK   структура v36_morok_biome построена: blocks=900, sub_air=0
  /riftstruct v39_morok -> riftstruct kind=v39_morok at=-6822,102,4271 natural=true biome=riftworld:morok_hollow night=true alive=1 spawned=true model_boxes=54 bones=8 atlas=256 eye_boxes=2 eye_atlas=16 glowing_eyes=true spawn_morok=1 spawn_wendigo=0 day_chance=0.05 night_chance=0.45 stalk=true dash=true b
  OK   структура v39_morok построена: blocks=900, sub_air=0
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
  рыба ray: Elisar Sea Fish has the following entity data: [696.4806291240423d, 39.91505675198468d, 693.92102680926d]
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
  /rifttree raw: rifttree at=-76,104,6 kind=ambra h=7 logs=44 branches=28 leaves=229 dist=69 loaded=true cam=-65,117,6 camtop=-65,127,6
  OK   дерево найдено: -76,104,6 (ветвей: 28)
  OK   у дерева есть ветки: 28 блоков вне ствола
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
  /riftdtat у посадки дерева -> riftdtat at=9,104,9 dt=120 dy=0 lv=86 air=853 water=0 top=116:riftworlddt:sizo_leaves col=rooty_elisar_soil,sizo_branchx6,airx4,sizo_leavesx4,air blocks=rooty_elisar_soil=1 sizo_branch=33 sizo_leaves=86 near=ivory_marble_bricks=71 temple_bricks=57 elisar_crust=38 elisar_stone=35 ivory_marble_tiles=1
  /dynamictrees gettree -> Species: Sizo Tree JoCode: JJJzJJxx98+RJOOPvn1JJ8JP 
  контрольный дуб: Successfully set tree at position -9, 104, 19 to species Oak with JoCode JJJ0JR7Jxx+nXv. 
  /riftdtat у контрольного дуба -> riftdtat at=-9,104,19 dt=1 dy=155 lv=136 air=817 water=0 top=116:dynamictrees:oak_leaves col=rooty_elisar_soil,oak_branchx6,air,oak_leaves,oak_branch,oak_leavesx3,airx3 blocks=rooty_elisar_soil=1 near=elisar_crust=81 eli
  саженец + костная мука -> riftgrow at=25,105,1 block=riftworlddt:sizo_sapling grew=8 after=riftworlddt:sizo_branch 
  /riftdtat у саженца -> riftdtat at=25,104,1 dt=71 dy=0 lv=62 air=902 water=0 top=112:riftworlddt:sizo_leaves col=rooty_elisar_soil,sizo_branchx7,airx8 blocks=rooty_elisar_soil=1 sizo_branch=8 sizo_leaves=62 near=elisar_crust=81 elisar_grass=77
  OK   саженец аддона вырос в дерево DT: блоков 71 (листвы 62)
  OK   DT вырастил дерево аддона: блоков 120 (ветвей 33, листвы 86)
  [БИОМ] секция садов и рощи
  режим игрока -> Set TestPlayer's game mode to Spectator Mode 
  /riftbiome gardens (поиск) -> riftbiome name=gardens at=406,-650 bio=riftworld:waterfall_gardens r=82 top=-2147483648 water=0 garden_grass=0 garden_leaves=0 garden_logs=0 ivy=0 planks=0 lantern=0 struct=- fall=399,-675 wall=411,86
  /riftbiome gardens (счёт) -> riftbiome name=gardens at=406,-650 bio=riftworld:waterfall_gardens r=82 top=104 water=5895 garden_grass=7874 garden_leaves=0 garden_logs=0 ivy=2872 planks=0 lantern=0 struct=- fall=399,-675 wall=411,86,-628:411,87,-639 river=287,92,-677:243,68,-650 isles=5 isl_r=37..82 sea=62 falls_water=448 falls_src=63 garden_oak=10078 sizo_blocks=0 below_sea_cols=949 submerged_air=0 blocks=garden_limestone=111600 elisar_stone=48184 garden_limestone_mossy=20263 elisar_stone_mossy=12002 elisar_soil=10923 garden_oak_leaves=8728 riftbiome name=gardens at=406,-650 bio=rift
  OK   сады: биом острова riftworld:waterfall_gardens (биом-сурс совпадает с рельефом)
  /locate biome waterfall_gardens -> The nearest riftworld:waterfall_gardens is at [407, 90, -651] (40 blocks away) 
  /riftwhere у найденного биома -> riftwhere at=407,-651 biome=riftworld:waterfall_gardens top=104:riftworlddt:garden_oak_leaves ground=104:riftworlddt:garden_oak_leaves sea=62 garden=true 
  OK   /locate biome waterfall_gardens привёл на землю садов (407,90,-651, верх: riftworlddt:garden_oak_leaves, земля 104:riftworlddt:garden_oak_leaves)
  /riftaround у точки /locate -> riftaround at=407,106,-651 r=24 cols=2401 land=2108 garden_limestone=26896 garden_grass=1469 lavender=0 elisar_stone=13048 holes=0 gaps=0 
  OK   /locate привёл на настоящий садовый остров: известняка полок 26896 блоков в r=24
  OK   под островом садов нет дыр до бедрока (holes=0, трава садов 1469)
  OK   сады: зелёной травы 8109 блоков
  OK   сады: воды 6327 блоков (пруды и водопады, без затопления)
  OK   сады: плюща на стенах 2933 блоков
  OK   сады: рукотворного нет (досок 0, фонарей 0)
  OK   сады: островов 5 (увеличенный архипелаг 5–8 больших островов)
  OK   сады: разброс радиусов увеличенных островов 37..82
  OK   сады: садового дуба аддона DT 10128 блоков
  OK   сады: сизоствола нет (sizo_blocks=0)
  OK   сады: водопады текут — 448 блоков струй, у 63 есть исток на кромке
  OK   сады: подводные каменные ярусы есть (985 колонок) и вода над ними не удалена (submerged_air=0)
  OK   сады: разбросанной листвы на земле нет (garden_leaves=0)
  кадр обрыва: камера 411 86 -628 -> взгляд 411 87 -639
  игрок после кадра обрыва -> TestPlayer has the following entity data: [411.5d, 86.0d, -627.5d] 
  /locate biome waterfall_gardens -> The nearest riftworld:waterfall_gardens is at [407, 90, -651] (23 blocks away) 
  /riftwhere у waterfall_gardens -> riftwhere at=407,-651 biome=riftworld:waterfall_gardens top=104:riftworlddt:garden_oak_leaves ground=104:riftworlddt:garden_oak_leaves sea=62 garden=true 
  OK   /locate biome waterfall_gardens привёл на землю биома (104:riftworlddt:garden_oak_leaves)
  /locate biome ashen_lands -> The nearest riftworld:ashen_lands is at [2407, 66, 1347] (2808 blocks away) 
  /riftbiome ashen -> riftbiome name=ashen at=2407,1347 bio=riftworld:ashen_lands r=380 combo=4 top=104 castle=2369,1313 
  OK   /locate biome ashen_lands нашёл случайный биом адского острова с замком (2407, 1347)
  /riftbiome sizo (поиск) -> riftbiome name=sizo at=-799,2114 bio=riftworld:sizo_grove r=176 top=-2147483648 water=0 garden_grass=0 garden_leaves=0 garden_logs=0 ivy=0 planks=0 lantern=0 struct=carts=1 shrines=1 temples=1 pillar_
  /riftbiome sizo (счёт) -> riftbiome name=sizo at=-799,2114 bio=riftworld:sizo_grove r=176 top=122 water=35262 garden_grass=0 garden_leaves=0 garden_logs=0 ivy=0 planks=37 lantern=0 struct=carts=1 shrines=1 temples=1 pillar_pairs=2 fall=- wall=- river=- sizo_trail=1624 sizo_flowers=884 sizo_pillars=9 sview1=-703,104,2098:-840,105,2109 sview2=-733,103,2106:-865,104,2114 sview3=-778,104,2125:-870,107,2116 falls_water=0 falls_src=0 garden_oak=0 sizo_blocks=0 below_sea_cols=0 submerged_air=0 blocks=elisar_stone=317642 elisar_stone_mossy=35982 sizo_leaves=21280 elisar_crust=11339 elisa
  /riftbiome sizo: структуры -> carts=1 shrines=1 temples=1 pillar_pairs=2 fall= trail=1624 flowers=884 pillars=9
  OK   роща сизо: мощёная тропа (1624), парные столбы с янтарём (9) и васильковые цветы (884)
  /riftdtwild (роща сизо) -> riftdtwild at=-799,2114 r=44 dt=16445 rooty=33 branch=1737 leaves=14675 blocks=sizo_leaves=14675 sizo_branch=1737 rooty_elisar_grass=31 rooty_elisar_moss=2 
  OK   роща сизо: гигантские динамические деревья сизо выросли (16445 блоков DT, кроны 14675)
  /riftbiome violet (поиск) -> riftbiome name=violet at=-989,-1202 bio=riftworld:violet_hills r=131 top=-2147483648 water=0 garden_grass=0 garden_leaves=0 garden_logs=0 ivy=0 planks=0 lantern=0 struct=- fall=- wall=- river=- vislan
  /riftbiome violet (счёт) -> riftbiome name=violet at=-989,-1202 bio=riftworld:violet_hills r=131 top=110 water=38307 garden_grass=0 garden_leaves=0 garden_logs=0 ivy=0 planks=23 lantern=9 struct=- fall=- wall=- river=- vislands=6 visl_r=82..132 violet_blooms=2085 violet_leaves=0 lavender=3904 accent_flowers=644 walls=98 fences=115 lamps=35 vground=82 road=-1077,88,-1223:-947,85,-1179 varch=-953,116,-1213:-796,86,-1109 vfields=-1031,99,-1216:-951,84,-1182 vpath=-994,84,-1197:-819,85,-1124 vtrees=-987,86,-1203:-977,90,-1209 falls_water=0 falls_src=0 garden_oak=0 sizo_blocks=0 below_s
  OK   холмы: увеличенный архипелаг из 6 огромных парящих островов
  OK   холмы: сизоствола нет (sizo_blocks=0)
  OK   холмы: воздушных цветов всего 5989 блоков (лаванда 3904 + соцветия 2085)
  OK   холмы: гряд лаванды 3904 блоков (воздушные ряды)
  OK   холмы: вдоль дороги каменные стенки (98), деревянные заборы (115) и столбы с факелами (35)
  OK   холмы: других цветов в малом количестве (644 на фоне 5989 фиолетовых)
  /riftdtwild (холмы, лиловый ветвецвет) -> riftdtwild at=-989,-1202 r=40 dt=1314 rooty=0 branch=158 leaves=1156 blocks=lilac_leaves=1156 lilac_branch=158 
  OK   холмы: динамических деревьев аддона 1314 блоков (лиловый ветвецвет)
  OK   роща: биом острова riftworld:sizo_grove
  /locate biome sizo_grove -> The nearest riftworld:sizo_grove is at [-386, 105, 722] (2016 blocks away) 
  /riftwhere у sizo_grove -> riftwhere at=-386,722 biome=riftworld:sizo_grove top=104:riftworld:elisar_tall_grass ground=104:riftworld:elisar_tall_grass sea=62 garden=false 
  OK   /locate biome sizo_grove привёл на землю биома (104:riftworld:elisar_tall_grass)
  OK   Фиолетовые холмы: биом луга riftworld:violet_hills
  /locate biome violet_hills -> The nearest riftworld:violet_hills is at [-989, 86, -1202] (0 blocks away) 
  /riftwhere у violet_hills -> riftwhere at=-989,-1202 biome=riftworld:violet_hills top=83:minecraft:grass ground=83:minecraft:grass sea=62 garden=false 
  OK   /locate biome violet_hills привёл на землю биома (83:minecraft:grass)
  /riftdtwhy (холмы) -> riftdtwhy at=-989,86,-1202 biome_surface=riftworld:violet_hills biome_top=riftworld:violet_hills biome_y0=riftworld:violet_hills dt_features=2 has_dt=true dt_species=handled:Species{registryName=riftworlddt:lilac} db_entries=8 db_populated=true plains_? sel=StaticSpeciesSelector 
  OK   DT/холмы: грунт найден
  OK   DT/холмы: почва приемлема для DT
  OK   DT/холмы: база DT ответила по виду
  /riftdtwild (холмы) -> riftdtwild at=-989,-1202 r=32 dt=549 rooty=0 branch=64 leaves=485 blocks=lilac_leaves=485 lilac_branch=64 
  /riftdtdbg (холмы) -> riftdtdbg at=-989,86,-1202 discs=3 disc0_r=8 real=true ground=82:Block{minecraft:grass_block} handled=true sp=Species{registryName=riftworlddt:lilac} codes=7[2:12 3:12 4:12 5:12 6:12 7:12 8:12 ] code_for_r8=ok gen=true 
  OK   DT растёт в мире сам: 549 блоков аддона вокруг точки
  OK   Мороковая Падь: туман включён (вес 1.00)
  /riftstruct v37_morok_natural -> riftstruct kind=v37_morok_natural at=-6822,104,4271 natural=true biome=riftworld:morok_hollow regions=3 land_regions=3 search=[pts=9,land=true,land_ring=9,above=1,rime=1,morok=1,chunks=0,region=-6695,4186,R=196] trees=2 logs=32 leaves=219 brambles=0 fangs=17 carpets=134 rime=948 
  FAIL структура v37_morok_natural: blocks=0 (ожидалось >=260), sub_air=0
  /riftstruct v37_morok_natural -> riftstruct kind=v37_morok_natural at=-6822,104,4271 natural=true biome=riftworld:morok_hollow regions=3 land_regions=3 search=[pts=9,land=true,land_ring=9,above=1,rime=1,morok=1,chunks=0,region=-6695,4186,R=196] trees=2 logs=32 leaves=219 brambles=0 fangs=17 carpets=134 rime=948 soil=1323 spawn_moro
  FAIL лес Пади: деревьев 2 (нужно >=6), листвы 219 (нужно >=120)
  FAIL подлесок Пади: колючек 0 (нужно >=3), клыков 17 (нужно >=2)
  OK   биом в точке пробы = riftworld:morok_hollow
  OK   спавнеры биома живы: Морок в списке монстров (wendigo=0 — он больше не спавнится сам)
  /riftstruct v32_liopleurodon -> riftstruct kind=v32_liopleurodon at=696,33,696 alive=2 hp=90 drop_tooth=true drop_scale=true model_boxes=66 bones=10 atlas=512 blocks=900 sub_air=0 cam=689.0,39.4,709.5:693.5,35.4,696.5 
  OK   структура v32_liopleurodon построена: blocks=900, sub_air=0
  OK   модель Лиоплевродона в сборке: кубоидов 66, костей 10 (читается из geo/liopleurodon.json)
  /riftstruct v38_liopleurodon_jaws -> riftstruct kind=v38_liopleurodon_jaws at=700,34,700 alive=1 model_boxes=66 bones=10 atlas=512 blocks=900 sub_air=0 cam=694.1,36.3,703.9:697.1,35.1,700.5 
  OK   структура v38_liopleurodon_jaws построена: blocks=900, sub_air=0
  /riftstruct v39_morok -> riftstruct kind=v39_morok at=-6822,103,4271 natural=true biome=riftworld:morok_hollow night=true alive=1 spawned=true model_boxes=54 bones=8 atlas=256 eye_boxes=2 eye_atlas=16 glowing_eyes=true spawn_morok=1 spawn_wendigo=0 day_chance=0.05 night_chance=0.45 stalk=true dash=true b
  OK   структура v39_morok построена: blocks=900, sub_air=0
  /riftstruct v39_morok -> riftstruct kind=v39_morok at=-6822,103,4271 natural=true biome=riftworld:morok_hollow night=true alive=1 spawned=true model_boxes=54 bones=8 atlas=256 eye_boxes=2 eye_atlas=16 glowing_eyes=true spawn_morok=1 spawn_wendigo=0 day_chance=0.05 night_chance=0.45 stalk=true dash=true blink=true hp=42 dmg=7 speed=0.37 blocks=
  OK   Морок жив в кадре: кубоидов 54, геометрия глаз 2 (RenderType.eyes = свечение)
  OK   Морок: горящие глаза включены и он реально в спавнерах биома (день 5 %, ночь 45 %)
  OK   Морок стоит в настоящей Мороковой Пади
  /riftstruct crashed_ship -> riftstruct kind=crashed_ship at=937,66,155 variant=0 blocks=1393 rock=617 sub_air=0 cam=920,74,138:934,71,161 
  OK   структура crashed_ship построена: blocks=1393, sub_air=0
  /riftstruct sunken_ship -> riftstruct kind=sunken_ship at=-673,14,325 variant=1 blocks=1097 sub_air=0 cam=-677,20,311:-675,15,326 
  OK   структура sunken_ship построена: blocks=1097, sub_air=0
  /riftstruct ocean_building -> riftstruct kind=ocean_building at=-783,12,314 variant=3 blocks=323 sub_air=0 cam=-771,18,326:-783,13,314 
  OK   структура ocean_building построена: blocks=323, sub_air=0
  /riftstruct white_village -> riftstruct kind=white_village at=-810,97,-365 variant=0 houses=5 blocks=637 sub_air=0 cam=-802,108,-341:-810,100,-369 
  OK   структура white_village построена: blocks=637, sub_air=0
  OK   деревня white_village содержит ровно 5 небольших домов (лимит <= 6 соблюдён)
  /riftstruct burned_village -> riftstruct kind=burned_village at=490,64,5249 bio=riftworld:ashen_lands variant=2 houses=4 blocks=672 fires=9 sub_air=0 cam=497,74,5272:490,67,5246 
  OK   структура burned_village построена: blocks=672, sub_air=0
  OK   деревня burned_village содержит ровно 4 небольших домов (лимит <= 6 соблюдён)
  /riftstruct stilt_village -> riftstruct kind=stilt_village at=382,65,-567 variant=0 houses=6 blocks=3732 sub_air=0 cam=368,78,-542:383,68,-573 
  OK   структура stilt_village построена: blocks=3732, sub_air=0
  OK   деревня stilt_village содержит ровно 6 небольших домов (лимит <= 6 соблюдён)
  /riftstruct city_castle_front -> riftstruct kind=city_castle_front at=0,112,36 main_plots=48 north_plots=11 blocks=2114 sub_air=0 cam=14,122,58:0,121,6 
  OK   структура city_castle_front построена: blocks=2114, sub_air=0
  /riftstruct city_castle_aerial -> riftstruct kind=city_castle_aerial at=0,112,0 main_plots=48 north_plots=11 blocks=2114 sub_air=0 cam=48,154,54:0,118,-6 
  OK   структура city_castle_aerial построена: blocks=2114, sub_air=0
  /riftstruct city_castle_north -> riftstruct kind=city_castle_north at=0,112,-54 main_plots=48 north_plots=11 blocks=2114 sub_air=0 cam=-18,132,-78:2,112,-30 
  OK   структура city_castle_north построена: blocks=2114, sub_air=0
  /riftstruct colonnade_boss -> riftstruct kind=colonnade_boss at=1220,89,-536 bosses=1 minions=2 attack_ok=0 blocks=120 sub_air=0 cam=1216,93,-547:1220,95,-536 
  OK   структура colonnade_boss построена: blocks=120, sub_air=0
  /riftstruct colonnade_boss_minion -> riftstruct kind=colonnade_boss_minion at=1220,89,-536 bosses=2 minions=2 attack_ok=0 blocks=220 sub_air=0 cam=1221,91,-545:1222,92,-542 
  OK   структура colonnade_boss_minion построена: blocks=220, sub_air=0
  OK   3D-оружие Колоннады (crystal_katar, crystal_barrage, crystal_aegis, astral_prism) выдано и отснято
  /riftstruct ocean_island_waves -> riftstruct kind=ocean_island_waves at=-405,63,-118 wake_islands=175 blocks=1075 sub_air=0 cam=-323,92,-94:-405,65,-118 
  OK   структура ocean_island_waves построена: blocks=1075, sub_air=0
  /riftstruct waterfall_ocean_waves -> riftstruct kind=waterfall_ocean_waves at=-360,63,1181 blocks=200 sub_air=0 cam=-353,76,1204:-358,64,1183 
  OK   структура waterfall_ocean_waves построена: blocks=200, sub_air=0
  /riftstruct harbor_port -> riftstruct kind=harbor_port at=-362,64,-544 island_top=68 blocks=42489 sub_air=0 cam=-336,81,-516:-364,68,-554 
  OK   структура harbor_port построена: blocks=42489, sub_air=0
  /riftstruct grand_theater -> riftstruct kind=grand_theater at=-552,99,381 blocks=7763 sub_air=0 cam=-542,115,403:-552,103,375 
  OK   структура grand_theater построена: blocks=7763, sub_air=0
  /riftstruct sea_lighthouse -> riftstruct kind=sea_lighthouse at=172,69,621 blocks=37936 sub_air=0 cam=196,78,647:172,85,621 
  OK   структура sea_lighthouse построена: blocks=37936, sub_air=0
  /riftstruct ancient_tree_altar -> riftstruct kind=ancient_tree_altar at=821,102,-2292 blocks=2725 sub_air=0 cam=836,109,-2277:821,108,-2292 
  OK   структура ancient_tree_altar построена: blocks=2725, sub_air=0
  /riftstruct ashen_belfry_barrow -> riftstruct kind=ashen_belfry_barrow at=-515,64,3139 blocks=1502 sub_air=0 cam=-519,75,3165:-513,74,3139 
  OK   структура ashen_belfry_barrow построена: blocks=1502, sub_air=0
  /riftstruct shattered_shard_forge -> riftstruct kind=shattered_shard_forge at=1384,86,-461 blocks=4870 sub_air=0 cam=1388,94,-445:1384,91,-461 
  OK   структура shattered_shard_forge построена: blocks=4870, sub_air=0
  FAIL /riftstruct violet_wind_observatory не нашёл структуру ( )
  FAIL /riftstruct elisar_eye_occlusion не нашёл структуру ( )
  FAIL /riftstruct colonnade_boss_pillars не нашёл структуру ( )
  FAIL /riftstruct colonnade_boss_hail не нашёл структуру ( )
  FAIL /riftstruct origami_dragon_closeup не нашёл структуру ( )
  FAIL /riftstruct origami_dragon_under_release не нашёл структуру ( )
  FAIL /riftstruct origami_dragon_over_release не нашёл структуру ( )
  FAIL /riftstruct blacktide_bridge_shore не нашёл структуру ( )
  FAIL /riftstruct blacktide_bay_aerial не нашёл структуру ( )
  FAIL /riftstruct blacktide_cathedral_hall не нашёл структуру ( )
  FAIL /riftstruct blacktide_second_floor не нашёл структуру ( )
  FAIL /riftstruct blacktide_rotunda_sanctum не нашёл структуру ( )
  FAIL /riftstruct blacktide_tower_walkability не нашёл структуру ( )
  FAIL /riftstruct blacktide_citadel не нашёл структуру ( )
  FAIL Цитадель: в защитных стенах остались дыры (wall_breaches=?)
  FAIL Цитадель: лестницы башен заблокированы (ступеней=0, заблокировано=?)
  FAIL Цитадель: у круглой Ротонды нет входа (rotunda_door=0)
  FAIL /riftstruct ashen_burned_church не нашёл структуру ( )
  FAIL /riftstruct ashen_combo_matrix не нашёл структуру ( )
  FAIL /riftstruct enlarged_biomes_audit не нашёл структуру ( )
  FAIL /riftstruct connected_carpets_showcase не нашёл структуру ( )
  FAIL /riftstruct city_sanctuary_garden не нашёл структуру ( )
  FAIL /riftstruct city_sanctuary_plaza_aerial не нашёл структуру ( )
  FAIL /riftstruct city_ornate_bridge не нашёл структуру ( )
  FAIL /riftstruct wild_inverted_tower_dragon не нашёл структуру ( )
  FAIL /riftstruct dragon_horn_beyond_sim не нашёл структуру ( )
  FAIL /riftstruct astral_prism_epic не нашёл структуру ( )
  FAIL /riftstruct v27_new_blocks_gallery не нашёл структуру ( )
  FAIL /riftstruct peaceful_sky_dart_fish не нашёл структуру ( )
  FAIL /riftstruct peaceful_blossom_stag не нашёл структуру ( )
  FAIL /riftstruct v28_wood_and_stone_gallery не нашёл структуру ( )
  FAIL /riftstruct v28_elisar_perf_2chunks не нашёл структуру ( )
  FAIL /riftstruct v29_candle_portal_open не нашёл структуру ( )
  FAIL /riftstruct v29_blacktide_seabed_bridge не нашёл структуру ( )
  FAIL /riftstruct v29_crafting_and_coin_ore не нашёл структуру ( )
  FAIL /riftstruct v29_sync_ocean_waves_perf не нашёл структуру ( )
  FAIL /riftstruct v31_morok_gallery не нашёл структуру ( )
  FAIL /riftstruct sea_cave_exit не нашёл структуру ( )
  /riftstruct locate_all_audit ->  
  FAIL locate_all_audit:  
  /locate structure riftworld:blacktide_citadel ->  
  FAIL ванильная команда /locate structure riftworld:blacktide_citadel не нашла цель:  
  /locate structure riftworld:city_sanctuary ->  
  FAIL ванильная команда /locate structure riftworld:city_sanctuary не нашла цель:  
  /locate structure riftworld:inverted_tower ->  
  FAIL ванильная команда /locate structure riftworld:inverted_tower не нашла цель:  
  /locate structure riftworld:grand_theater ->  
  FAIL ванильная команда /locate structure riftworld:grand_theater не нашла цель:  
  /locate structure riftworld:sea_cave_exit ->  
  FAIL ванильная команда /locate structure riftworld:sea_cave_exit не нашла цель:  
  /locate biome riftworld:shallow_reef ->  
  FAIL ванильная команда /locate biome riftworld:shallow_reef не нашла цель:  
  /locate biome riftworld:abyss ->  
  FAIL ванильная команда /locate biome riftworld:abyss не нашла цель:  
  /locate biome riftworld:violet_hills ->  
  FAIL ванильная команда /locate biome riftworld:violet_hills не нашла цель:  
  /locate biome riftworld:crystal_rifts ->  
  FAIL ванильная команда /locate biome riftworld:crystal_rifts не нашла цель:  
  [Overworld] /locate structure riftworld:overgrown_temple -> The nearest riftworld:overgrown_temple is at [96, ~, 128] (160 blocks away) 
  OK   ванильная команда /locate structure riftworld:overgrown_temple работает в Обычном мире
  /riftstruct spawn_eggs_audit ->  
  FAIL spawn_eggs_audit:  
  /riftportal observatory_find -> riftportal observatory_find natural=true at=96,-48,128 
  /riftportal observatory_verify -> riftportal observatory_verify seal=104,-59,136 candles=8 chests=2 lecterns=1 rugs=25 cam=106,-55,148:104,-56,134 
  OK   Обсерватория Обычного мира построена со всем ритуальным набором: candles=8, chests=2
  /riftportal observatory_awaken -> riftportal observatory_awaken rift=true lit_candles=8 lit_runes=72 cam=103,-55,146:104,-52,136 
  OK   Обсерватория пробудила все руны, свечи и Белый 3D-Пролом в Элисар!
  /riftportal setup -> riftportal setup center=24,-60,0 snapshot=2435 cam=28,-57,24:24,-49,0 
  /riftportal trigger ->  
  /riftportal walkin ->  
  после захода в белый пролом и катсцены: Dimension=No entity was found  | riftportal verify mutated=0 total=2435 
  FAIL кастомный портал: Dimension=No entity was found , mutated=0
  СТОП клиент потерян (перед генерацией Хрустальных разломов): JVM клиента умерла, дальше кадры и пробы невозможны
  СТОП причина ищется в reports/iteration-N/client.log (маркер '!! JVM КЛИЕНТА УМЕРЛА')
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
- ci-shots/10_fish_reef.jpg
- ci-shots/10b_fish_tropical.jpg
- ci-shots/11_fish_cave.jpg
- ci-shots/11b_fish_cave_rare.jpg
- ci-shots/123_overworld_portal_observatory.jpg
- ci-shots/124_overworld_portal_observatory_awakened.jpg
- ci-shots/125_open_rift_50m_white_aura.jpg
- ci-shots/127_paper_dragon_spawn_egg_test.jpg
- ci-shots/12_fish_closeup.jpg
- ci-shots/13_reef_floor.jpg
- ci-shots/146_v32_liopleurodon_deep_water.jpg
- ci-shots/147_v34_pegasus_winged_steed.jpg
- ci-shots/148_v35_wendigo_antlered_horror.jpg
- ci-shots/149_v36_morok_hollow_fog_forest.jpg
- ci-shots/14_tree_branches.jpg
- ci-shots/14b_tree_top.jpg
- ci-shots/150_v38_morok_natural_forest.jpg
- ci-shots/151_v38_liopleurodon_jaws_closeup.jpg
- ci-shots/152_v39_morok_night_hunter.jpg
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

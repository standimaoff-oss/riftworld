# CI, шаг 2: настоящий клиент Minecraft в Элисаре

- клиент: ForgeGradle runClient под Xvfb (llvmpipe, без GPU)
- воды в колонке (696,y,696): **58** блоков
- море: дно y=10, вода 51 блоков, глубочайшее дно y=1
- пещеры: 8497 колонок, воды 202732 / воздуха 11560 блоков (у свода 6121, висячего 916)
- провалов проб: **4**
- строк Exception/ERROR в логе клиента (без звука/наратора/authlib): **5**

## пробы
```
  /riftsea raw: riftsea floor=10 water=51 sea=62 min=1 max=15 cols=9409 caves=8497 wet=202732 air=11560 reef=1981 plants=1773 corals=64 float=916 ceilair=6121 bottom=-62 caveat=625,5,570 
  /riftsea raw: riftsea floor=10 water=51 sea=62 min=1 max=15 cols=9409 caves=8497 wet=202732 air=11560 reef=1981 plants=1773 corals=64 float=916 ceilair=6121 bottom=-62 caveat=625,5,570 
  OK   вода на глубине (696,56,696) = elisar_water
  /riftsea raw: riftsea floor=5 water=56 sea=62 min=-8 max=14 cols=9341 caves=8429 wet=163024 air=13239 reef=2239 plants=1402 corals=86 float=1033 ceilair=7461 bottom=-62 caveat=650,3,600 
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
  рыба sting: Elisar Sea Fish has the following entity data: [696.5d, 39.90928345322609d, 693.9015917181969d]
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
  /riftdtat у посадки дерева -> riftdtat at=9,104,9 dt=120 dy=0 lv=86 air=853 water=0 top=116:riftworlddt:sizo_leaves col=rooty_elisar_soil,sizo_branchx6,airx4,sizo_leavesx4,air blocks=rooty_elisar_soil=1 sizo_branch=33 sizo_leaves=86 near=ivory_marble_bricks=71 temple_bricks=57 elisar_crust=38 elisar_stone=35 ivory_marble_tiles=1
  /dynamictrees gettree -> Species: Sizo Tree JoCode: JJJzJJxx98+RJOOPvn1JJ8JP 
  контрольный дуб: Successfully set tree at position -9, 104, 19 to species Oak with JoCode JJJ0JR7Jxx+nXv. 
  /riftdtat у контрольного дуба -> riftdtat at=-9,104,19 dt=1 dy=156 lv=137 air=816 water=0 top=116:dynamictrees:oak_leaves col=rooty_elisar_soil,oak_branchx6,air,oak_leaves,oak_branch,oak_leavesx3,airx3 blocks=rooty_elisar_soil=1 near=elisar_crust=81 eli
  саженец + костная мука -> riftgrow at=25,105,1 block=riftworlddt:sizo_sapling grew=8 after=riftworlddt:sizo_branch 
  /riftdtat у саженца -> riftdtat at=25,104,1 dt=46 dy=0 lv=38 air=927 water=0 top=112:riftworlddt:sizo_leaves col=rooty_elisar_soil,sizo_branchx6,sizo_leaves,airx8 blocks=rooty_elisar_soil=1 sizo_branch=7 sizo_leaves=38 near=elisar_crust=81 eli
  OK   саженец аддона вырос в дерево DT: блоков 46 (листвы 38)
  OK   DT вырастил дерево аддона: блоков 120 (ветвей 33, листвы 86)
  [БИОМ] секция садов и рощи
  режим игрока -> Set TestPlayer's game mode to Spectator Mode 
  /riftbiome gardens (поиск) -> riftbiome name=gardens at=406,-650 bio=riftworld:waterfall_gardens r=82 top=-2147483648 water=0 garden_grass=0 garden_leaves=0 garden_logs=0 ivy=0 planks=0 lantern=0 struct=- fall=399,-675 wall=411,86
  /riftbiome gardens (счёт) -> riftbiome name=gardens at=406,-650 bio=riftworld:waterfall_gardens r=82 top=104 water=6147 garden_grass=7867 garden_leaves=0 garden_logs=0 ivy=2835 planks=0 lantern=0 struct=- fall=399,-675 wall=411,86,-628:411,87,-639 river=287,92,-677:243,68,-650 isles=5 isl_r=37..82 sea=62 falls_water=528 falls_src=63 garden_oak=10711 sizo_blocks=0 below_sea_cols=949 submerged_air=0 blocks=garden_limestone=111600 elisar_stone=48184 garden_limestone_mossy=20263 elisar_stone_mossy=12002 elisar_soil=10923 garden_oak_leaves=9200 
  OK   сады: биом острова riftworld:waterfall_gardens (биом-сурс совпадает с рельефом)
  /locate biome waterfall_gardens -> The nearest riftworld:waterfall_gardens is at [407, 90, -651] (40 blocks away) 
  /riftwhere у найденного биома -> riftwhere at=407,-651 biome=riftworld:waterfall_gardens top=104:riftworlddt:garden_oak_leaves ground=104:riftworlddt:garden_oak_leaves sea=62 garden=true 
  OK   /locate biome waterfall_gardens привёл на землю садов (407,90,-651, верх: riftworlddt:garden_oak_leaves, земля 104:riftworlddt:garden_oak_leaves)
  /riftaround у точки /locate -> riftaround at=407,106,-651 r=24 cols=2401 land=2108 garden_limestone=26896 garden_grass=1469 lavender=0 elisar_stone=13048 holes=0 gaps=0 
  OK   /locate привёл на настоящий садовый остров: известняка полок 26896 блоков в r=24
  OK   под островом садов нет дыр до бедрока (holes=0, трава садов 1469)
  OK   сады: зелёной травы 7867 блоков
  OK   сады: воды 6147 блоков (пруды и водопады, без затопления)
  OK   сады: плюща на стенах 2835 блоков
  OK   сады: рукотворного нет (досок 0, фонарей 0)
  OK   сады: островов 5 (увеличенный архипелаг 5–8 больших островов)
  OK   сады: разброс радиусов увеличенных островов 37..82
  OK   сады: садового дуба аддона DT 10711 блоков
  OK   сады: сизоствола нет (sizo_blocks=0)
  OK   сады: водопады текут — 528 блоков струй, у 63 есть исток на кромке
  OK   сады: подводные каменные ярусы есть (949 колонок) и вода над ними не удалена (submerged_air=0)
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
  /riftbiome sizo (счёт) -> riftbiome name=sizo at=-799,2114 bio=riftworld:sizo_grove r=176 top=123 water=38241 garden_grass=0 garden_leaves=0 garden_logs=0 ivy=0 planks=37 lantern=0 struct=carts=1 shrines=1 temples=1 pillar_pairs=2 fall=- wall=- river=- sizo_trail=1624 sizo_flowers=910 sizo_pillars=9 sview1=-703,104,2098:-840,105,2109 sview2=-733,103,2106:-865,104,2114 sview3=-778,104,2125:-870,107,2116 falls_water=0 falls_src=0 garden_oak=0 sizo_blocks=0 below_sea_cols=0 submerged_air=0 blocks=elisar_stone=344489 elisar_stone_mossy=39089 sizo_leaves=22130 elisar_crust=12267 elisa
  /riftbiome sizo: структуры -> carts=1 shrines=1 temples=1 pillar_pairs=2 fall= trail=1624 flowers=910 pillars=9
  OK   роща сизо: мощёная тропа (1624), парные столбы с янтарём (9) и васильковые цветы (910)
  /riftdtwild (роща сизо) -> riftdtwild at=-799,2114 r=44 dt=16777 rooty=32 branch=1692 leaves=15053 blocks=sizo_leaves=15053 sizo_branch=1692 rooty_elisar_grass=29 rooty_elisar_moss=3 
  OK   роща сизо: гигантские динамические деревья сизо выросли (16777 блоков DT, кроны 15053)
  /riftbiome violet (поиск) -> riftbiome name=violet at=-989,-1202 bio=riftworld:violet_hills r=131 top=-2147483648 water=0 garden_grass=0 garden_leaves=0 garden_logs=0 ivy=0 planks=0 lantern=0 struct=- fall=- wall=- river=- vislan
  /riftbiome violet (счёт) -> riftbiome name=violet at=-989,-1202 bio=riftworld:violet_hills r=131 top=110 water=38307 garden_grass=0 garden_leaves=0 garden_logs=0 ivy=0 planks=23 lantern=9 struct=- fall=- wall=- river=- vislands=6 visl_r=82..132 violet_blooms=2086 violet_leaves=0 lavender=3900 accent_flowers=644 walls=98 fences=115 lamps=35 vground=82 road=-1077,88,-1223:-947,85,-1179 varch=-953,116,-1213:-796,86,-1109 vfields=-1031,99,-1216:-951,84,-1182 vpath=-994,84,-1197:-819,85,-1124 vtrees=-988,85,-1200:-982,86,-1190 falls_water=0 falls_src=0 garden_oak=0 sizo_blocks=0 below_s
  OK   холмы: увеличенный архипелаг из 6 огромных парящих островов
  OK   холмы: сизоствола нет (sizo_blocks=0)
  OK   холмы: воздушных цветов всего 5986 блоков (лаванда 3900 + соцветия 2086)
  OK   холмы: гряд лаванды 3900 блоков (воздушные ряды)
  OK   холмы: вдоль дороги каменные стенки (98), деревянные заборы (115) и столбы с факелами (35)
  OK   холмы: других цветов в малом количестве (644 на фоне 5986 фиолетовых)
  /riftdtwild (холмы, лиловый ветвецвет) -> riftdtwild at=-989,-1202 r=40 dt=1762 rooty=0 branch=231 leaves=1531 blocks=lilac_leaves=1531 lilac_branch=231 
  OK   холмы: динамических деревьев аддона 1762 блоков (лиловый ветвецвет)
  OK   роща: биом острова riftworld:sizo_grove
  /locate biome sizo_grove -> The nearest riftworld:sizo_grove is at [-386, 105, 722] (2016 blocks away) 
  /riftwhere у sizo_grove -> riftwhere at=-386,722 biome=riftworld:sizo_grove top=122:riftworlddt:sizo_leaves ground=122:riftworlddt:sizo_leaves sea=62 garden=false 
  OK   /locate biome sizo_grove привёл на землю биома (122:riftworlddt:sizo_leaves)
  OK   Фиолетовые холмы: биом луга riftworld:violet_hills
  /locate biome violet_hills -> The nearest riftworld:violet_hills is at [-989, 86, -1202] (0 blocks away) 
  /riftwhere у violet_hills -> riftwhere at=-989,-1202 biome=riftworld:violet_hills top=94:riftworlddt:lilac_leaves ground=94:riftworlddt:lilac_leaves sea=62 garden=false 
  OK   /locate biome violet_hills привёл на землю биома (94:riftworlddt:lilac_leaves)
  /riftdtwhy (холмы) -> riftdtwhy at=-989,86,-1202 biome_surface=riftworld:violet_hills biome_top=riftworld:violet_hills biome_y0=riftworld:violet_hills dt_features=2 has_dt=true dt_species=handled:Species{registryName=riftworlddt:lilac} db_entries=7 db_populated=true plains_? sel=StaticSpeciesSelector 
  OK   DT/холмы: грунт найден
  OK   DT/холмы: почва приемлема для DT
  OK   DT/холмы: база DT ответила по виду
  /riftdtwild (холмы) -> riftdtwild at=-989,-1202 r=32 dt=960 rooty=0 branch=122 leaves=838 blocks=lilac_leaves=838 lilac_branch=122 
  /riftdtdbg (холмы) -> riftdtdbg at=-989,86,-1202 discs=2 disc0_r=7 real=true ground=82:Block{minecraft:grass_block} handled=true sp=Species{registryName=riftworlddt:lilac} codes=7[2:12 3:12 4:12 5:12 6:12 7:12 8:12 ] code_for_r7=ok gen=false 
  OK   DT растёт в мире сам: 960 блоков аддона вокруг точки
  /locate biome crystal_rifts -> The nearest riftworld:crystal_rifts is at [-2441, 89, -2825] (2177 blocks away) 
  /riftwhere у crystal_rifts -> riftwhere at=-2441,-2825 biome=riftworld:crystal_rifts top=87:riftworld:crystal_block ground=87:riftworld:crystal_block sea=62 garden=false 
  OK   /locate biome crystal_rifts привёл на землю биома (87:riftworld:crystal_block)
  /riftbiome crystal -> riftbiome name=crystal at=1214,-551 isl1=1179,-365 bio=riftworld:crystal_rifts cislands=7 cbridges=8 bridge_walkable=1 crystal_blocks=2836 marble_blocks=431441 rift_turf=9391 rift_grass=2040 vanilla_grass=0 isl1_trees=0 crystal_flora=2710 water=432 ivy=48 sizo_blocks=0 cview1=1193,93,-599:1225,93,-525 cview2=1199,94,-488:1199,91,-469 cview3=1189,95,-395:1181
  OK   Хрустальные разломы: биом святилища riftworld:crystal_rifts
  OK   Хрустальные разломы: архипелаг из 7 островов и 8 арочных мостов
  OK   Хрустальные разломы: каменный арочный мост непрерывен и полностью проходим пешком
  OK   Хрустальные разломы: многоблочные кристаллы на всех островах (2836), мрамор/сланец (431441), чаши/водопады (432)
  OK   Хрустальные разломы: собственный шалфейный дёрн (9391) и трава разлома (2040), ванильной травы=0, деревьев на кристальном острове=0
  OK   Хрустальные разломы: чужих деревьев сизо нет (sizo_blocks=0)
  /riftdtwild (хрустальные разломы, мини-бонсай серебрица) -> riftdtwild at=1214,-551 r=44 dt=36 rooty=3 branch=20 leaves=13 blocks=crystal_birch_branch=20 crystal_birch_leaves=13 rooty_rift_turf=3 
  OK   Хрустальные разломы: миниатюрные редкие деревца DT вдоль аллеи (36 блоков, без лесов и без ambra)
  /riftstruct v34_pegasus -> riftstruct kind=v34_pegasus at=1996,117,-2276 alive=1 tamed=0 where=morok_hollow spawned=true saddleable=true saddle_seat=1.15 blocks=900 sub_air=0 cam=2002,116,-2276:1996,116,-2276 
  OK   структура v34_pegasus построена: blocks=900, sub_air=0
  /riftstruct v35_wendigo -> riftstruct kind=v35_wendigo at=1996,119,-2276 alive=2 hp=64 darkness_on_hit=true antlers=6 blocks=900 sub_air=0 cam=2003,119,-2276:1996,119,-2276 
  OK   структура v35_wendigo построена: blocks=900, sub_air=0
  /riftstruct v36_morok_biome -> riftstruct kind=v36_morok_biome at=1996,120,-2276 biome=riftworld:morok_hollow morok_weight=0.61 natural=true fog=true trees=5 lantern=true blocks=900 sub_air=0 cam=1987,122,-2265:1996,121,-2276 
  OK   структура v36_morok_biome построена: blocks=900, sub_air=0
  OK   Мороковая Падь: туман включён (вес 0.61)
  /riftstruct v37_morok_natural -> riftstruct kind=v37_morok_natural at=1996,122,-2276 natural=true biome=riftworld:morok_hollow regions=49 land_regions=49 search=[pts=9,land=true,above=2,rime=1,morok=1,chunks=0,region=1862,-2276,R=169] trees=2 logs=32 leaves=219 brambles=0 fangs=17 carpets=62 rime=460 soil=1323 s
  FAIL структура v37_morok_natural: blocks=0 (ожидалось >=260), sub_air=0
  /riftstruct v37_morok_natural -> riftstruct kind=v37_morok_natural at=1996,122,-2276 natural=true biome=riftworld:morok_hollow regions=49 land_regions=49 search=[pts=9,land=true,above=2,rime=1,morok=1,chunks=0,region=1862,-2276,R=169] trees=2 logs=32 leaves=219 brambles=0 fangs=17 carpets=62 rime=460 soil=1323 spawn_wendigo=1 spawn
  FAIL лес Пади: деревьев 2 (нужно >=6), листвы 219 (нужно >=120)
  FAIL подлесок Пади: колючек 0 (нужно >=3), клыков 17 (нужно >=2)
  OK   биом в точке пробы = riftworld:morok_hollow
  OK   спавнеры биома живы: riftworld:wendigo weight=1 (было spawners: {})
  /riftstruct v32_liopleurodon -> riftstruct kind=v32_liopleurodon at=696,33,696 alive=2 hp=90 drop_tooth=true drop_scale=true model_boxes=66 bones=10 atlas=512 blocks=900 sub_air=0 cam=689.0,39.4,709.5:693.5,35.4,696.5 
  OK   структура v32_liopleurodon построена: blocks=900, sub_air=0
  OK   модель Лиоплевродона в сборке: кубоидов 66, костей 10 (читается из geo/liopleurodon.json)
  /riftstruct v38_liopleurodon_jaws -> riftstruct kind=v38_liopleurodon_jaws at=700,34,700 alive=1 model_boxes=66 bones=10 atlas=512 blocks=900 sub_air=0 cam=694.1,36.3,703.9:697.1,35.1,700.5 
  OK   структура v38_liopleurodon_jaws построена: blocks=900, sub_air=0
  /riftstruct v39_morok -> riftstruct kind=v39_morok at=1996,121,-2276 natural=true biome=riftworld:morok_hollow night=true alive=1 spawned=true model_boxes=50 bones=8 atlas=256 eye_boxes=2 eye_atlas=16 glowing_eyes=true spawn_morok=1 spawn_wendigo=1 day_chance=0.05 night_chance=0.45 stalk=true dash=true b
  OK   структура v39_morok построена: blocks=900, sub_air=0
  /riftstruct v39_morok -> riftstruct kind=v39_morok at=1996,121,-2276 natural=true biome=riftworld:morok_hollow night=true alive=1 spawned=true model_boxes=50 bones=8 atlas=256 eye_boxes=2 eye_atlas=16 glowing_eyes=true spawn_morok=1 spawn_wendigo=1 day_chance=0.05 night_chance=0.45 stalk=true dash=true blink=true hp=42 dmg=7 speed=0.37 blocks=
  OK   Морок жив в кадре: кубоидов 50, геометрия глаз 2 (RenderType.eyes = свечение)
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
  /riftstruct city_castle_front -> riftstruct kind=city_castle_front at=0,112,36 main_plots=48 north_plots=11 blocks=2050 sub_air=0 cam=14,122,58:0,121,6 
  OK   структура city_castle_front построена: blocks=2050, sub_air=0
  /riftstruct city_castle_aerial -> riftstruct kind=city_castle_aerial at=0,112,0 main_plots=48 north_plots=11 blocks=2050 sub_air=0 cam=48,154,54:0,118,-6 
  OK   структура city_castle_aerial построена: blocks=2050, sub_air=0
  /riftstruct city_castle_north -> riftstruct kind=city_castle_north at=0,112,-54 main_plots=48 north_plots=11 blocks=2050 sub_air=0 cam=-18,132,-78:2,112,-30 
  OK   структура city_castle_north построена: blocks=2050, sub_air=0
  /riftstruct colonnade_boss -> riftstruct kind=colonnade_boss at=1220,89,-536 bosses=1 minions=2 attack_ok=0 blocks=120 sub_air=0 cam=1216,93,-547:1220,95,-536 
  OK   структура colonnade_boss построена: blocks=120, sub_air=0
  /riftstruct colonnade_boss_minion -> riftstruct kind=colonnade_boss_minion at=1220,89,-536 bosses=1 minions=2 attack_ok=0 blocks=120 sub_air=0 cam=1221,91,-545:1222,92,-542 
  OK   структура colonnade_boss_minion построена: blocks=120, sub_air=0
  OK   3D-оружие Колоннады (crystal_katar, crystal_barrage, crystal_aegis, astral_prism) выдано и отснято
  /riftstruct ocean_island_waves -> riftstruct kind=ocean_island_waves at=-405,63,-118 wake_islands=175 blocks=1075 sub_air=0 cam=-323,92,-94:-405,65,-118 
  OK   структура ocean_island_waves построена: blocks=1075, sub_air=0
  /riftstruct waterfall_ocean_waves -> riftstruct kind=waterfall_ocean_waves at=-360,63,1181 blocks=200 sub_air=0 cam=-353,76,1204:-358,64,1183 
  OK   структура waterfall_ocean_waves построена: blocks=200, sub_air=0
  /riftstruct harbor_port -> riftstruct kind=harbor_port at=-362,64,-544 island_top=68 blocks=42489 sub_air=0 cam=-336,81,-516:-364,68,-554 
  OK   структура harbor_port построена: blocks=42489, sub_air=0
  /riftstruct grand_theater -> riftstruct kind=grand_theater at=-552,99,381 blocks=7765 sub_air=0 cam=-542,115,403:-552,103,375 
  OK   структура grand_theater построена: blocks=7765, sub_air=0
  /riftstruct sea_lighthouse -> riftstruct kind=sea_lighthouse at=172,69,621 blocks=37936 sub_air=0 cam=196,78,647:172,85,621 
  OK   структура sea_lighthouse построена: blocks=37936, sub_air=0
  /riftstruct ancient_tree_altar -> riftstruct kind=ancient_tree_altar at=821,102,-2292 blocks=2913 sub_air=0 cam=836,109,-2277:821,108,-2292 
  OK   структура ancient_tree_altar построена: blocks=2913, sub_air=0
  /riftstruct ashen_belfry_barrow -> riftstruct kind=ashen_belfry_barrow at=-515,64,3139 blocks=1502 sub_air=0 cam=-519,75,3165:-513,74,3139 
  OK   структура ashen_belfry_barrow построена: blocks=1502, sub_air=0
  /riftstruct shattered_shard_forge -> riftstruct kind=shattered_shard_forge at=1384,86,-461 blocks=4870 sub_air=0 cam=1388,94,-445:1384,91,-461 
  OK   структура shattered_shard_forge построена: blocks=4870, sub_air=0
  /riftstruct violet_wind_observatory -> riftstruct kind=violet_wind_observatory at=-993,98,-880 blocks=2587 sub_air=0 cam=-977,108,-861:-994,104,-874 
  OK   структура violet_wind_observatory построена: blocks=2587, sub_air=0
  /riftstruct elisar_eye_occlusion -> riftstruct kind=elisar_eye_occlusion at=170,73,605 blocks=250 sub_air=0 cam=170,73,605:179,93,629 
  OK   структура elisar_eye_occlusion построена: blocks=250, sub_air=0
  /riftstruct colonnade_boss_pillars -> riftstruct kind=colonnade_boss_pillars at=1220,89,-536 bosses=1 minions=2 attack_ok=1 blocks=170 sub_air=0 cam=1204,98,-559:1217,93,-545 
  OK   структура colonnade_boss_pillars построена: blocks=170, sub_air=0
  /riftstruct colonnade_boss_hail -> riftstruct kind=colonnade_boss_hail at=1220,89,-536 bosses=1 minions=2 attack_ok=1 blocks=170 sub_air=0 cam=1202,98,-563:1216,99,-546 
  OK   структура colonnade_boss_hail построена: blocks=170, sub_air=0
  /riftstruct origami_dragon_closeup -> riftstruct kind=origami_dragon_closeup at=-971,127,-29 city_safe=true dragons=1 blocks=220 sub_air=0 cam=-959,131,-13:-964,127,-37 
  OK   структура origami_dragon_closeup построена: blocks=220, sub_air=0
  /riftstruct origami_dragon_under_release -> riftstruct kind=origami_dragon_under_release at=-1002,78,-57 city_safe=true dragons=1 blocks=220 sub_air=0 cam=-986,73,-24:-999,79,-60 
  OK   структура origami_dragon_under_release построена: blocks=220, sub_air=0
  /riftstruct origami_dragon_over_release -> riftstruct kind=origami_dragon_over_release at=-974,131,-32 city_safe=true dragons=1 blocks=220 sub_air=0 cam=-960,139,-1:-970,129,-35 
  OK   структура origami_dragon_over_release построена: blocks=220, sub_air=0
  /riftstruct blacktide_bridge_shore -> riftstruct kind=blacktide_bridge_shore at=2237,74,2304 ocean_only=true bridge_seabed=true sub_bridge=220 lvl1_east=0 water_ring=1 wall_breaches=0 tower_stairs=1027 stairs_jammed=0 rotunda_door=48 blocks=0 sub_air=0 cam=2207,76,2288:2287,90,2306 
  FAIL структура blacktide_bridge_shore: blocks=0 (ожидалось >=6000), sub_air=0
  /riftstruct blacktide_bay_aerial -> riftstruct kind=blacktide_bay_aerial at=2325,100,2320 ocean_only=true bridge_seabed=true sub_bridge=215 lvl1_east=227 water_ring=6 wall_breaches=0 tower_stairs=1027 stairs_jammed=0 rotunda_door=48 blocks=149968 sub_air=0 cam=2217,124,2228:2319,92,2330 
  OK   структура blacktide_bay_aerial построена: blocks=149968, sub_air=0
  /riftstruct blacktide_cathedral_hall -> riftstruct kind=blacktide_cathedral_hall at=2324,101,2320 carpets=575 thrones=1 chandeliers=14 pillars=1721 gilded=0 blocks=2626 sub_air=0 cam=2324,101,2320:2361,102,2320 
  OK   структура blacktide_cathedral_hall построена: blocks=2626, sub_air=0
  /riftstruct blacktide_second_floor -> riftstruct kind=blacktide_second_floor at=2326,111,2320 floor2_blocks=1316 warm_lights=452 floor2_books=144 blocks=3844 sub_air=0 cam=2326,111,2320:2359,105,2320 
  OK   структура blacktide_second_floor построена: blocks=3844, sub_air=0
  /riftstruct blacktide_rotunda_sanctum -> riftstruct kind=blacktide_rotunda_sanctum at=2373,101,2320 carpets=124 altars=1 books=434 gilded=0 blocks=1424 sub_air=0 cam=2373,101,2320:2390,99,2320 
  OK   структура blacktide_rotunda_sanctum построена: blocks=1424, sub_air=0
  /riftstruct blacktide_tower_walkability -> riftstruct kind=blacktide_tower_walkability at=2297,100,2338 climbed_towers=28 total_stairs=1027 blocks=1027 sub_air=0 cam=2297,100,2338:2339,118,2332 
  OK   структура blacktide_tower_walkability построена: blocks=1027, sub_air=0
  /riftstruct blacktide_citadel -> riftstruct kind=blacktide_citadel at=2325,94,2320 ocean_only=true bridge_seabed=true sub_bridge=220 lvl1_east=227 water_ring=8 wall_breaches=0 tower_stairs=1027 stairs_jammed=0 rotunda_door=48 blocks=149968 sub_air=0 cam=2237,114,2424:2323,94,2332 
  OK   структура blacktide_citadel построена: blocks=149968, sub_air=0
  OK   Цитадель: все 4 линии защитных стен герметичны (wall_breaches=0)
  OK   Цитадель: все винтовые лестницы 28 башен проходимы (ступеней=1027, заблокировано=0)
  OK   Цитадель: у круглой Ротонды есть настоящий сквозной вход (rotunda_door=48)
  /riftstruct ashen_burned_church -> riftstruct kind=ashen_burned_church at=2425,64,1137 blocks=2307 sub_air=0 cam=2400,75,1155:2423,72,1137 
  OK   структура ashen_burned_church построена: blocks=2307, sub_air=0
  /riftstruct ashen_combo_matrix -> riftstruct kind=ashen_combo_matrix at=2477,64,-3150 empty=14 partial=63 all_three=11 ash_r=407 blocks=2041 sub_air=0 cam=2429,88,-3102:2519,66,-3192 
  OK   структура ashen_combo_matrix построена: blocks=2041, sub_air=0
  /riftstruct enlarged_biomes_audit -> riftstruct kind=enlarged_biomes_audit at=-989,84,-1202 ash_r=434 violet_r=146 crystal_r=83 sizo_r=177 gardens_r=82 mist_r=105 blocks=600 sub_air=0 cam=-1031,111,-1160:-965,96,-1226 
  OK   структура enlarged_biomes_audit построена: blocks=600, sub_air=0
  /riftstruct connected_carpets_showcase -> riftstruct kind=connected_carpets_showcase at=768,137,768 carpets=68 blocks=340 sub_air=0 cam=768,147,778:768,137,768 
  OK   структура connected_carpets_showcase построена: blocks=340, sub_air=0
  /riftstruct city_sanctuary_garden -> riftstruct kind=city_sanctuary_garden at=0,110,18 sanctuary=1733 white_plaza=5310 copper_dome=753 dt_branches=482 plants=4132 blocks=11175 sub_air=0 cam=4,113,34:0,111,-3 
  OK   структура city_sanctuary_garden построена: blocks=11175, sub_air=0
  /riftstruct city_sanctuary_plaza_aerial -> riftstruct kind=city_sanctuary_plaza_aerial at=0,110,18 sanctuary=1733 white_plaza=5310 copper_dome=753 dt_branches=482 plants=4132 blocks=11175 sub_air=0 cam=26,144,36:0,108,-4 
  OK   структура city_sanctuary_plaza_aerial построена: blocks=11175, sub_air=0
  /riftstruct city_ornate_bridge -> riftstruct kind=city_ornate_bridge at=157,107,-33 viaduct_stone=895 columns=21 lanterns=19 blocks=1495 sub_air=0 cam=135,117,-39:157,107,-33 
  OK   структура city_ornate_bridge построена: blocks=1495, sub_air=0
  /riftstruct wild_inverted_tower_dragon -> riftstruct kind=wild_inverted_tower_dragon at=897,215,-129 coiled=true gilded_whiskers=true beyond_sim_saved=true blocks=5583 sub_air=0 cam=916,223,-110:897,214,-129 
  OK   структура wild_inverted_tower_dragon построена: blocks=5583, sub_air=0
  /riftstruct dragon_horn_beyond_sim -> riftstruct kind=dragon_horn_beyond_sim at=-620,148,-420 horn_summon=true tamed_in_sky=1 blocks=450 sub_air=0 cam=-620,148,-420:-620,166,-386 
  OK   структура dragon_horn_beyond_sim построена: blocks=450, sub_air=0
  /riftstruct astral_prism_epic -> riftstruct kind=astral_prism_epic at=768,138,736 cataclysm_entities=12 blocks=360 sub_air=0 cam=768,145,752:768,141,736 
  OK   структура astral_prism_epic построена: blocks=360, sub_air=0
  /riftstruct v27_new_blocks_gallery -> riftstruct kind=v27_new_blocks_gallery at=768,138,704 new_blocks=30/30 blocks=600 sub_air=0 cam=768,143,715:768,138,703 
  OK   структура v27_new_blocks_gallery построена: blocks=600, sub_air=0
  /riftstruct peaceful_sky_dart_fish -> riftstruct kind=peaceful_sky_dart_fish at=880,138,620 spawned=5 blocks=600 sub_air=0 cam=880,138,623:880,139,620 
  OK   структура peaceful_sky_dart_fish построена: blocks=600, sub_air=0
  /riftstruct peaceful_blossom_stag -> riftstruct kind=peaceful_blossom_stag at=960,138,700 spawned=4 blocks=480 sub_air=0 cam=960,137,703:960,138,700 
  OK   структура peaceful_blossom_stag построена: blocks=480, sub_air=0
  /riftstruct v28_wood_and_stone_gallery -> riftstruct kind=v28_wood_and_stone_gallery at=1040,138,780 placed=106/106 items=106/106 axe_strip=7/7 blocks=900 sub_air=0 cam=1040,147,796:1040,137,779 
  OK   структура v28_wood_and_stone_gallery построена: blocks=900, sub_air=0
  /riftstruct v28_elisar_perf_2chunks -> riftstruct kind=v28_elisar_perf_2chunks at=-401,63,-117 effR2=76.0 wake2ms=0.050 wake12ms=0.150 island10kMs=2.99 biome10kMs=14.37 hits=2308 blocks=800 sub_air=0 cam=-325,90,-95:-401,65,-117 
  OK   структура v28_elisar_perf_2chunks построена: blocks=800, sub_air=0
  /riftstruct v29_candle_portal_open -> riftstruct kind=v29_candle_portal_open at=820,146,760 lit_candles=8 stage3_mosaic=1 portal_bridge=24 blocks=780 sub_air=0 cam=811,151,750:821,145,765 
  OK   структура v29_candle_portal_open построена: blocks=780, sub_air=0
  /riftstruct v29_blacktide_seabed_bridge -> riftstruct kind=v29_blacktide_seabed_bridge at=2237,68,2304 pure_ocean=true sub_bridge=2079 water_above=297 above_far_island=0 blocks=2079 sub_air=0 cam=2211,73,2336:2263,70,2304 
  OK   структура v29_blacktide_seabed_bridge построена: blocks=2079, sub_air=0
  /riftstruct v29_crafting_and_coin_ore -> riftstruct kind=v29_crafting_and_coin_ore at=890,146,760 passed_crafts=8/8 gilded_3x3=true ducat_smelt=true blocks=800 sub_air=0 cam=890,148,767:890,145,760 
  OK   структура v29_crafting_and_coin_ore построена: blocks=800, sub_air=0
  /riftstruct v29_sync_ocean_waves_perf -> riftstruct kind=v29_sync_ocean_waves_perf at=310,76,310 r2=28.0 r6=92.0 r12=188.0 fish_despawn=true blocks=900 sub_air=0 cam=310,88,310:365,64,365 
  OK   структура v29_sync_ocean_waves_perf построена: blocks=900, sub_air=0
  /riftstruct v31_morok_gallery -> riftstruct kind=v31_morok_gallery at=1160,138,780 placed=27/27 items=27/27 lang=27/27 blocks=900 sub_air=0 cam=1160,141,795:1160,137,782 
  OK   структура v31_morok_gallery построена: blocks=900, sub_air=0
  /riftstruct sea_cave_exit -> riftstruct kind=sea_cave_exit at=-15,14,36 mouth_cols=41 cave_water=622 blocks=1032 sub_air=0 cam=-10,20,38:-14,7,37 
  OK   структура sea_cave_exit построена: blocks=1032, sub_air=0
  /riftstruct locate_all_audit -> riftstruct kind=locate_all_audit at=0,112,0 structs_found=26/26 biomes_found=9/9 wild_tower_locate=true blocks=700 sub_air=0 cam=24,154,68:0,116,0 
  OK   аналитический /locate нашёл ВСЕ 26 структур (26/26, включая inverted_tower и city_sanctuary) и ВСЕ 9 биомов (9/9) с небесной высоты!
  /locate structure riftworld:blacktide_citadel -> The nearest riftworld:blacktide_citadel is at [513, ~, -1775] (1681 blocks away) 
  OK   ванильная команда /locate structure riftworld:blacktide_citadel мгновенно вернула координаты
  /locate structure riftworld:city_sanctuary -> The nearest riftworld:city_sanctuary is at [0, ~, -4] (181 blocks away) 
  OK   ванильная команда /locate structure riftworld:city_sanctuary мгновенно вернула координаты
  /locate structure riftworld:inverted_tower -> The nearest riftworld:inverted_tower is at [-104, ~, 22] (276 blocks away) 
  OK   ванильная команда /locate structure riftworld:inverted_tower мгновенно вернула координаты
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
  /locate biome riftworld:crystal_rifts -> The nearest riftworld:crystal_rifts is at [1220, 91, -536] (1172 blocks away) 
  OK   ванильная команда /locate biome riftworld:crystal_rifts мгновенно вернула координаты
  [Overworld] /locate structure riftworld:overgrown_temple -> The nearest riftworld:overgrown_temple is at [96, ~, 128] (160 blocks away) 
  OK   ванильная команда /locate structure riftworld:overgrown_temple работает в Обычном мире
  /riftstruct spawn_eggs_audit -> riftstruct kind=spawn_eggs_audit at=540,98,-420 eggs_ok=18/18 dragon_egg_spawn=1 blocks=450 sub_air=0 cam=514,106,-394:540,102,-420 
  OK   все 18/18 яиц призыва мобов зарегистрированы (включая рыбку-стрекозу, цветущего оленя и Дракона)!
  /riftportal observatory_find -> riftportal observatory_find natural=true at=96,-48,128 
  /riftportal observatory_verify -> riftportal observatory_verify seal=104,-59,136 candles=8 chests=2 lecterns=1 rugs=25 cam=106,-55,148:104,-56,134 
  OK   Обсерватория Обычного мира построена со всем ритуальным набором: candles=8, chests=2
  /riftportal observatory_awaken -> riftportal observatory_awaken rift=true lit_candles=8 lit_runes=72 cam=103,-55,146:104,-52,136 
  OK   Обсерватория пробудила все руны, свечи и Белый 3D-Пролом в Элисар!
  /riftportal setup -> riftportal setup center=24,-60,0 snapshot=2435 cam=28,-57,24:24,-49,0 
  /riftportal trigger -> riftportal trigger started=true 
  /riftportal walkin -> riftportal walkin started=true 
  после захода в белый пролом и катсцены: Dimension=TestPlayer has the following entity data: "riftworld:elisar"  | riftportal verify mutated=0 total=2435 
  OK   белый 3D-Пролом при входе бесшовно перенёс в Белый Город с катсценой, изменённых блоков: 0
  игрок: TestPlayer has the following entity data: [0.5d, 105.0d, 22.5d]
  рыбы:  Elisar Sea Fish has the following entity data: [-25.55720922223205d, 23.964931745957813d, 13.852130764334921d]
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
- ci-shots/128_blacktide_second_floor_gallery.jpg
- ci-shots/129_city_sanctuary_lush_gardens.jpg
- ci-shots/12_fish_closeup.jpg
- ci-shots/130_city_ornate_viaduct_bridge.jpg
- ci-shots/131_wild_inverted_tower_coiled_dragon.jpg
- ci-shots/132_astral_prism_cataclysm.jpg
- ci-shots/133_dragon_horn_beyond_sim_summon.jpg
- ci-shots/134_blacktide_ashen_bay_overview.jpg
- ci-shots/135_city_sanctuary_circular_white_plaza.jpg
- ci-shots/136_v27_thirty_new_blocks_gallery.jpg
- ci-shots/137_peaceful_sky_dart_dragonfly_fish.jpg
- ci-shots/138_peaceful_snow_white_blossom_stags.jpg
- ci-shots/139_v28_all_wood_and_stone_sets_gallery.jpg
- ci-shots/13_reef_floor.jpg
- ci-shots/140_v28_elisar_lag_free_perf_2chunks.jpg
- ci-shots/141_v29_candle_lit_portal_opened.jpg
- ci-shots/142_v29_blacktide_seabed_bridge_underwater.jpg
- ci-shots/143_v29_crafting_and_coin_ore_showcase.jpg
- ci-shots/144_v29_sync_clamped_ocean_waves.jpg
- ci-shots/145_v31_morok_hollow_blocks_gallery.jpg
- ci-shots/147_v34_pegasus_winged_steed.jpg
- ci-shots/148_v35_wendigo_antlered_horror.jpg
- ci-shots/149_v36_morok_hollow_fog_forest.jpg
- ci-shots/14_tree_branches.jpg
- ci-shots/14b_tree_top.jpg
- ci-shots/150_v38_morok_natural_forest.jpg
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

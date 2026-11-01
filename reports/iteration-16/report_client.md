# CI, шаг 2: настоящий клиент Minecraft в Элисаре

- клиент: ForgeGradle runClient под Xvfb (llvmpipe, без GPU)
- воды в колонке (696,y,696): **57** блоков
- море: дно y=10, вода 51 блоков, глубочайшее дно y=1
- пещеры: 8497 колонок, воды 202460 / воздуха 11606 блоков (у свода 6145, висячего 916)
- провалов проб: **32**
- строк Exception/ERROR в логе клиента (без звука/наратора): **4**

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
  рыба striped: Elisar Sea Fish has the following entity data: [696.4806291240423d, 39.91497949801478d, 693.92102680926d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[45f,0f],sf_variant:3} -> Summoned new Elisar Sea Fish
  рыба jelly: Elisar Sea Fish has the following entity data: [696.4806291240423d, 39.91497949801478d, 693.92102680926d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[45f,0f],sf_variant:4} -> Summoned new Elisar Sea Fish
  рыба ray: Elisar Sea Fish has the following entity data: [696.4903145620211d, 39.90928345322609d, 693.9113092637284d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[90f,0f],sf_variant:5} -> Summoned new Elisar Sea Fish
  рыба angler: Elisar Sea Fish has the following entity data: [696.4806291240423d, 39.91505675198468d, 693.92102680926d]
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
  /riftdtat у посадки дерева -> riftdtat at=9,104,9 dt=120 dy=0 lv=86 air=825 water=0 top=116:riftworlddt:sizo_leaves col=rooty_elisar_soil,sizo_branchx6,airx4,sizo_leavesx4,air blocks=rooty_elisar_soil=1 sizo_branch=33 sizo_leaves=86 near=elisar_bricks=171 temple_floor=61 garden_water=28 ashlar=4 temple_mosaic=4 elisar_cobble=2
  /dynamictrees gettree -> Species: Sizo Tree JoCode: JJJzJJxx98+RJOOPvn1JJ8JP  
  контрольный дуб: Successfully set tree at position -9, 104, 19 to species Oak with JoCode JJJ0JR7Jxx+nXv.  
  /riftdtat у контрольного дуба -> riftdtat at=-9,104,19 dt=1 dy=154 lv=135 air=818 water=0 top=116:dynamictrees:oak_leaves col=rooty_elisar_soil,oak_branchx6,airx2,oak_branch,oak_leavesx3,airx3 blocks=rooty_elisar_soil=1 near=elisar_bricks=156 keep_stone
  саженец + костная мука -> riftgrow at=25,105,1 block=riftworlddt:sizo_sapling grew=8 after=riftworlddt:sizo_branch  
  /riftdtat у саженца -> riftdtat at=25,104,1 dt=62 dy=0 lv=55 air=911 water=0 top=112:riftworlddt:sizo_leaves col=rooty_elisar_soil,sizo_branchx6,sizo_leavesx2,airx6,temple_bricks blocks=rooty_elisar_soil=1 sizo_branch=6 sizo_leaves=55 near=eli
  OK   саженец аддона вырос в дерево DT: блоков 62 (листвы 55)
  OK   DT вырастил дерево аддона: блоков 120 (ветвей 33, листвы 86)
  [БИОМ] секция садов и рощи
  режим игрока -> Set TestPlayer's game mode to Spectator Mode  
  /riftbiome gardens (поиск) -> riftbiome name=gardens at=-470,-1349 bio=riftworld:waterfall_gardens r=46 top=-2147483648 water=0 garden_grass=0 garden_leaves=0 garden_logs=0 ivy=0 planks=0 lantern=0 struct=- fall=-471,-1331 wall=-4
  /riftbiome gardens (счёт) -> riftbiome name=gardens at=-470,-1349 bio=riftworld:waterfall_gardens r=46 top=104 water=14380 garden_grass=2959 garden_leaves=0 garden_logs=0 ivy=3284 planks=0 lantern=0 struct=- fall=-471,-1331 wall=-457,79,-1318:-457,80,-1329 river=-412,92,-1339:-440,68,-1383 isles=4 isl_r=28..46 sea=62 falls_water=913 falls_src=70 garden_oak=3209 sizo_blocks=0 below_sea_cols=1474 submerged_air=0 blocks=garden_limestone=46102 elisar_stone=22780 garden_limestone_mossy=8086 elisar_stone_mossy=4863 elisar_soil=4552 garden_oak_leaves=2792  
  OK   сады: биом острова riftworld:waterfall_gardens (биом-сурс совпадает с рельефом)
  /locate biome waterfall_gardens -> The nearest riftworld:waterfall_gardens is at [-470, 130, -1349] (0 blocks away)  
  /riftwhere у найденного биома -> riftwhere at=-470,-1349 biome=riftworld:waterfall_gardens top=98:riftworld:garden_grass ground=98:riftworld:garden_grass sea=62 garden=true  
  OK   /locate biome waterfall_gardens привёл на землю садов (-470,130,-1349, верх: riftworld:garden_grass, земля 98:riftworld:garden_grass)
  /riftaround у точки /locate -> riftaround at=-470,100,-1349 r=24 cols=2401 land=1933 garden_limestone=23599 garden_grass=1416 lavender=0 elisar_stone=15951 holes=0 gaps=0  
  OK   /locate привёл на настоящий садовый остров: известняка полок 23599 блоков в r=24
  OK   под островом садов нет дыр до бедрока (holes=0, трава садов 1416)
  OK   сады: зелёной травы 2959 блоков
  OK   сады: воды 14380 блоков (пруды и водопады, без затопления)
  OK   сады: плюща на стенах 3284 блоков
  OK   сады: рукотворного нет (досок 0, фонарей 0)
  OK   сады: островов 4 (3–6 больших)
  OK   сады: разброс радиусов островов 28..46 (от 22..38 до 43..55)
  OK   сады: садового дуба аддона DT 3209 блоков
  OK   сады: сизоствола нет (sizo_blocks=0)
  OK   сады: водопады текут — 913 блоков струй, у 70 есть исток на кромке
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
  /riftbiome sizo (счёт) -> <timeout> 
  /riftbiome sizo: структуры -> carts=1 shrines=0 temples=1 trail=0 flowers=0 pillars=0
  FAIL роща сизо: тропа=0 столбы=0 цветы=0 (нужно >=100, >=4, >=80)
  /riftdtwild (роща сизо) -> <unreachable: [Errno 111] Connection refused> 
  FAIL роща сизо: DT-блоков 0, листвы крон 0 (нужно >=400 и >=300)
  /riftbiome violet (поиск) -> <unreachable: [Errno 111] Connection refused> 
  FAIL /riftbiome violet не нашёл регион Фиолетовых холмов
  OK   роща: биом острова riftworld:sizo_grove
  /locate biome sizo_grove -> <unreachable: [Errno 111] Connection refused> 
  FAIL /locate biome sizo_grove не вернул координаты
  FAIL Фиолетовые холмы: биом луга ? вместо riftworld:violet_hills
  /locate biome violet_hills -> <unreachable: [Errno 111] Connection refused> 
  FAIL /locate biome violet_hills не вернул координаты
  /riftdtwhy (холмы) -> <unreachable: [Errno 111] Connection refused> 
  OK   DT/холмы: грунт найден
  FAIL DT/холмы: soil_ok=false — почва луга не в списке DT
  OK   DT/холмы: база DT ответила по виду
  /riftdtwild (холмы) -> <unreachable: [Errno 111] Connection refused> 
  /riftdtdbg (холмы) -> <unreachable: [Errno 111] Connection refused> 
  FAIL DT в мире не растёт (0 блоков аддона) — фичи биома не запускаются
  /locate biome crystal_rifts -> <unreachable: [Errno 111] Connection refused> 
  FAIL /locate biome crystal_rifts не вернул координаты
  FAIL /riftbiome crystal не нашёл архипелаг Хрустальных разломов
  FAIL /riftstruct crashed_ship не нашёл структуру (<unreachable: [Errno 111] Connection refused> )
  FAIL /riftstruct sunken_ship не нашёл структуру (<unreachable: [Errno 111] Connection refused> )
  FAIL /riftstruct ocean_building не нашёл структуру (<unreachable: [Errno 111] Connection refused> )
  FAIL /riftstruct white_village не нашёл структуру (<unreachable: [Errno 111] Connection refused> )
  FAIL /riftstruct burned_village не нашёл структуру (<unreachable: [Errno 111] Connection refused> )
  FAIL /riftstruct stilt_village не нашёл структуру (<unreachable: [Errno 111] Connection refused> )
  FAIL /riftstruct city_castle_front не нашёл структуру (<unreachable: [Errno 111] Connection refused> )
  FAIL /riftstruct city_castle_aerial не нашёл структуру (<unreachable: [Errno 111] Connection refused> )
  FAIL /riftstruct city_castle_north не нашёл структуру (<unreachable: [Errno 111] Connection refused> )
  FAIL /riftstruct colonnade_boss не нашёл структуру (<unreachable: [Errno 111] Connection refused> )
  FAIL /riftstruct colonnade_boss_minion не нашёл структуру (<unreachable: [Errno 111] Connection refused> )
  FAIL /riftstruct ocean_island_waves не нашёл структуру (<unreachable: [Errno 111] Connection refused> )
  FAIL /riftstruct waterfall_ocean_waves не нашёл структуру (<unreachable: [Errno 111] Connection refused> )
  FAIL /riftstruct harbor_port не нашёл структуру (<unreachable: [Errno 111] Connection refused> )
  FAIL /riftstruct grand_theater не нашёл структуру (<unreachable: [Errno 111] Connection refused> )
  FAIL /riftstruct sea_lighthouse не нашёл структуру (<unreachable: [Errno 111] Connection refused> )
  FAIL /riftstruct ancient_tree_altar не нашёл структуру (<unreachable: [Errno 111] Connection refused> )
  FAIL /riftstruct ashen_belfry_barrow не нашёл структуру (<unreachable: [Errno 111] Connection refused> )
  FAIL /riftstruct shattered_shard_forge не нашёл структуру (<unreachable: [Errno 111] Connection refused> )
  FAIL /riftstruct violet_wind_observatory не нашёл структуру (<unreachable: [Errno 111] Connection refused> )
  FAIL /riftstruct elisar_eye_occlusion не нашёл структуру (<unreachable: [Errno 111] Connection refused> )
  /riftportal setup -> <unreachable: [Errno 111] Connection refused> 
  FAIL /riftportal setup не вернул камеру
  игрок: <unreachable: [Errno 111] Connection refused>
  рыбы:  <unreachable: [Errno 111] Connection refused>
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
- ci-shots/60_ashen_castle_biome.jpg
- ci-shots/61_ashen_castle_closeup.jpg
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

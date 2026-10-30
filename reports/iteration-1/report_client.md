# CI, шаг 2: настоящий клиент Minecraft в Элисаре

- клиент: ForgeGradle runClient под Xvfb (llvmpipe, без GPU)
- воды в колонке (696,y,696): **57** блоков
- море: дно y=10, вода 51 блоков, глубочайшее дно y=1
- пещеры: 8497 колонок, воды 202460 / воздуха 11606 блоков (у свода 6145, висячего 916)
- провалов проб: **1**
- строк Exception/ERROR в логе клиента (без звука/наратора): **16**

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
  рыба angler: Elisar Sea Fish has the following entity data: [696.5d, 39.90928345322609d, 693.9015917181969d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[90f,0f],sf_variant:6} -> Summoned new Elisar Sea Fish
  рыба eel: Elisar Sea Fish has the following entity data: [696.4903145620211d, 39.90928345322609d, 693.9113092637284d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[90f,0f],sf_variant:7} -> Summoned new Elisar Sea Fish
  рыба surgeon: Elisar Sea Fish has the following entity data: [696.5d, 39.90928345322609d, 693.9015917181969d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[90f,0f],sf_variant:8} -> Summoned new Elisar Sea Fish
  рыба turtle: Elisar Sea Fish has the following entity data: [696.4903145620211d, 39.90928345322609d, 693.9113092637284d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[90f,0f],sf_variant:9} -> Summoned new Elisar Sea Fish
  рыба gloom: Elisar Sea Fish has the following entity data: [696.4903145620211d, 39.90928345322609d, 693.9113092637284d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[90f,0f],sf_variant:10} -> Summoned new Elisar Sea Fish
  рыба sting: Elisar Sea Fish has the following entity data: [696.4903145620211d, 39.90928345322609d, 693.9113092637284d]
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
  dynamictrees settree 9,105,151 riftworlddt:sizo -> Successfully set tree at position 9, 105, 151 to species Sizo Tree with JoCode JJJ0JP1JJ+ZJOOPvnRJOOPvn.  
  /riftdtat у посадки дерева -> riftdtat at=9,105,151 dt=120 dy=0 lv=86 air=853 water=0 top=117:riftworlddt:sizo_leaves col=rooty_elisar_soil,sizo_branchx6,airx4,sizo_leavesx4,air blocks=rooty_elisar_soil=1 sizo_branch=33 sizo_leaves=86 near=elisar_stone=147 elisar_crust=80 elisar_stone_mossy=15
  /dynamictrees gettree -> Species: Sizo Tree JoCode: JJJzJJxx98+RJOOPvn1JJ8JP  
  контрольный дуб: Successfully set tree at position -9, 105, 161 to species Oak with JoCode JJJ0JR7Jxx+nXv.  
  /riftdtat у контрольного дуба -> riftdtat at=-9,105,161 dt=1 dy=156 lv=137 air=816 water=0 top=117:dynamictrees:oak_leaves col=rooty_elisar_soil,oak_branchx6,air,oak_leaves,oak_branch,oak_leavesx3,airx3 blocks=rooty_elisar_soil=1 near=elisar_crust=81 el
  саженец + костная мука -> riftgrow at=25,106,143 block=riftworlddt:sizo_sapling grew=8 after=riftworlddt:sizo_branch  
  /riftdtat у саженца -> riftdtat at=25,105,143 dt=144 dy=0 lv=135 air=829 water=0 top=111:riftworlddt:sizo_leaves col=rooty_elisar_soil,sizo_branchx4,sizo_leavesx2,airx9 blocks=rooty_elisar_soil=1 sizo_branch=8 sizo_leaves=135 near=elisar_stone
  OK   саженец аддона вырос в дерево DT: блоков 144 (листвы 135)
  OK   DT вырастил дерево аддона: блоков 120 (ветвей 33, листвы 86)
  [БИОМ] секция садов и рощи
  режим игрока -> Set TestPlayer's game mode to Spectator Mode  
  /riftbiome gardens (поиск) -> riftbiome name=gardens at=-470,-1349 bio=riftworld:waterfall_gardens r=46 top=-2147483648 water=0 garden_grass=0 garden_leaves=0 garden_logs=0 ivy=0 planks=0 lantern=0 struct=- fall=-471,-1331 wall=-4
  /riftbiome gardens (счёт) -> riftbiome name=gardens at=-470,-1349 bio=riftworld:waterfall_gardens r=46 top=104 water=14433 garden_grass=2964 garden_leaves=0 garden_logs=0 ivy=3266 planks=0 lantern=0 struct=- fall=-471,-1331 wall=-457,79,-1318:-457,80,-1329 isles=4 isl_r=28..46 sea=62 falls_water=945 falls_src=69 garden_oak=3183 sizo_blocks=0 below_sea_cols=1474 submerged_air=0 blocks=garden_limestone=46102 elisar_stone=22780 garden_limestone_mos
  OK   сады: биом острова riftworld:waterfall_gardens (биом-сурс совпадает с рельефом)
  /locate biome waterfall_gardens -> The nearest riftworld:waterfall_gardens is at [-470, 130, -1349] (0 blocks away)  
  /riftwhere у найденного биома -> riftwhere at=-470,-1349 biome=riftworld:waterfall_gardens top=98:riftworld:garden_grass ground=98:riftworld:garden_grass sea=62 garden=true  
  OK   /locate biome waterfall_gardens привёл на землю садов (-470,130,-1349, верх: riftworld:garden_grass, земля 98:riftworld:garden_grass)
  /riftaround у точки /locate -> riftaround at=-470,132,-1349 r=24 cols=2401 land=1931 garden_limestone=653 garden_grass=88 lavender=0 elisar_stone=0 holes=0 gaps=0  
  FAIL /locate привёл НЕ на садовый остров: известняка 653 в r=24 (нужно >=1200)
  OK   под островом садов нет дыр до бедрока (holes=0, трава садов 88)
  OK   сады: зелёной травы 2964 блоков
  OK   сады: воды 14433 блоков (пруды и водопады, без затопления)
  OK   сады: плюща на стенах 3266 блоков
  OK   сады: рукотворного нет (досок 0, фонарей 0)
  OK   сады: островов 4 (3–6 больших)
  OK   сады: разброс радиусов островов 28..46 (от 22..38 до 43..55)
  OK   сады: садового дуба аддона DT 3183 блоков
  OK   сады: сизоствола нет (sizo_blocks=0)
  OK   сады: водопады текут — 945 блоков струй, у 69 есть исток на кромке
  OK   сады: подводные каменные ярусы есть (1474 колонок) и вода над ними не удалена (submerged_air=0)
  OK   сады: разбросанной листвы на земле нет (garden_leaves=0)
  кадр обрыва: камера -457 79 -1318 -> взгляд -457 80 -1329
  игрок после кадра обрыва -> TestPlayer has the following entity data: [-456.5d, 79.0d, -1317.5d]  
  /locate biome waterfall_gardens -> The nearest riftworld:waterfall_gardens is at [-457, 79, -1318] (0 blocks away)  
  /riftwhere у waterfall_gardens -> riftwhere at=-457,-1318 biome=riftworld:waterfall_gardens top=77:riftworld:garden_grass ground=77:riftworld:garden_grass sea=62 garden=true  
  OK   /locate biome waterfall_gardens привёл на землю биома (77:riftworld:garden_grass)
  /locate biome ashen_lands -> The nearest riftworld:ashen_lands is at [-137, 79, -934] (499 blocks away)  
  OK   /locate biome ashen_lands нашёл случайный биом адского острова (-137, -934)
  /riftbiome sizo (поиск) -> riftbiome name=sizo at=-240,841 bio=riftworld:sizo_grove r=80 top=-2147483648 water=0 garden_grass=0 garden_leaves=0 garden_logs=0 ivy=0 planks=0 lantern=0 struct=carts=0 shrines=0 temples=0 pillar_pa
  /riftbiome sizo (счёт) -> riftbiome name=sizo at=-240,841 bio=riftworld:sizo_grove r=80 top=137 water=38307 garden_grass=0 garden_leaves=0 garden_logs=0 ivy=0 planks=2 lantern=0 struct=carts=0 shrines=0 temples=0 pillar_pairs=1 fall=- wall=- falls_water=0 falls_src=0 garden_oak=0 sizo_blocks=0 below_sea_cols=0 submerged_air=0 blocks=elisar_stone=408666 elisar_stone_mossy=43412 sizo_leaves=40994 elisar_crust=12596 elisar_grass=8052 sizo_branch
  /riftbiome sizo: структуры -> carts=0 shrines=0 temples=0 pillar_pairs=1 fall= (по ТЗ: тележка ~1/3, памятник и храм ~1/7, 1-2 пары столбов)
  /rifttree у рощи -> rifttree at=105,112,-69 kind=sizo h=11 logs=0 branches=0 leaves=0 dist=126 loaded=true cam=116,129,-69 camtop=116,139,-69  
  /riftbiome violet (поиск) -> riftbiome name=violet at=409,469 bio=riftworld:violet_hills r=93 top=-2147483648 water=0 garden_grass=0 garden_leaves=0 garden_logs=0 ivy=0 planks=0 lantern=0 struct=- fall=- wall=- violet_blooms=0 vi
  /riftbiome violet (счёт) -> riftbiome name=violet at=409,469 bio=riftworld:violet_hills r=93 top=93 water=39043 garden_grass=329 garden_leaves=0 garden_logs=0 ivy=0 planks=0 lantern=0 struct=- fall=- wall=- violet_blooms=10570 violet_leaves=0 lavender=10126 fences=0 lamps=0 falls_water=0 falls_src=0 garden_oak=0 sizo_blocks=0 below_sea_cols=0 submerged_air=0 blocks=elisar_stone=79270 garden_limestone=34640 garden_limestone_mossy=14907 elisar_so
  OK   холмы: лаванды всего 20696 блоков (гряды 10126 + цветы 10570)
  OK   холмы: гряд лаванды 10126 блоков (сплошные поля, как на фото)
  /riftdtwild (холмы, лиловый хлыст) -> riftdtwild at=409,469 r=40 dt=274 rooty=0 branch=250 leaves=24 blocks=lilac_branch=250 lilac_leaves=24  
  OK   холмы: динамических деревьев аддона 274 блоков (лиловый хлыст)
  OK   роща: биом острова riftworld:sizo_grove
  /locate biome sizo_grove -> The nearest riftworld:sizo_grove is at [-168, 95, 868] (719 blocks away)  
  /riftwhere у sizo_grove -> riftwhere at=-168,868 biome=riftworld:sizo_grove top=107:riftworld:elisar_grass ground=107:riftworld:elisar_grass sea=62 garden=false  
  OK   /locate biome sizo_grove привёл на землю биома (107:riftworld:elisar_grass)
  OK   Фиолетовые холмы: биом луга riftworld:violet_hills
  /locate biome violet_hills -> The nearest riftworld:violet_hills is at [440, 95, 484] (0 blocks away)  
  /riftwhere у violet_hills -> riftwhere at=440,484 biome=riftworld:violet_hills top=82:riftworld:violet_bloom ground=82:riftworld:violet_bloom sea=62 garden=false  
  OK   /locate biome violet_hills привёл на землю биома (82:riftworld:violet_bloom)
  /riftdtwhy (холмы) -> riftdtwhy at=440,95,484 biome_surface=riftworld:violet_hills biome_top=riftworld:violet_hills biome_y0=riftworld:violet_hills dt_features=2 has_dt=true dt_species=handled:Species{registryName=riftworlddt:lilac} db_entries=7 db_populated=true plains_? sel=StaticSpeciesSelector gro
  OK   DT/холмы: грунт найден
  OK   DT/холмы: почва приемлема для DT
  OK   DT/холмы: база DT ответила по виду
  /riftdtwild (холмы) -> riftdtwild at=440,484 r=32 dt=610 rooty=0 branch=552 leaves=58 blocks=lilac_branch=552 lilac_leaves=58  
  /riftdtdbg (холмы) -> riftdtdbg at=440,95,484 discs=1 disc0_r=6 real=true ground=80:Block{minecraft:grass_block} handled=true sp=Species{registryName=riftworlddt:lilac} codes=7[2:12 3:12 4:12 5:12 6:12 7:12 8:12 ] code_for_r6=ok gen=true  
  OK   DT растёт в мире сам: 610 блоков аддона вокруг точки
  игрок: TestPlayer has the following entity data: [440.5d, 95.0d, 484.5d]
  рыбы:  Elisar Sea Fish has the following entity data: [574.0029139566233d, 47.85471429561287d, 575.9663140338747d]
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
- ci-shots/50_violet_fields.jpg
- ci-shots/51_violet_path.jpg
- ci-shots/60_ashen_castle_biome.jpg
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

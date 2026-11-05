# CI, шаг 2: настоящий клиент Minecraft в Элисаре

- клиент: ForgeGradle runClient под Xvfb (llvmpipe, без GPU)
- воды в колонке (696,y,696): **58** блоков
- море: дно y=10, вода 51 блоков, глубочайшее дно y=1
- пещеры: 8497 колонок, воды 202732 / воздуха 11560 блоков (у свода 6121, висячего 916)
- **клиент потерян посреди прогона**: оставшиеся кадры и пробы не снимались (см. маркер в client.log)
- провалов проб: **14**
- строк Exception/ERROR в логе клиента (без звука/наратора/authlib): **5**

## пробы
```
  /riftsea raw: riftsea floor=10 water=51 sea=62 min=1 max=15 cols=9409 caves=8497 wet=202732 air=11560 reef=1981 plants=1773 corals=64 float=916 ceilair=6121 bottom=-62 caveat=625,5,570 
  /riftsea raw: riftsea floor=10 water=51 sea=62 min=1 max=15 cols=9409 caves=8497 wet=202732 air=11560 reef=1981 plants=1773 corals=64 float=916 ceilair=6121 bottom=-62 caveat=625,5,570 
  OK   вода на глубине (696,56,696) = elisar_water
  /riftsea raw: riftsea floor=5 water=56 sea=62 min=-8 max=14 cols=8925 caves=8058 wet=153787 air=12734 reef=2118 plants=1402 corals=86 float=1013 ceilair=7144 bottom=-62 caveat=650,3,600 
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
  /riftstruct v34_pegasus -> riftstruct kind=v34_pegasus at=-6652,119,4218 alive=1 tamed=0 where=morok_hollow spawned=true saddleable=true saddle_seat=1.15 blocks=900 sub_air=0 cam=-6652.41,119.05,4215.376:-6651.5,118.3,4218.5 stage=13x13 
  OK   структура v34_pegasus построена: blocks=900, sub_air=0
  /riftstruct v35_wendigo -> riftstruct kind=v35_wendigo at=-6652,121,4218 alive=1 hp=64 darkness_on_hit=true antlers=6 blocks=900 sub_air=0 cam=-6652.909,120.85,4215.646:-6651.5,120.25,4218.5 stage=13x13 
  OK   структура v35_wendigo построена: blocks=900, sub_air=0
  /riftstruct v36_morok_biome -> riftstruct kind=v36_morok_biome at=-6652,122,4218 biome=riftworld:morok_hollow morok_weight=1.00 natural=true fog=true trees=5 lantern=true blocks=900 sub_air=0 cam=-6641,129,4231:-6652,123,4218 cam_vantage=true 
  OK   структура v36_morok_biome построена: blocks=900, sub_air=0
  /riftstruct v39_morok -> riftstruct kind=v39_morok at=-6652,122,4218 natural=true biome=riftworld:morok_hollow night=true alive=1 spawned=true model_boxes=62 bones=7 atlas=256 eye_boxes=2 eye_atlas=16 glowing_eyes=true spawn_morok=1 spawn_wendigo=0 day_chance=0.05 night_chance=0.45 stalk=true dash=true b
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
  рыба striped: Elisar Sea Fish has the following entity data: [696.4806291240423d, 39.91497949801478d, 693.92102680926d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[45f,0f],sf_variant:3} -> Summoned new Elisar Sea Fish
  рыба jelly: Elisar Sea Fish has the following entity data: [696.4806291240423d, 39.91505675198468d, 693.92102680926d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[45f,0f],sf_variant:4} -> Summoned new Elisar Sea Fish
  рыба ray: Elisar Sea Fish has the following entity data: [696.4806291240423d, 39.91497949801478d, 693.92102680926d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[90f,0f],sf_variant:5} -> Summoned new Elisar Sea Fish
  рыба angler: Elisar Sea Fish has the following entity data: [696.4903145620211d, 39.90928345322609d, 693.9113092637284d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[90f,0f],sf_variant:6} -> Summoned new Elisar Sea Fish
  рыба eel: Elisar Sea Fish has the following entity data: [696.4806291240423d, 39.91505675198468d, 693.92102680926d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[90f,0f],sf_variant:7} -> Summoned new Elisar Sea Fish
  рыба surgeon: Elisar Sea Fish has the following entity data: [696.4806291240423d, 39.91505675198468d, 693.92102680926d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[90f,0f],sf_variant:8} -> Summoned new Elisar Sea Fish
  рыба turtle: Elisar Sea Fish has the following entity data: [696.4806291240423d, 39.91505675198468d, 693.92102680926d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[90f,0f],sf_variant:9} -> Summoned new Elisar Sea Fish
  рыба gloom: Elisar Sea Fish has the following entity data: [696.4903145620211d, 39.90928345322609d, 693.9113092637284d]
  summon ^0 ^0 ^2.6 {NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[90f,0f],sf_variant:10} -> Summoned new Elisar Sea Fish
  рыба sting: Elisar Sea Fish has the following entity data: [696.4806291240423d, 39.91497949801478d, 693.92102680926d]
  OK   клиент не жаловался на geo-модели
  /rifttree raw: rifttree at=-76,104,6 kind=ambra h=7 logs=30 branches=14 leaves=110 dist=69 loaded=true cam=-65,117,6 camtop=-65,127,6
  OK   дерево найдено: -76,104,6 (ветвей: 14)
  OK   у дерева есть ветки: 14 блоков вне ствола
  /rifttree повтор: rifttree at=85,103,-13 kind=ambra h=6 logs=24 branches=16 leaves=168 dist=80 loaded=true cam=96,115,-13 camtop=96,125,-13
  OK   дерево с ветками и кроной: ветвей 16, листвы 168
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
  /riftdtat у контрольного дуба -> riftdtat at=-9,104,19 dt=1 dy=154 lv=135 air=818 water=0 top=116:dynamictrees:oak_leaves col=rooty_elisar_soil,oak_branchx6,airx2,oak_branch,oak_leavesx3,airx3 blocks=rooty_elisar_soil=1 near=elisar_crust=81 elisar_grass
  саженец + костная мука -> riftgrow at=25,105,1 block=riftworlddt:sizo_sapling grew=8 after=riftworlddt:sizo_branch 
  /riftdtat у саженца -> riftdtat at=25,104,1 dt=38 dy=0 lv=30 air=935 water=0 top=112:riftworlddt:sizo_leaves col=rooty_elisar_soil,sizo_branchx6,sizo_leaves,airx8 blocks=rooty_elisar_soil=1 sizo_branch=7 sizo_leaves=30 near=elisar_crust=81 eli
  OK   саженец аддона вырос в дерево DT: блоков 38 (листвы 30)
  OK   DT вырастил дерево аддона: блоков 120 (ветвей 33, листвы 86)
  [БИОМ] секция садов и рощи
  режим игрока -> Set TestPlayer's game mode to Spectator Mode 
  /riftbiome gardens (поиск) -> riftbiome name=gardens at=406,-650 bio=riftworld:waterfall_gardens r=82 top=-2147483648 water=0 garden_grass=0 garden_leaves=0 garden_logs=0 ivy=0 planks=0 lantern=0 struct=- fall=399,-675 wall=411,86
  /riftbiome gardens (счёт) -> riftbiome name=gardens at=406,-650 bio=riftworld:waterfall_gardens r=82 top=104 water=6327 garden_grass=8101 garden_leaves=0 garden_logs=0 ivy=2931 planks=0 lantern=0 struct=- fall=399,-675 wall=411,86,-628:411,87,-639 river=287,92,-677:243,68,-650 isles=5 isl_r=37..82 sea=62 falls_water=448 falls_src=63 garden_oak=11013 sizo_blocks=0 below_sea_cols=985 submerged_air=0 blocks=garden_limestone=114006 elisar_stone=48323 garden_limestone_mossy=20509 elisar_stone_mossy=12076 elisar_soil=11189 garden_oak_leaves=9567 riftbiome name=gardens at=406,-650 bio=rift
  OK   сады: биом острова riftworld:waterfall_gardens (биом-сурс совпадает с рельефом)
  /locate biome waterfall_gardens -> The nearest riftworld:waterfall_gardens is at [407, 90, -651] (40 blocks away) 
  /riftwhere у найденного биома -> riftwhere at=407,-651 biome=riftworld:waterfall_gardens top=104:riftworlddt:garden_oak_leaves ground=104:riftworlddt:garden_oak_leaves sea=62 garden=true 
  OK   /locate biome waterfall_gardens привёл на землю садов (407,90,-651, верх: riftworlddt:garden_oak_leaves, земля 104:riftworlddt:garden_oak_leaves)
  /riftaround у точки /locate -> riftaround at=407,106,-651 r=24 cols=2401 land=2109 garden_limestone=26910 garden_grass=1469 lavender=0 elisar_stone=13058 holes=0 gaps=0 
  OK   /locate привёл на настоящий садовый остров: известняка полок 26910 блоков в r=24
  OK   под островом садов нет дыр до бедрока (holes=0, трава садов 1469)
  OK   сады: зелёной травы 8141 блоков
  OK   сады: воды 6327 блоков (пруды и водопады, без затопления)
  OK   сады: плюща на стенах 2937 блоков
  OK   сады: рукотворного нет (досок 0, фонарей 0)
  OK   сады: островов 5 (увеличенный архипелаг 5–8 больших островов)
  OK   сады: разброс радиусов увеличенных островов 37..82
  OK   сады: садового дуба аддона DT 11013 блоков
  OK   сады: сизоствола нет (sizo_blocks=0)
  OK   сады: водопады текут — 448 блоков струй, у 63 есть исток на кромке
  OK   сады: подводные каменные ярусы есть (985 колонок) и вода над ними не удалена (submerged_air=0)
  OK   сады: разбросанной листвы на земле нет (garden_leaves=0)
  кадр обрыва: камера 411 86 -628 -> взгляд 411 87 -639
  игрок после кадра обрыва -> No entity was found 
  /locate biome waterfall_gardens ->  
  FAIL /locate biome waterfall_gardens не вернул координаты
  /locate biome ashen_lands ->  
  FAIL /locate biome ashen_lands не вернул координаты
  /riftbiome sizo (поиск) -> riftbiome name=sizo at=-799,2114 bio=riftworld:sizo_grove r=176 top=-2147483648 water=0 garden_grass=0 garden_leaves=0 garden_logs=0 ivy=0 planks=0 lantern=0 struct=carts=1 shrines=1 temples=1 pillar_
  /riftbiome sizo (счёт) -> riftbiome name=sizo at=-799,2114 bio=riftworld:sizo_grove r=176 top=-2147483648 water=0 garden_grass=0 garden_leaves=0 garden_logs=0 ivy=0 planks=0 lantern=0 struct=carts=1 shrines=1 temples=1 pillar_pairs=2 fall=- wall=- river=- sizo_trail=0 sizo_flowers=0 sizo_pillars=0 sview1=-703,104,2098:-840,105,2109 sview2=-733,103,2106:-865,104,2114 sview3=-778,104,2125:-870,107,2116 falls_water=0 falls_src=0 garden_oak=0 sizo_blocks=0 below_sea_cols=0 submerged_air=0 blocks=- 
  /riftbiome sizo: структуры -> carts=1 shrines=1 temples=1 pillar_pairs=2 fall= trail=0 flowers=0 pillars=0
  FAIL роща сизо: тропа=0 столбы=0 цветы=0 (нужно >=100, >=4, >=80)
  /riftdtwild (роща сизо) ->  
  FAIL роща сизо: DT-блоков 0, листвы крон 0 (нужно >=400 и >=300)
  /riftbiome violet (поиск) -> riftbiome name=violet at=-989,-1202 bio=riftworld:violet_hills r=131 top=-2147483648 water=0 garden_grass=0 garden_leaves=0 garden_logs=0 ivy=0 planks=0 lantern=0 struct=- fall=- wall=- river=- vislan
  /riftbiome violet (счёт) -> riftbiome name=violet at=-989,-1202 bio=riftworld:violet_hills r=131 top=-2147483648 water=0 garden_grass=0 garden_leaves=0 garden_logs=0 ivy=0 planks=0 lantern=0 struct=- fall=- wall=- river=- vislands=6 visl_r=82..132 violet_blooms=0 violet_leaves=0 lavender=0 accent_flowers=0 walls=0 fences=0 lamps=0 vground=82 road=-1077,88,-1223:-947,85,-1179 varch=-953,116,-1213:-796,86,-1109 vfields=-1031,99,-1216:-951,84,-1182 vpath=-994,84,-1197:-819,85,-1124 vtrees=- falls_water=0 falls_src=0 garden_oak=0 sizo_blocks=0 below_sea_cols=0 submerged_air=0 blocks=- 
  OK   холмы: увеличенный архипелаг из 6 огромных парящих островов
  OK   холмы: сизоствола нет (sizo_blocks=0)
  FAIL холмы: лаванды 0 + соцветий 0 (нужно >=2000 на поле)
  FAIL холмы: гряд лаванды 0 (нужно >=2000)
  FAIL холмы: придорожная ограда неполная — walls=0 fences=0 lamps=0
  FAIL холмы: акцентных цветов 0 (нужно немного: 25..2200)
  /riftdtwild (холмы, лиловый ветвецвет) ->  
  FAIL холмы: деревьев аддона 0 (нужно >=150 — в биоме должен расти лиловый ветвецвет)
  OK   роща: биом острова riftworld:sizo_grove
  /locate biome sizo_grove ->  
  FAIL /locate biome sizo_grove не вернул координаты
  OK   Фиолетовые холмы: биом луга riftworld:violet_hills
  /locate biome violet_hills ->  
  FAIL /locate biome violet_hills не вернул координаты
  /riftdtwhy (холмы) ->  
  OK   DT/холмы: грунт найден
  FAIL DT/холмы: soil_ok=false — почва луга не в списке DT
  OK   DT/холмы: база DT ответила по виду
  /riftdtwild (холмы) ->  
  /riftdtdbg (холмы) ->  
  FAIL DT в мире не растёт (0 блоков аддона) — фичи биома не запускаются
  FAIL Мороковая Падь: туман не включён ( )
  СТОП клиент потерян (перед бором Пади): JVM клиента умерла, дальше кадры и пробы невозможны
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
- ci-shots/10_fish_reef.jpg
- ci-shots/10b_fish_tropical.jpg
- ci-shots/11_fish_cave.jpg
- ci-shots/11b_fish_cave_rare.jpg
- ci-shots/12_fish_closeup.jpg
- ci-shots/13_reef_floor.jpg
- ci-shots/147_v34_pegasus_winged_steed.jpg
- ci-shots/148_v35_wendigo_antlered_horror.jpg
- ci-shots/149_v36_morok_hollow_fog_forest.jpg
- ci-shots/14_tree_branches.jpg
- ci-shots/14b_tree_top.jpg
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

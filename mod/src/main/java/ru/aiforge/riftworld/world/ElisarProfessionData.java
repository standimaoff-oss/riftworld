package ru.aiforge.riftworld.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * v0.55.0 — торговые списки профессий без Minecraft.
 *
 * <p>Профессии нужны в двух местах: в самом мире (жители торгуют настоящими
 * {@code ItemStack}) и в проверке экономики, которая должна запускаться
 * обычным {@code javac + java} без Forge. Этот класс — общая часть:
 * здесь лежат те же списки, но обычными строками-идентификаторами.</p>
 *
 * <p>Файл генерируется скриптом {@code tools/rift/gen_elisar_profession_data.py}
 * прямо из {@link ElisarProfession}: если торговля в мире и проверка
 * экономики разойдутся, тест будет врать, поэтому список правится
 * только через исходник.</p>
 */
public final class ElisarProfessionData {

    private static final Map<String, List<String>> GOODS = new LinkedHashMap<>();
    private static final Map<String, List<String>> BUYS = new LinkedHashMap<>();

    private ElisarProfessionData() {
    }

    /** Товары, которые торговец продаёт. */
    public static List<String> goods(String profession) {
        List<String> list = GOODS.get(profession);
        return list == null ? Collections.emptyList() : Collections.unmodifiableList(list);
    }

    /** Товары, которые торговец покупает у игрока. */
    public static List<String> buys(String profession) {
        List<String> list = BUYS.get(profession);
        return list == null ? Collections.emptyList() : Collections.unmodifiableList(list);
    }

    /** Все профессии — по именам, в порядке объявления. */
    public static List<String> professions() {
        return Collections.unmodifiableList(new ArrayList<>(GOODS.keySet()));
    }

    static {
        GOODS.put("MASON", List.of(
                "riftworld:sizo_planks",
                "riftworld:elisar_stone_wall",
                "riftworld:elisar_moss",
                "minecraft:bricks"
        ));
        BUYS.put("MASON", List.of(
                "minecraft:cobblestone",
                "minecraft:sand",
                "minecraft:stone"
        ));
        GOODS.put("SMITH", List.of(
                "riftworld:aurite_ingot",
                "riftworld:aurite_pickaxe",
                "riftworld:aurite_shovel",
                "riftworld:elisar_guard_sword"
        ));
        BUYS.put("SMITH", List.of(
                "minecraft:raw_iron",
                "minecraft:coal",
                "minecraft:iron_ingot"
        ));
        GOODS.put("ARMOURER", List.of(
                "riftworld:aurite_sword",
                "riftworld:elisar_bow",
                "riftworld:elisar_shield",
                "riftworld:elisar_guard_shield"
        ));
        BUYS.put("ARMOURER", List.of(
                "minecraft:chainmail_chestplate",
                "minecraft:flint",
                "minecraft:iron_ingot"
        ));
        GOODS.put("HERBALIST", List.of(
                "minecraft:potion",
                "minecraft:bread",
                "riftworld:blossom_moss_block",
                "minecraft:honey_bottle"
        ));
        BUYS.put("HERBALIST", List.of(
                "minecraft:nether_wart",
                "minecraft:brown_mushroom",
                "minecraft:sugar_cane"
        ));
        GOODS.put("FISHER", List.of(
                "minecraft:cod",
                "minecraft:salmon",
                "minecraft:tropical_fish",
                "minecraft:kelp"
        ));
        BUYS.put("FISHER", List.of(
                "minecraft:string",
                "minecraft:water_bucket",
                "minecraft:fishing_rod"
        ));
        GOODS.put("BAKER", List.of(
                "minecraft:bread",
                "minecraft:cake",
                "minecraft:wheat",
                "minecraft:honey_bottle"
        ));
        BUYS.put("BAKER", List.of(
                "minecraft:sugar",
                "minecraft:egg",
                "minecraft:carrot"
        ));
        GOODS.put("SCULPTOR", List.of(
                "minecraft:calcite",
                "minecraft:tuff",
                "minecraft:amethyst_shard",
                "riftworld:gilded_paper"
        ));
        BUYS.put("SCULPTOR", List.of(
                "minecraft:sandstone",
                "minecraft:polished_basalt",
                "minecraft:quartz"
        ));
        GOODS.put("MERCHANT", List.of(
                "riftworld:elisar_ducat",
                "riftworld:gilded_paper",
                "minecraft:emerald",
                "minecraft:redstone"
        ));
        BUYS.put("MERCHANT", List.of(
                "minecraft:raw_gold",
                "minecraft:coal",
                "minecraft:bone_block"
        ));
        GOODS.put("HUNTER", List.of(
                "minecraft:rabbit",
                "minecraft:mutton",
                "minecraft:rabbit_hide",
                "minecraft:feather"
        ));
        BUYS.put("HUNTER", List.of(
                "minecraft:arrow",
                "minecraft:bone",
                "minecraft:string"
        ));
        GOODS.put("BANKER", List.of(
                "riftworld:aurite_ingot",
                "minecraft:gold_ingot",
                "minecraft:emerald",
                "minecraft:redstone"
        ));
        BUYS.put("BANKER", List.of(
                "minecraft:raw_gold",
                "minecraft:gold_nugget",
                "minecraft:netherite_scrap"
        ));
        GOODS.put("TANNER", List.of(
                "minecraft:leather",
                "minecraft:rabbit_foot",
                "minecraft:rabbit_hide",
                "minecraft:leather_helmet"
        ));
        BUYS.put("TANNER", List.of(
                "minecraft:charcoal",
                "minecraft:string",
                "minecraft:dirt"
        ));
        GOODS.put("BREWER", List.of(
                "minecraft:brewing_stand",
                "minecraft:baked_potato",
                "minecraft:hay_block",
                "minecraft:wheat"
        ));
        BUYS.put("BREWER", List.of(
                "minecraft:wheat_seeds",
                "minecraft:nether_wart",
                "minecraft:glass_bottle"
        ));
        GOODS.put("JEWELLER", List.of(
                "minecraft:amethyst_shard",
                "minecraft:calcite",
                "minecraft:lapis_lazuli",
                "minecraft:quartz_block"
        ));
        BUYS.put("JEWELLER", List.of(
                "minecraft:diamond",
                "minecraft:gold_nugget",
                "minecraft:stick"
        ));
        GOODS.put("CARTER", List.of(
                "minecraft:saddle",
                "minecraft:lead",
                "minecraft:minecart",
                "minecraft:oak_boat"
        ));
        BUYS.put("CARTER", List.of(
                "minecraft:string",
                "minecraft:chest",
                "minecraft:iron_ingot"
        ));
    }
}

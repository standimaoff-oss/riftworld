import ru.aiforge.riftworld.world.ElisarEconomy;
import ru.aiforge.riftworld.world.ElisarProfessionData;

/**
 * v0.54.0 — проверка экономики Элисара.
 *
 * <p>Тест намеренно не зависит от Minecraft: {@code ElisarEconomy} — это чистая
 * арифметика цен, поэтому её можно проверить обычным {@code javac + java} в CI.</p>
 */
public final class ElisarEconomyTest {

    private static void check(boolean value, String message) {
        if (!value) {
            throw new AssertionError(message);
        }
    }

    public static void main(String[] args) {
        // ------------------------------------------------------------------
        // 1. Маржа: закупочная цена ВСЕГДА не выше продажной.
        //    Иначе торговец работает в минус, а игрок может крутить цикл
        //    «продать торговцу → купить у него же» и печатать дукаты.
        // ------------------------------------------------------------------
        int[] bases = {
                ElisarEconomy.BLOCK_BASE,
                ElisarEconomy.FOOD_BASE,
                ElisarEconomy.TOOL_BASE,
                ElisarEconomy.WEAPON_BASE,
                ElisarEconomy.AURITE_INGOT_BASE,
                ElisarEconomy.RIFTCRYSTAL_BASE,
                ElisarEconomy.INGOT_WORTH,
        };
        int[] rarities = {
                ElisarEconomy.RARITY_COMMON,
                ElisarEconomy.RARITY_UNCOMMON,
                ElisarEconomy.RARITY_RARE,
        };
        for (int base : bases) {
            for (int rarity : rarities) {
                int buy = ElisarEconomy.buyPrice(base, rarity);
                int sell = ElisarEconomy.sellPrice(base, rarity);
                check(buy > 0, "закупочная цена должна быть положительной");
                check(sell > 0, "продажная цена должна быть положительной");
                check(buy <= sell, "закупочная цена не должна превышать продажную");
                check(ElisarEconomy.pricesSafe(buy, sell), "цены должны быть безопасны");
            }
        }

        // ------------------------------------------------------------------
        // 2. Ни одна цена не проваливается в ноль и не уходит в минус.
        // ------------------------------------------------------------------
        for (int base : bases) {
            for (int rarity : rarities) {
                check(ElisarEconomy.buyPrice(base, rarity) >= ElisarEconomy.MIN_PRICE,
                        "закупочная цена не ниже минимума");
                check(ElisarEconomy.sellPrice(base, rarity) >= ElisarEconomy.MIN_PRICE + 1,
                        "у продажной цены есть минимальная маржа");
            }
        }

        // ------------------------------------------------------------------
        // 3. Плавающий множитель зажат сверху и снизу — экономика не рушится.
        //    Миллион сделок не должен уронить цену в ноль и не должен
        //    взорвать её в бесконечность.
        // ------------------------------------------------------------------
        float buy = 1.0F;
        for (int i = 0; i < 1000; i++) {
            buy = ElisarEconomy.priceFactor(buy, 10);
        }
        check(buy >= 1.0F / ElisarEconomy.MAX_PRICE_MULTIPLIER - 1e-5F,
                "множитель закупки не опускается ниже потолка");
        check(buy <= ElisarEconomy.MAX_PRICE_MULTIPLIER + 1e-5F,
                "множитель закупки не поднимается выше потолка");
        check(ElisarEconomy.apply(buy, 100) >= ElisarEconomy.MIN_PRICE,
                "цена после обесценивания остаётся положительной");

        float sell = 1.0F;
        for (int i = 0; i < 1000; i++) {
            sell = ElisarEconomy.sellFactor(sell, 10);
        }
        check(sell >= 1.0F / ElisarEconomy.MAX_PRICE_MULTIPLIER - 1e-5F,
                "множитель продажи не опускается ниже потолка");
        check(sell <= ElisarEconomy.MAX_PRICE_MULTIPLIER + 1e-5F,
                "множитель продажи не поднимается выше потолка");

        // Цена растёт только до потолка, сколько бы сделок ни прошло.
        int capped = ElisarEconomy.apply(sell, 100);
        check(capped <= 100 * ElisarEconomy.MAX_PRICE_MULTIPLIER,
                "потолок цены соблюдён");

        // ------------------------------------------------------------------
        // 4. Множитель никогда не «перескакивает» в ноль или отрицательное.
        // ------------------------------------------------------------------
        float f = 1.0F;
        for (int i = 0; i < 500; i++) {
            f = ElisarEconomy.priceFactor(f, 1);
            check(f > 0.0F, "множитель всегда положителен");
        }

        // ------------------------------------------------------------------
        // 5. Монетная цепочка: слиток → монета. Курс фиксирован рецептом.
        // ------------------------------------------------------------------
        check(ElisarEconomy.ingotWorth() >= 1, "слиток стоит хотя бы одну монету");
        check(ElisarEconomy.DUCAT == 1, "монета — базовая единица");
        // Скупщик платит за слиток ровно столько, сколько даёт переплавка:
        // арбитраж «купить слиток → переплавить» невозможен.
        check(ElisarEconomy.buyPrice(ElisarEconomy.INGOT_WORTH, ElisarEconomy.RARITY_UNCOMMON)
                <= ElisarEconomy.ingotWorth(),
                "переплавка слитка не должна быть выгоднее продажи торговцу");

        // ------------------------------------------------------------------
        // 6. Кросс-профессиональных циклов быть не должно.
        //    Если А продаёт то, что Б покупает, игрок может «перекладывать»
        //    товар из рук в руки и получать дукаты из воздуха.
        // ------------------------------------------------------------------
        java.util.Map<String, java.util.List<String>> sold = new java.util.LinkedHashMap<>();
        java.util.Map<String, java.util.List<String>> bought = new java.util.LinkedHashMap<>();
        sold.put("SMITH", java.util.List.of("aurite_ingot"));
        bought.put("SMITH", java.util.List.of("raw_iron"));
        sold.put("ARMOURER", java.util.List.of("aurite_sword"));
        // List.of() неизменяемый, а список нужно будет дополнить ниже.
        bought.put("ARMOURER", new java.util.ArrayList<>(java.util.List.of("leather")));
        check(ElisarEconomy.noCrossProfessionCycle(sold, bought),
                "чистый набор профессий не должен давать цикла");

        // Теперь добавляем цикл: SMITH продаёт то, что покупает ARMOURER.
        bought.get("ARMOURER").add("aurite_ingot");
        check(!ElisarEconomy.noCrossProfessionCycle(sold, bought),
                "кросс-профессиональный цикл должен ловиться");

        // ------------------------------------------------------------------
        // 7. Настоящие данные профессий: у каждой есть и продажи, и закупки,
        //    никто не покупает то, что сам же продаёт, и между профессиями
        //    нет кросс-циклов. Иначе торговля печатает дукаты из воздуха.
        // ------------------------------------------------------------------
        // Торговые списки берём из ElisarProfessionData — это те же данные,
        // что у живых профессий, но без Minecraft, поэтому тест собирается
        // обычным javac. Сверить, что файл не разошёлся с исходником,
        // помогает tools/rift/gen_elisar_profession_data.py --check.
        for (String prof : ElisarProfessionData.professions()) {
            check(!ElisarProfessionData.goods(prof).isEmpty(), "у профессии " + prof + " должны быть продажи");
            check(!ElisarProfessionData.buys(prof).isEmpty(), "у профессии " + prof + " должны быть закупки");

            java.util.List<String> sellNames = new java.util.ArrayList<>(ElisarProfessionData.goods(prof));
            for (String bought : ElisarProfessionData.buys(prof)) {
                check(!sellNames.contains(bought),
                        "профессия " + prof + " покупает то, что сама продаёт");
            }
        }

        java.util.Map<String, java.util.List<String>> realSold = new java.util.LinkedHashMap<>();
        java.util.Map<String, java.util.List<String>> realBought = new java.util.LinkedHashMap<>();
        for (String prof : ElisarProfessionData.professions()) {
            realSold.put(prof, new java.util.ArrayList<>(ElisarProfessionData.goods(prof)));
            realBought.put(prof, new java.util.ArrayList<>(ElisarProfessionData.buys(prof)));
        }
        // noCrossProfessionCycle возвращает true, когда цикла НЕТ.
        check(ElisarEconomy.noCrossProfessionCycle(realSold, realBought),
                "в настоящих профессиях не должно быть кросс-профессиональных циклов");

        System.out.println("OK: маржа торговцев, потолки цен, монетный курс, "
                + "отсутствие циклов и " + ElisarProfessionData.professions().size() + " профессий");
    }
}

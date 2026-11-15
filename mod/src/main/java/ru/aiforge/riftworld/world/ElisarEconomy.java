package ru.aiforge.riftworld.world;

/**
 * v0.54.0 — «Экономика Элисара»: цены, которые не обрушивают город.
 *
 * <p>Класс намеренно <b>не зависит от Minecraft</b>: только чистая арифметика.
 * Так его можно прогнать обычным юнит-тестом в CI (javac + java), без Forge,
 * и цены нельзя случайно сломать правкой рецептов.</p>
 *
 * <h2>Три правила, которые держат экономику</h2>
 * <ol>
 *   <li><b>Никогда не продавать и покупать один и тот же товар.</b> Если торговец
 *       покупает слиток за 4 дуката и тут же продаёт его за 4 — игрок крутит
 *       цикл в ноль, но город теряет товар и не получает прибыли; если цены
 *       разойдутся не в ту сторону — игрок вообще делает дукаты из воздуха.
 *       Поэтому «покупаемые» товары принципиально не пересекаются с
 *       «продаваемыми».</li>
 *   <li><b>Цена закупки всегда ниже цены продажи.</b> Торговец покупает дешевле,
 *       чем продаёт (маржа), иначе он не зарабатывает, а разоряется.</li>
 *   <li><b>Цены двигаются по объёму торговли.</b> Если товар много скупают —
 *       цена закупки падает (товара и так много), если много продают — цена
 *       продажи растёт (товар дефицитен). Это удерживает баланс без ручного
 *       «респауна» цен.</li>
 * </ol>
 *
 * <h2>Откуда берутся дукаты</h2>
 * <p>Монеты выплавляются из руды в два шага: {@code ducat_ore} → {@code aurite_ingot}
 * → {@code elisar_ducat}. Денег в мире ровно столько, сколько игрок добыл
 * и переплавил: ни копейки из воздуха. Торговля лишь <b>перераспределяет</b> их
 * между игроком и городом, но не увеличивает их число — значит, обесценивание
 * монеты невозможно по построению.</p>
 */
public final class ElisarEconomy {

    private ElisarEconomy() {
    }

    // =====================================================================================
    // Базовые цены. Число — сколько дукатов.
    // =====================================================================================

    /** Монета — базовая единица; все цены считаются в ней. */
    public static final int DUCAT = 1;

    /**
     * Слиток монетного серебра: 1 слиток → 1 монета по рецепту
     * {@code aurite_ingot_to_ducat}.
     *
     * <p>Ровно столько же платит за слиток и Банкир, поэтому арбитраж
     * «купить слиток у торговца → переплавить» невозможен в обе стороны.</p>
     */
    public static final int INGOT_WORTH = 1;

    /** Ауритовый слиток — металл брони и инструментов; торговец берёт его дёшево. */
    public static final int AURITE_INGOT_BASE = 6;

    /** Рифт-кристалл — редкий; за него дают много. */
    public static final int RIFTCRYSTAL_BASE = 24;

    /** Кирпич/блок — строительный материал дешёвый. */
    public static final int BLOCK_BASE = 1;

    /** Еда — базовая цена за штуку. */
    public static final int FOOD_BASE = 2;

    /** Инструмент — дорогой товар. */
    public static final int TOOL_BASE = 14;

    /** Оружие — самое дорогое, что житель может купить. */
    public static final int WEAPON_BASE = 22;

    /** Редкость — множитель базовой цены. */
    public static final int RARITY_COMMON = 1;
    public static final int RARITY_UNCOMMON = 2;
    public static final int RARITY_RARE = 4;

    // =====================================================================================
    // Диапазоны, в которых плавают цены.
    // =====================================================================================

    /** Минимальная цена — ниже не опускаем, иначе товар обесценивается в ноль. */
    public static final int MIN_PRICE = 1;

    /**
     * Максимальная цена — выше не поднимаем.
     *
     * <p>Это и есть «потолок падения экономики»: даже если игрок выкупил весь
     * рынок, товар не подорожает выше потолка, а значит монеты не обесценятся
     * и город не сможет выкачать у игрока последнее.</p>
     */
    public static final int MAX_PRICE_MULTIPLIER = 4;

    /** Шаг изменения цены за одну сделку. */
    private static final float STEP = 0.08F;

    /**
     * Базовая цена закупки: сколько торговец платит игроку за 1 единицу товара.
     *
     * @param rarity множитель редкости
     * @param base   базовая цена категории
     */
    public static int buyPrice(int base, int rarity) {
        return Math.max(MIN_PRICE, (base * rarity) / 4);
    }

    /**
     * Базовая цена продажи: сколько торговец берёт с игрока за 1 единицу товара.
     *
     * <p>Всегда строго выше {@link #buyPrice} — это маржа торговца. Без маржи
     * торговец работает в минус и город разоряется.</p>
     */
    public static int sellPrice(int base, int rarity) {
        return Math.max(MIN_PRICE + 1, (base * rarity) / 2);
    }

    // =====================================================================================
    // Плавающие цены.
    // =====================================================================================

    /**
     * Множитель цены, зависящий от накопленного объёма торгов.
     *
     * <p>Хранится в NBT жителя как число с плавающей точкой. Товар, который
     * скупают, дешевеет (покупателей много, товара много); товар, который
     * продают, дорожает (предложение упало). Множитель зажат в
     * {@code [1/MAX, MAX]}.</p>
     */
    public static float priceFactor(float stored, int tradedAmount) {
        if (tradedAmount <= 0) {
            return clamp(stored, 1.0F / MAX_PRICE_MULTIPLIER, MAX_PRICE_MULTIPLIER);
        }
        // Чем больше сделок, тем сильнее сдвиг, но с насыщением.
        float shift = STEP * (float) Math.min(6, tradedAmount);
        return clamp(stored - shift, 1.0F / MAX_PRICE_MULTIPLIER, MAX_PRICE_MULTIPLIER);
    }

    /** Обратное движение: товар продали много — цена растёт. */
    public static float sellFactor(float stored, int tradedAmount) {
        if (tradedAmount <= 0) {
            return clamp(stored, 1.0F / MAX_PRICE_MULTIPLIER, MAX_PRICE_MULTIPLIER);
        }
        float shift = STEP * (float) Math.min(6, tradedAmount);
        return clamp(stored + shift, 1.0F / MAX_PRICE_MULTIPLIER, MAX_PRICE_MULTIPLIER);
    }

    private static float clamp(float v, float lo, float hi) {
        if (v < lo) {
            return lo;
        }
        return v > hi ? hi : v;
    }

    /** Применить множитель к базовой цене, с округлением и защитой от нуля. */
    public static int apply(float factor, int basePrice) {
        int p = Math.round(basePrice * factor);
        return Math.max(MIN_PRICE, p);
    }

    // =====================================================================================
    // Проверка «экономического треугольника».
    // =====================================================================================

    /**
     * Проверить, что торговец не может купить у игрока товар дешевле, чем сам
     * его продаёт (иначе цикл «продать → купить → продать» печатает дукаты).
     *
     * @return {@code true}, если цены безопасны
     */
    public static boolean pricesSafe(int buy, int sell) {
        return buy > 0 && sell > 0 && buy <= sell;
    }

    /**
     * Сколько дукатов даёт слиток по фиксированному рецепту.
     *
     * <p>Совпадает с рецептом {@code aurite_ingot_to_ducat}: один слиток
     * переплавляется в одну монету, и ровно столько же платит за него
     * Банкир — поэтому арбитраж невозможен.</p>
     */
    public static int ingotWorth() {
        return INGOT_WORTH;
    }

    /**
     * Проверить, что в наборе профессий нет кросс-профессионального цикла.
     *
     * <p>Цикл — это когда торговец А продаёт товар, который торговец Б покупает,
     * и при этом закупочная цена у Б выше продажной у А. Тогда игрок может
     * «перекладывать» товар из рук в руки и получать дукаты из воздуха.</p>
     *
     * <p>Метод принимает списки названий предметов, а не сами {@code ItemStack},
     * чтобы оставаться независимым от Minecraft и тестироваться обычным
     * юнит-тестом.</p>
     *
     * @param soldBy   профессия → что она продаёт
     * @param boughtBy профессия → что она покупает
     * @return {@code true}, если циклов нет
     */
    public static boolean noCrossProfessionCycle(java.util.Map<String, java.util.List<String>> soldBy,
                                                  java.util.Map<String, java.util.List<String>> boughtBy) {
        for (java.util.Map.Entry<String, java.util.List<String>> seller : soldBy.entrySet()) {
            for (java.util.Map.Entry<String, java.util.List<String>> buyer : boughtBy.entrySet()) {
                if (seller.getKey().equals(buyer.getKey())) {
                    continue;
                }
                for (String item : seller.getValue()) {
                    if (buyer.getValue().contains(item)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}

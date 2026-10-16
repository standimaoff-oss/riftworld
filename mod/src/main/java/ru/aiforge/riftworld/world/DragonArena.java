package ru.aiforge.riftworld.world;

/**
 * Морская арена бумажного дракона-оригами (rev 7b, план рев4: «фото 01 = drowned-spire sea arena
 * для будущего оригами-дракона»). Одна детерминированная точка в море Элисара: утопленный шпиль-ротонда
 * с рваными готическими арками, цепями, кованой клеткой-фонарём на постаменте, винтовой лестницей в воду
 * и кольцом моховых столбов-останцев. Дракон облетает её и ныряет под воду.
 */
public final class DragonArena {
    public static final double AX = 640.0, AZ = -720.0;
    public static final int R = 26;

    private DragonArena() {
    }

    public static boolean near(double x, double z, double d) {
        return Math.hypot(x - AX, z - AZ) < d;
    }

    /** Высота площадки арены (дек) над уровнем моря. */
    public static int deckY(int ocean) {
        return ocean + 14;
    }
}

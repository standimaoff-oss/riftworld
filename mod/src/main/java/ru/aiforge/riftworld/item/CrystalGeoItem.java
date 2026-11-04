package ru.aiforge.riftworld.item;

/** Предмет/оружие Разлома, рисуемое 3D GeoModel-моделью в {@code ModelItemRenderer}. */
public interface CrystalGeoItem {
    String geoModel();

    default String geoTexture() {
        return "rift_obelisk_boss";
    }
}

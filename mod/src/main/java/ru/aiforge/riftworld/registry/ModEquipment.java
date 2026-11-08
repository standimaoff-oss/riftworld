package ru.aiforge.riftworld.registry;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.item.RiftArmorItem;
import ru.aiforge.riftworld.item.RiftArmorMaterials;
import ru.aiforge.riftworld.item.RiftBowItem;
import ru.aiforge.riftworld.item.RiftEquipmentTiers;
import ru.aiforge.riftworld.item.RiftShieldItem;

/** v0.41.0 — two complete ore armour and weapon sets. */
public final class ModEquipment {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RiftWorld.MODID);

    public static final RegistryObject<Item> AURITE_INGOT = ITEMS.register("aurite_ingot",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> RIFTCRYSTAL_SHARD = ITEMS.register("riftcrystal_shard",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON).fireResistant()));
    public static final RegistryObject<Item> RIFTCRYSTAL_INGOT = ITEMS.register("riftcrystal_ingot",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE).fireResistant()));

    public static final RegistryObject<Item> AURITE_HELMET = armor("aurite", ArmorItem.Type.HELMET);
    public static final RegistryObject<Item> AURITE_CHESTPLATE = armor("aurite", ArmorItem.Type.CHESTPLATE);
    public static final RegistryObject<Item> AURITE_LEGGINGS = armor("aurite", ArmorItem.Type.LEGGINGS);
    public static final RegistryObject<Item> AURITE_BOOTS = armor("aurite", ArmorItem.Type.BOOTS);
    public static final RegistryObject<Item> RIFTCRYSTAL_HELMET = armor("riftcrystal", ArmorItem.Type.HELMET);
    public static final RegistryObject<Item> RIFTCRYSTAL_CHESTPLATE = armor("riftcrystal", ArmorItem.Type.CHESTPLATE);
    public static final RegistryObject<Item> RIFTCRYSTAL_LEGGINGS = armor("riftcrystal", ArmorItem.Type.LEGGINGS);
    public static final RegistryObject<Item> RIFTCRYSTAL_BOOTS = armor("riftcrystal", ArmorItem.Type.BOOTS);

    public static final RegistryObject<Item> AURITE_SWORD = sword("aurite");
    public static final RegistryObject<Item> AURITE_PICKAXE = pickaxe("aurite");
    public static final RegistryObject<Item> AURITE_WAR_AXE = axe("aurite");
    public static final RegistryObject<Item> AURITE_BOW = bow("aurite");
    public static final RegistryObject<Item> AURITE_SHIELD = shield("aurite");
    public static final RegistryObject<Item> RIFTCRYSTAL_SWORD = sword("riftcrystal");
    public static final RegistryObject<Item> RIFTCRYSTAL_PICKAXE = pickaxe("riftcrystal");
    public static final RegistryObject<Item> RIFTCRYSTAL_WAR_AXE = axe("riftcrystal");
    public static final RegistryObject<Item> RIFTCRYSTAL_BOW = bow("riftcrystal");
    public static final RegistryObject<Item> RIFTCRYSTAL_SHIELD = shield("riftcrystal");

    private static RegistryObject<Item> armor(String material, ArmorItem.Type type) {
        String id = material + "_" + type.getName();
        return ITEMS.register(id, () -> {
            RiftArmorMaterials mat = material.equals("aurite") ? RiftArmorMaterials.AURITE : RiftArmorMaterials.RIFTCRYSTAL;
            Item.Properties properties = new Item.Properties().rarity(mat == RiftArmorMaterials.AURITE ? Rarity.UNCOMMON : Rarity.RARE);
            if (mat.isFireResistant()) properties.fireResistant();
            return new RiftArmorItem(mat, type, properties);
        });
    }

    private static RegistryObject<Item> sword(String material) {
        return ITEMS.register(material + "_sword", () -> new SwordItem(tier(material), 3, -2.4F, properties(material)));
    }

    private static RegistryObject<Item> pickaxe(String material) {
        return ITEMS.register(material + "_pickaxe", () -> new PickaxeItem(tier(material), 1, -2.8F, properties(material)));
    }

    private static RegistryObject<Item> axe(String material) {
        return ITEMS.register(material + "_war_axe", () -> new AxeItem(tier(material), 6.0F, -3.1F, properties(material)));
    }

    private static RegistryObject<Item> bow(String material) {
        boolean crystal = material.equals("riftcrystal");
        return ITEMS.register(material + "_bow", () -> new RiftBowItem(
                properties(material).durability(crystal ? 1600 : 640), crystal));
    }

    private static RegistryObject<Item> shield(String material) {
        boolean crystal = material.equals("riftcrystal");
        return ITEMS.register(material + "_shield", () -> new RiftShieldItem(
                properties(material).durability(crystal ? 1100 : 520), crystal));
    }

    private static RiftEquipmentTiers tier(String material) {
        return material.equals("aurite") ? RiftEquipmentTiers.AURITE : RiftEquipmentTiers.RIFTCRYSTAL;
    }

    private static Item.Properties properties(String material) {
        Item.Properties properties = new Item.Properties().rarity(material.equals("aurite") ? Rarity.UNCOMMON : Rarity.RARE);
        if (material.equals("riftcrystal")) properties.fireResistant();
        return properties;
    }

    /** Keep the creative inventory grouped by material, then by armour slot and weapon type. */
    public static void addToCreativeTab(CreativeModeTab.Output out) {
        out.accept(AURITE_INGOT.get());
        out.accept(RIFTCRYSTAL_SHARD.get());
        out.accept(RIFTCRYSTAL_INGOT.get());
        for (String material : new String[]{"aurite", "riftcrystal"}) {
            for (String part : new String[]{"helmet", "chestplate", "leggings", "boots"}) accept(out, material + "_" + part);
            for (String tool : new String[]{"sword", "pickaxe", "war_axe", "bow", "shield"}) accept(out, material + "_" + tool);
        }
    }

    private static void accept(CreativeModeTab.Output out, String id) {
        out.accept(ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation(RiftWorld.MODID, id)));
    }

    private ModEquipment() {
    }
}

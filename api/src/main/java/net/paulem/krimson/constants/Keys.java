package net.paulem.krimson.constants;

import net.paulem.krimson.pdc.DataTypes;
import net.paulem.krimson.properties.DataKey;
import net.paulem.krimson.inventories.InventoryData;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public final class Keys {
    private Keys() {
        /* This utility class should not be instantiated */
    }

    public static final DataKey<Byte, Byte> CUSTOM_BLOCK = new DataKey<>("custom_block", PersistentDataType.BYTE);
    public static final DataKey<String, String> IDENTIFIER = new DataKey<>("identifier", PersistentDataType.STRING);
    public static final DataKey<String, String> DROP_IDENTIFIER = new DataKey<>("drop_identifier", PersistentDataType.STRING);
    public static final DataKey<String, String> BLOCK_INSIDE = new DataKey<>("block_inside", PersistentDataType.STRING);
    public static final DataKey<byte[], ItemStack> DISPLAYED_ITEM = new DataKey<>("displayed_item", DataTypes.ITEM_STACK);

    // Inventory
    public static final DataKey<Integer, Integer> INVENTORY_SIZE = new DataKey<>("inventory_size", PersistentDataType.INTEGER);
    public static final DataKey<String, String> INVENTORY_TITLE = new DataKey<>("inventory_title", PersistentDataType.STRING);
    public static final DataKey<byte[], InventoryData> INVENTORY_DATA = new DataKey<>("inventory_data", DataTypes.INVENTORY_DATA);

    // Light
    public static final DataKey<Integer, Integer> EMITTING_LIGHT_LEVEL = new DataKey<>("emitting_light_level", PersistentDataType.INTEGER);

    // Note block backed custom blocks
    /** Marker telling {@code CustomBlockTypeChecker} that this block renders through a note block state. */
    public static final DataKey<Byte, Byte> NOTE_BLOCK = new DataKey<>("note_block", PersistentDataType.BYTE);

    // Vanilla note block emulation: the real note block keeps its blockstate pinned, so the state a player
    // would normally see in the world lives here instead.
    public static final DataKey<Integer, Integer> VANILLA_NOTE = new DataKey<>("vanilla_note", PersistentDataType.INTEGER);
    public static final DataKey<Byte, Byte> VANILLA_POWERED = new DataKey<>("vanilla_powered", PersistentDataType.BYTE);
}
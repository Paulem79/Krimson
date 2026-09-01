package net.paulem.krimsontest.blocks;

import net.paulem.krimson.blocks.Blocks;
import net.paulem.krimson.blocks.custom.InventoryCustomBlock;
import net.paulem.krimson.blocks.mining.MiningProperties;
import net.paulem.krimson.blocks.mining.ToolTier;
import net.paulem.krimson.blocks.noteblock.NoteBlockCustomBlock;
import net.paulem.krimson.KrimsonPlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import java.util.List;

public class PluginBlocks {
    private PluginBlocks() {
        /* This utility class should not be instantiated */
    }

    private static final String KEY_RAW_TIN = "raw_tin";
    private static final String KEY_RAW_MYTHRIL = "raw_mythril";
    private static final String KEY_RAW_ADAMANTIUM = "raw_adamantium";
    private static final String KEY_ONYX_GEM = "onyx_gem";

    private static NamespacedKey key(String key) {
        return new NamespacedKey(KrimsonPlugin.getInstance(), key);
    }

    public static final InventoryCustomBlock TEST = Blocks.register(
            "test_block",
            meta -> {
                meta.setDisplayName("§aTest Block");
                meta.setLore(List.of("§bThis is a test block", "§cfor inventory placement."));
            },
            key -> new InventoryCustomBlock(key, key, Material.OAK_WOOD, 3 * 9, "Inventaire de placement de test")
    );

    // --- Tin (weakest tier: stone pickaxe) ---

    public static final NoteBlockCustomBlock TIN_ORE = Blocks.register(
            "tin_ore",
            meta -> meta.setDisplayName("§fTin Ore"),
            key -> new NoteBlockCustomBlock(key, key(KEY_RAW_TIN))
    );

    public static final NoteBlockCustomBlock DEEPSLATE_TIN_ORE = Blocks.register(
            "deepslate_tin_ore",
            meta -> meta.setDisplayName("§fDeepslate Tin Ore"),
            key -> new NoteBlockCustomBlock(key, key(KEY_RAW_TIN))
    );

    public static final NoteBlockCustomBlock TIN_BLOCK = Blocks.register(
            "tin_block",
            meta -> meta.setDisplayName("§fBlock of Tin"),
            key -> new NoteBlockCustomBlock(key, key)
    );

    public static final NoteBlockCustomBlock RAW_TIN_BLOCK = Blocks.register(
            "raw_tin_block",
            meta -> meta.setDisplayName("§fBlock of Raw Tin"),
            key -> new NoteBlockCustomBlock(key, key)
    );

    // --- Mythril (mid tier: iron pickaxe) ---

    public static final NoteBlockCustomBlock MYTHRIL_ORE = Blocks.register(
            "mythril_ore",
            meta -> {
                meta.setDisplayName("§dMythril Ore");
                meta.setLore(List.of("§bA custom block rendered", "§bthrough a noteblock state."));
            },
            key -> new NoteBlockCustomBlock(key, key(KEY_RAW_MYTHRIL))
    );

    public static final NoteBlockCustomBlock DEEPSLATE_MYTHRIL_ORE = Blocks.register(
            "deepslate_mythril_ore",
            meta -> meta.setDisplayName("§dDeepslate Mythril Ore"),
            key -> new NoteBlockCustomBlock(key, key(KEY_RAW_MYTHRIL))
    );

    public static final NoteBlockCustomBlock MYTHRIL_BLOCK = Blocks.register(
            "mythril_block",
            meta -> meta.setDisplayName("§dBlock of Mythril"),
            key -> new NoteBlockCustomBlock(key, key)
    );

    public static final NoteBlockCustomBlock RAW_MYTHRIL_BLOCK = Blocks.register(
            "raw_mythril_block",
            meta -> meta.setDisplayName("§dBlock of Raw Mythril"),
            key -> new NoteBlockCustomBlock(key, key)
    );

    // --- Adamantium (top tier: diamond pickaxe) ---

    public static final NoteBlockCustomBlock ADAMANTIUM_ORE = Blocks.register(
            "adamantium_ore",
            meta -> meta.setDisplayName("§bAdamantium Ore"),
            key -> new NoteBlockCustomBlock(key, key(KEY_RAW_ADAMANTIUM))
    );

    public static final NoteBlockCustomBlock DEEPSLATE_ADAMANTIUM_ORE = Blocks.register(
            "deepslate_adamantium_ore",
            meta -> meta.setDisplayName("§bDeepslate Adamantium Ore"),
            key -> new NoteBlockCustomBlock(key, key(KEY_RAW_ADAMANTIUM))
    );

    public static final NoteBlockCustomBlock ADAMANTIUM_BLOCK = Blocks.register(
            "adamantium_block",
            meta -> meta.setDisplayName("§bBlock of Adamantium"),
            key -> new NoteBlockCustomBlock(key, key)
    );

    public static final NoteBlockCustomBlock RAW_ADAMANTIUM_BLOCK = Blocks.register(
            "raw_adamantium_block",
            meta -> meta.setDisplayName("§bBlock of Raw Adamantium"),
            key -> new NoteBlockCustomBlock(key, key)
    );

    // --- Onyx (top tier: diamond pickaxe, no deepslate/raw variant) ---

    public static final NoteBlockCustomBlock ONYX_ORE = Blocks.register(
            "onyx_ore",
            meta -> meta.setDisplayName("§8Onyx Ore"),
            key -> new NoteBlockCustomBlock(key, key(KEY_ONYX_GEM))
    );

    public static final NoteBlockCustomBlock ONYX_BLOCK = Blocks.register(
            "onyx_block",
            meta -> meta.setDisplayName("§8Block of Onyx"),
            key -> new NoteBlockCustomBlock(key, key)
    );

    public static void init() {
        KrimsonPlugin.getInstance().getLogger().info("Registering blocks...");

        TIN_ORE.setMiningProperties(MiningProperties.pickaxe(3f, ToolTier.STONE));
        DEEPSLATE_TIN_ORE.setMiningProperties(MiningProperties.pickaxe(4.5f, ToolTier.STONE));
        TIN_BLOCK.setMiningProperties(MiningProperties.pickaxe(5f, ToolTier.STONE));
        RAW_TIN_BLOCK.setMiningProperties(MiningProperties.pickaxe(5f, ToolTier.STONE));

        // Mined like an obsidian-ish ore: needs an iron pickaxe to drop anything.
        MYTHRIL_ORE.setMiningProperties(MiningProperties.pickaxe(5f, ToolTier.IRON));
        DEEPSLATE_MYTHRIL_ORE.setMiningProperties(MiningProperties.pickaxe(6f, ToolTier.IRON));
        MYTHRIL_BLOCK.setMiningProperties(MiningProperties.pickaxe(5f, ToolTier.IRON));
        RAW_MYTHRIL_BLOCK.setMiningProperties(MiningProperties.pickaxe(5f, ToolTier.IRON));

        ADAMANTIUM_ORE.setMiningProperties(MiningProperties.pickaxe(6f, ToolTier.DIAMOND));
        DEEPSLATE_ADAMANTIUM_ORE.setMiningProperties(MiningProperties.pickaxe(8f, ToolTier.DIAMOND));
        ADAMANTIUM_BLOCK.setMiningProperties(MiningProperties.pickaxe(6f, ToolTier.DIAMOND));
        RAW_ADAMANTIUM_BLOCK.setMiningProperties(MiningProperties.pickaxe(6f, ToolTier.DIAMOND));

        ONYX_ORE.setMiningProperties(MiningProperties.pickaxe(7f, ToolTier.DIAMOND));
        ONYX_BLOCK.setMiningProperties(MiningProperties.pickaxe(7f, ToolTier.DIAMOND));

        Blocks.REGISTRY.freeze();
    }
}

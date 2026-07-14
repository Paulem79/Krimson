package net.paulem.krimson.inventories;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.paulem.krimson.blocks.custom.InventoryCustomBlock;
import net.paulem.krimson.codec.Codecs;
import net.paulem.krimson.common.KrimsonPlugin;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public record InventoryData(Inventory inventory, String title) {
    public static final Codec<InventoryData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("world_uuid").forGetter(data -> {
                InventoryCustomBlock.InventoryCustomBlockHolder holder = (InventoryCustomBlock.InventoryCustomBlockHolder) data.inventory().getHolder();
                assert holder != null;
                return holder.worldUUID();
            }),
            Codec.INT.fieldOf("x").forGetter(data -> ((InventoryCustomBlock.InventoryCustomBlockHolder) data.inventory().getHolder()).x()),
            Codec.INT.fieldOf("y").forGetter(data -> ((InventoryCustomBlock.InventoryCustomBlockHolder) data.inventory().getHolder()).y()),
            Codec.INT.fieldOf("z").forGetter(data -> ((InventoryCustomBlock.InventoryCustomBlockHolder) data.inventory().getHolder()).z()),
            Codec.INT.fieldOf("size").forGetter(data -> data.inventory().getSize()),
            Codec.STRING.fieldOf("title").forGetter(InventoryData::title),
            Codecs.ITEM_STACK_LIST.fieldOf("items").forGetter(data -> data.inventory().getContents())
    ).apply(instance, (uuid, x, y, z, size, title, items) -> {
        InventoryCustomBlock.InventoryCustomBlockHolder holder = new InventoryCustomBlock.InventoryCustomBlockHolder(uuid, x, y, z);
        Inventory inventory = KrimsonPlugin.getInstance().getServer().createInventory(holder, size, title);
        for (int i = 0; i < items.length; i++) {
            ItemStack item = items[i];
            inventory.setItem(i, item);
        }
        return new InventoryData(inventory, title);
    }));

    public static byte[] encode(InventoryData data) {
        DataResult<JsonElement> result = CODEC.encodeStart(JsonOps.INSTANCE, data);
        JsonElement json = result.resultOrPartial(s -> KrimsonPlugin.getInstance().getLogger().severe(s))
                .orElseThrow(() -> new RuntimeException("Failed to encode inventory data"));
        String jsonString = json.toString();
        return jsonString.getBytes(StandardCharsets.UTF_8);
    }

    public static InventoryData decode(byte[] bytes) {
        if (bytes == null) throw new RuntimeException("Decompression failed");
        String jsonString = new String(bytes, StandardCharsets.UTF_8);
        JsonElement json = JsonParser.parseString(jsonString);
        DataResult<InventoryData> result = CODEC.parse(JsonOps.INSTANCE, json);
        return result.resultOrPartial(s -> KrimsonPlugin.getInstance().getLogger().severe(s))
                .orElseThrow(() -> new RuntimeException("Failed to decode inventory data"));
    }
}

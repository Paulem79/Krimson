package ovh.paulem.krimson.codec;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ovh.paulem.krimson.Krimson;
import ovh.paulem.krimson.blocks.InventoryCustomBlock;
import ovh.paulem.krimson.common.compat.serialize.ItemSerializerHandler;
import ovh.paulem.krimson.common.compat.stream.input.InputStreamHandler;
import ovh.paulem.krimson.common.compat.stream.output.OutputStreamHandler;
import ovh.paulem.krimson.inventories.InventoryData;
import ovh.paulem.krimson.utils.UuidUtils;
import ovh.paulem.krimson.compat.CompatAccess;

import java.util.UUID;

public interface Codecs {
    ZLibCodec<ItemStack> ITEM_STACK_CODEC = new ZLibCodec<>() {
        @Override
        protected OutputStreamHandler<?> createEncoder(@NotNull OutputStreamHandler<?> dataOutput, ItemStack object) throws Exception {
            if (object == null) {
                // Ensure the correct order by including empty/null items
                // Simply remove the write line if you don't want this
                dataOutput.writeInt(0);
                return dataOutput;
            }

            ItemSerializerHandler itemSerializer = CompatAccess.getHandler();
            itemSerializer.serializeAndWrite(object, dataOutput);
            return dataOutput;
        }

        @Override
        public ItemStack decode(@NotNull InputStreamHandler<?> dataInput, byte[] object) throws Exception {
            int length = dataInput.readInt();

            if(length == 0) return null;

            ItemSerializerHandler itemSerializer = CompatAccess.getHandler();
            return itemSerializer.readAndDeserialize(dataInput, length);
        }
    };

    Codec<ItemStack, byte[]> ITEM_STACK_BASE_CODEC = ITEM_STACK_CODEC.toBasicCodec();

    ZLibCodec<InventoryData> INVENTORY_DATA_CODEC = new ZLibCodec<>() {
        @Override
        protected OutputStreamHandler<?> createEncoder(@NotNull OutputStreamHandler<?> dataOutput, InventoryData object) throws Exception {
            Inventory inventory = object.inventory();

            // Write the owner of the inventory, if applicable
            InventoryCustomBlock.InventoryCustomBlockHolder holder = (InventoryCustomBlock.InventoryCustomBlockHolder) inventory.getHolder();

            assert holder != null;
            byte[] UUIDbytes = UuidUtils.asBytes(holder.getWorldUUID());
            // Write the world UUID length
            dataOutput.writeInt(UUIDbytes.length);
            // Write the world UUID
            dataOutput.write(UUIDbytes);
            // Write the inventory holder x, y, z coordinates
            dataOutput.writeInt(holder.getX());
            dataOutput.writeInt(holder.getY());
            dataOutput.writeInt(holder.getZ());

            // Write the size of the inventory
            dataOutput.writeInt(inventory.getSize());

            // Write the title of the inventory
            dataOutput.writeUTF(object.title());

            ItemStack[] items = inventory.getContents();

            dataOutput.writeInt(items.length);

            for (ItemStack item : items) {
                Codecs.ITEM_STACK_BASE_CODEC.encode(dataOutput, item);
            }

            // Serialize that array
            dataOutput.close();

            return dataOutput;
        }

        @Override
        public @NotNull InventoryData decode(@NotNull InputStreamHandler<?> dataInput, byte[] data) throws Exception {
            int uuidLength = dataInput.readInt();
            byte[] uuidBytes = new byte[uuidLength];
            dataInput.read(uuidBytes);
            UUID worldUUID = UuidUtils.asUuid(uuidBytes);

            // Read the x, y, z coordinates of the inventory holder
            int x = dataInput.readInt();
            int y = dataInput.readInt();
            int z = dataInput.readInt();

            InventoryCustomBlock.InventoryCustomBlockHolder holder = new InventoryCustomBlock.InventoryCustomBlockHolder(worldUUID, x, y, z);
            int size = dataInput.readInt();
            String title = dataInput.readUTF();
            Inventory inventory = Krimson.getInstance().getServer().createInventory(holder, size, title);

            int count = dataInput.readInt();

            for (int i = 0; i < count; i++) {
                @Nullable ItemStack stack = Codecs.ITEM_STACK_BASE_CODEC.decode(null);

                if (stack == null) {
                    // Empty item, keep entry as null
                    continue;
                }

                inventory.setItem(i, stack);
            }

            dataInput.close();
            return new InventoryData(inventory, title);
        }
    };
}

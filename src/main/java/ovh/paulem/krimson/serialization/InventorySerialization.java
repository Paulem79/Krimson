package ovh.paulem.krimson.serialization;

import org.bukkit.inventory.*;
import ovh.paulem.krimson.Krimson;
import ovh.paulem.krimson.blocks.InventoryCustomBlock;
import ovh.paulem.krimson.inventories.InventoryData;
import ovh.paulem.krimson.utils.UuidUtils;
import ovh.paulem.krimson.utils.ZLibUtils;
import ovh.paulem.krimson.versioned.Access;
import ovh.paulem.krimson.common.versioned.serialize.ItemSerializerHandler;
import ovh.paulem.krimson.common.versioned.stream.input.InputStreamHandler;
import ovh.paulem.krimson.common.versioned.stream.output.OutputStreamHandler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

public class InventorySerialization {
    /**
     * A method to serialize an inventory to Base64 string.
     *
     * <p />
     *
     * Special thanks to Comphenix in the Bukkit forums or also known
     * as aadnk on GitHub.
     *
     * <a href="https://gist.github.com/aadnk/8138186">Original Source</a>
     *
     * @param inventoryData to serialize
     * @return Base64 string of the provided inventory
     * @throws IllegalStateException
     */
    public static byte[] serialize(InventoryData inventoryData) throws IllegalStateException {
            Inventory inventory = inventoryData.inventory();
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                OutputStreamHandler<?> dataOutput = Access.getHandler(outputStream);

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
                dataOutput.writeUTF(inventoryData.title());

                ItemStack[] items = inventory.getContents();

                dataOutput.writeInt(items.length);

                for (ItemStack item : items) {
                    if (item == null) {
                        // Ensure the correct order by including empty/null items
                        // Simply remove the write line if you don't want this
                        dataOutput.writeInt(0);
                        continue;
                    }

                    ItemSerializerHandler itemSerializer = Access.getHandler();
                    itemSerializer.serializeAndWrite(item, dataOutput);
                }

                // Serialize that array
                dataOutput.close();

                // Compression avec zlib
                return ZLibUtils.compress(outputStream.toByteArray());
            } catch (Exception e) {
                throw new IllegalStateException("Unable to save item stacks.", e);
            }
    }

    /**
     *
     * A method to get an {@link Inventory} from an encoded, Base64, string.
     *
     * <p />
     *
     * Special thanks to Comphenix in the Bukkit forums or also known
     * as aadnk on GitHub.
     *
     * <a href="https://gist.github.com/aadnk/8138186">Original Source</a>
     *
     * @param data Base64 string of data containing an inventory.
     * @return Inventory created from the Base64 string.
     * @throws IOException
     */
    public static Inventory deserialize(byte[] data) throws IOException {
        byte[] decompressed = ZLibUtils.decompress(data);
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(decompressed)) {
            InputStreamHandler<?> dataInput = Access.getHandler(inputStream);


            int uuidLength = dataInput.readInt();
            byte[] uuidBytes = new byte[uuidLength];
            dataInput.read(uuidBytes);
            UUID worldUUID = UuidUtils.asUuid(uuidBytes);

            // Read the x, y, z coordinates of the inventory holder
            int x = dataInput.readInt();
            int y = dataInput.readInt();
            int z = dataInput.readInt();

            InventoryCustomBlock.InventoryCustomBlockHolder holder = new InventoryCustomBlock.InventoryCustomBlockHolder(worldUUID, x, y, z);
            Inventory inventory = Krimson.getInstance().getServer().createInventory(holder, dataInput.readInt(), dataInput.readUTF());

            int count = dataInput.readInt();

            for (int i = 0; i < count; i++) {
                int length = dataInput.readInt();
                if (length == 0) {
                    // Empty item, keep entry as null
                    continue;
                }

                ItemSerializerHandler itemSerializer = Access.getHandler();
                ItemStack stack = itemSerializer.readAndDeserialize(dataInput, length);
                inventory.setItem(i, stack);
            }

            dataInput.close();
            return inventory;
        } catch (Exception e) {
            throw new RuntimeException("Error while reading itemstack", e);
        }
    }
}

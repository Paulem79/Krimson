package net.paulem.krimson.pdc;

import net.paulem.krimson.blocks.custom.InventoryCustomBlock;
import net.paulem.krimson.common.KrimsonPlugin;
import net.paulem.krimson.inventories.InventoryData;
import net.paulem.krimson.paper.compat.serialize.PaperItemSerializer;
import net.paulem.krimson.paper.compat.stream.input.PaperInputStream;
import net.paulem.krimson.paper.compat.stream.output.PaperOutputStream;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.UUID;

public class InventoryDataType implements PersistentDataType<byte[], InventoryData> {
    @Override
    public Class<byte[]> getPrimitiveType() {
        return byte[].class;
    }

    @Override
    public Class<InventoryData> getComplexType() {
        return InventoryData.class;
    }

    @Override
    public byte[] toPrimitive(InventoryData data, PersistentDataAdapterContext context) {
        try {
            ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
            // DataOutputStream est parfait pour les types primitifs (int, String, long)
            DataOutputStream out = new DataOutputStream(byteArrayOut);

            InventoryCustomBlock.InventoryCustomBlockHolder holder =
                    (InventoryCustomBlock.InventoryCustomBlockHolder) data.inventory().getHolder();
            assert holder != null;

            // 1. Sauvegarde de l'UUID (converti en 2 longs : beaucoup plus rapide qu'une String)
            UUID uuid = holder.worldUUID();
            out.writeLong(uuid.getMostSignificantBits());
            out.writeLong(uuid.getLeastSignificantBits());

            // 2. Coordonnées
            out.writeInt(holder.x());
            out.writeInt(holder.y());
            out.writeInt(holder.z());

            // 3. Métadonnées de l'inventaire
            out.writeInt(data.inventory().getSize());
            out.writeUTF(data.title()); // writeUTF gère l'encodage des Strings nativement

            out.flush(); // On s'assure que tout est poussé dans le ByteArrayOutputStream

            // 4. Les Items (en réutilisant ta logique existante)
            PaperOutputStream paperOut = new PaperOutputStream(byteArrayOut);
            ItemStack[] items = data.inventory().getContents();

            if (items == null || items.length == 0) {
                paperOut.writeInt(0);
            } else {
                PaperItemSerializer.INSTANCE.serializeAndWrite(items, paperOut);
            }

            return byteArrayOut.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to encode InventoryData to byte array", e);
        }
    }

    @Override
    public InventoryData fromPrimitive(byte[] primitive, PersistentDataAdapterContext context) {
        try {
            ByteArrayInputStream byteArrayIn = new ByteArrayInputStream(primitive);
            DataInputStream in = new DataInputStream(byteArrayIn);

            // 1. Lecture de l'UUID
            long mostSigBits = in.readLong();
            long leastSigBits = in.readLong();
            UUID uuid = new UUID(mostSigBits, leastSigBits);

            // 2. Lecture des coordonnées
            int x = in.readInt();
            int y = in.readInt();
            int z = in.readInt();

            // 3. Lecture des métadonnées de l'inventaire
            int size = in.readInt();
            String title = in.readUTF();

            // Reconstruction du conteneur et de l'inventaire vide
            InventoryCustomBlock.InventoryCustomBlockHolder holder =
                    new InventoryCustomBlock.InventoryCustomBlockHolder(uuid, x, y, z);
            Inventory inventory = KrimsonPlugin.getInstance().getServer().createInventory(holder, size, title);

            // 4. Lecture des Items
            PaperInputStream paperIn = new PaperInputStream(byteArrayIn);
            int length = paperIn.readInt();

            if (length > 0) {
                ItemStack[] items = PaperItemSerializer.INSTANCE.readAndDeserializeList(paperIn, length);
                inventory.setContents(items);
            }

            return new InventoryData(inventory, title);

        } catch (Exception e) {
            throw new RuntimeException("Failed to decode InventoryData from byte array", e);
        }
    }
}
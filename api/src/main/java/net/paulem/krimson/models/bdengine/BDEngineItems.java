package net.paulem.krimson.models.bdengine;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.paulem.krimson.utils.nbt.SnbtCompound;
import net.paulem.krimson.utils.nbt.SnbtList;
import net.paulem.krimson.utils.nbt.SnbtTag;
import net.paulem.krimson.utils.nbt.SnbtValue;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/**
 * Construction d'{@link ItemStack} depuis le NBT d'un modèle BDEngine, en utilisant
 * uniquement l'API Bukkit.
 * <p>
 * L'ancienne implémentation passait par le codec NMS {@code ItemStack.CODEC}, ce qui
 * liait le plugin à une version précise de Minecraft. Ici on lit les champs nous-mêmes,
 * dans les deux formats rencontrés : composants 1.20.5+ ({@code components}) et
 * ancien NBT ({@code tag}).
 */
public final class BDEngineItems {
    private BDEngineItems() {}

    public static ItemStack fromNbt(SnbtCompound itemTag) {
        if (itemTag == null || itemTag.isEmpty()) return new ItemStack(Material.AIR);

        Material material = matchMaterial(itemTag.getString("id"));
        if (material == null || material == Material.AIR) return new ItemStack(Material.AIR);

        int count = readCount(itemTag);
        ItemStack stack = new ItemStack(material, Math.max(1, count));

        SnbtCompound components = itemTag.getCompound("components");
        SnbtCompound legacyTag = itemTag.getCompound("tag");

        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        applyCustomModelData(meta, components, legacyTag);
        applyItemModel(meta, components);
        applyDamage(meta, components, legacyTag);
        applySkullTextures(meta, components, legacyTag);

        stack.setItemMeta(meta);
        return stack;
    }

    private static Material matchMaterial(String id) {
        if (id == null || id.isBlank()) return null;
        return Material.matchMaterial(id.contains(":") ? id : "minecraft:" + id);
    }

    private static int readCount(SnbtCompound itemTag) {
        if (itemTag.contains("count")) return itemTag.getInt("count");
        if (itemTag.contains("Count")) return itemTag.getInt("Count");
        return 1;
    }

    private static void applyCustomModelData(ItemMeta meta, SnbtCompound components, SnbtCompound legacyTag) {
        if (legacyTag.contains("CustomModelData")) {
            meta.setCustomModelData(legacyTag.getInt("CustomModelData"));
            return;
        }

        SnbtTag cmd = components.getNamespaced("minecraft:custom_model_data");
        if (cmd instanceof SnbtValue value) {
            // Format 1.20.5 -> 1.21.3 : entier direct
            meta.setCustomModelData(value.asInt());
        } else if (cmd instanceof SnbtCompound compound) {
            // Format 1.21.4+ : { floats: [...], strings: [...], ... }
            SnbtList floats = compound.getList("floats");
            if (!floats.isEmpty()) {
                meta.setCustomModelData((int) floats.getFloat(0));
            }
        }
    }

    private static void applyItemModel(ItemMeta meta, SnbtCompound components) {
        SnbtTag itemModel = components.getNamespaced("minecraft:item_model");
        if (!(itemModel instanceof SnbtValue value)) return;

        NamespacedKey key = NamespacedKey.fromString(value.asString());
        if (key == null) return;

        try {
            meta.setItemModel(key);
        } catch (NoSuchMethodError ignored) {
            // Serveur antérieur à 1.21.2 : composant non supporté, on l'ignore
        }
    }

    private static void applyDamage(ItemMeta meta, SnbtCompound components, SnbtCompound legacyTag) {
        if (!(meta instanceof Damageable damageable)) return;

        if (components.containsNamespaced("minecraft:damage")) {
            SnbtTag damage = components.getNamespaced("minecraft:damage");
            if (damage instanceof SnbtValue value) damageable.setDamage(value.asInt());
        } else if (legacyTag.contains("Damage")) {
            damageable.setDamage(legacyTag.getInt("Damage"));
        }
    }

    private static void applySkullTextures(ItemMeta meta, SnbtCompound components, SnbtCompound legacyTag) {
        if (!(meta instanceof SkullMeta skullMeta)) return;

        String texture = readProfileTexture(components, legacyTag);
        if (texture == null || texture.isBlank()) return;

        String skinUrl = decodeSkinUrl(texture);
        if (skinUrl == null) return;

        try {
            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID(), "");
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(URL.of(java.net.URI.create(skinUrl), null));
            profile.setTextures(textures);
            skullMeta.setOwnerProfile(profile);
        } catch (Exception ignored) {
            // Skin invalide : on garde la tête par défaut
        }
    }

    /**
     * Récupère la valeur base64 des textures, que ce soit via le composant
     * {@code minecraft:profile} (1.20.5+) ou l'ancien {@code SkullOwner}.
     */
    private static String readProfileTexture(SnbtCompound components, SnbtCompound legacyTag) {
        SnbtTag profileTag = components.getNamespaced("minecraft:profile");
        if (profileTag instanceof SnbtCompound profile) {
            String value = firstPropertyValue(profile.getList("properties"));
            if (value != null) return value;
        }

        SnbtCompound skullOwner = legacyTag.getCompound("SkullOwner");
        if (!skullOwner.isEmpty()) {
            SnbtList textures = skullOwner.getCompound("Properties").getList("textures");
            for (SnbtTag tag : textures.values()) {
                if (tag instanceof SnbtCompound entry && entry.contains("Value")) {
                    return entry.getString("Value");
                }
            }
        }

        return null;
    }

    private static String firstPropertyValue(SnbtList properties) {
        for (SnbtTag tag : properties.values()) {
            if (tag instanceof SnbtCompound property && property.contains("value")) {
                return property.getString("value");
            }
        }
        return null;
    }

    private static String decodeSkinUrl(String base64) {
        try {
            String json = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonObject skin = root.getAsJsonObject("textures").getAsJsonObject("SKIN");
            return skin.get("url").getAsString();
        } catch (Exception e) {
            return null;
        }
    }
}

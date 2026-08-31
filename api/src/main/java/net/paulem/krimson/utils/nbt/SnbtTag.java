package net.paulem.krimson.utils.nbt;

/**
 * Racine de la représentation NBT interne de Krimson.
 * <p>
 * Volontairement indépendante de NMS : les classes {@code net.minecraft.nbt.*}
 * changent de signature à chaque version de Minecraft (ex: {@code TagParser#parseTag}
 * supprimé en 1.21.11), ce qui cassait le chargement des modèles BDEngine.
 */
public sealed interface SnbtTag permits SnbtCompound, SnbtList, SnbtValue {
}

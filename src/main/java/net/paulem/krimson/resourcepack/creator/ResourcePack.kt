package net.paulem.krimson.resourcepack.creator

import net.radstevee.packed.core.asset.impl.ResourceAssetResolutionStrategy
import net.radstevee.packed.core.item.definition.BasicItem
import net.radstevee.packed.core.item.definition.ItemDefinition
import net.radstevee.packed.core.key.Key
import net.radstevee.packed.core.pack.PackFormat
import net.radstevee.packed.core.pack.ResourcePack
import net.radstevee.packed.core.pack.ResourcePackBuilder.Companion.resourcePack
import net.paulem.krimson.items.CustomBlockItem
import net.paulem.krimson.items.Items
import java.io.File

fun createBlockModel(
    pack: ResourcePack,
    texture: Key,
) {
    pack.addItemModel(texture) {
        parent = "minecraft:block/cube_all"
        cubeTexture("all", texture)
    }
    pack.addItemDefinition(ItemDefinition(texture, BasicItem(texture)))
}

fun main(dataFolder: File, packFormat: Int): File {
    val zipFile = File(dataFolder, "krimson_resource_pack_v${packFormat}.zip")
    val deleted = zipFile.delete()

    if (!deleted) {
        println("No existing resource pack zip to delete.")
    }

    val tmpDir = dataFolder.resolve("tmp")
    tmpDir.deleteRecursively()
    tmpDir.mkdirs()

    val pack = resourcePack {
        meta {
            description = "§eKrimson Resource Pack"
            format = packFormat
            outputDir = tmpDir
        }

        assetResolutionStrategy = ResourceAssetResolutionStrategy(this::class.java)
    }

    for (namespacedKey in Items.REGISTRY.keys()) {
        val blockItem: CustomBlockItem = Items.REGISTRY.getOrThrow(namespacedKey) as CustomBlockItem
        val modelPath = blockItem.customBlock.itemDisplayStack.itemMeta!!.itemModel ?: continue
        createBlockModel(pack, Key(modelPath.namespace, modelPath.key))
    }

    pack.save(deleteOld = true)

    pack.createZip(zipFile)
    tmpDir.deleteRecursively()

    return zipFile
}
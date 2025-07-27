package ovh.paulem.krimson.resourcepack.creator

import net.radstevee.packed.core.asset.impl.ResourceAssetResolutionStrategy
import net.radstevee.packed.core.item.definition.BasicItem
import net.radstevee.packed.core.item.definition.ItemDefinition
import net.radstevee.packed.core.key.Key
import net.radstevee.packed.core.pack.PackFormat
import net.radstevee.packed.core.pack.ResourcePack
import net.radstevee.packed.core.pack.ResourcePackBuilder.Companion.resourcePack
import ovh.paulem.krimson.items.Items
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

fun main(dataFolder: File): File {
    val zipFile = File(dataFolder, "pack.zip")
    zipFile.delete()

    val tmpDir = dataFolder.resolve("tmp")
    tmpDir.deleteRecursively()
    tmpDir.mkdirs()

    val pack = resourcePack {
        meta {
            description = "§eServer Pack"
            format = PackFormat.LATEST
            outputDir = tmpDir
        }

        assetResolutionStrategy = ResourceAssetResolutionStrategy(this::class.java)
    }

    for (namespacedKey in Items.REGISTRY.keys()) {
        val blockItem = Items.REGISTRY.getOrThrow(namespacedKey)
        val modelPath = blockItem.itemStack.itemMeta!!.itemModel ?: continue
        createBlockModel(pack, Key(modelPath.namespace, modelPath.key))
    }

    pack.save(deleteOld = true)

    pack.createZip(zipFile)
    tmpDir.deleteRecursively()

    return zipFile
}
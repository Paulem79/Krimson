package net.paulem.krimson.resourcepack.creator

import com.google.gson.GsonBuilder
import net.paulem.krimson.items.CustomBlockItem
import net.paulem.krimson.items.Items
import net.paulem.krimson.models.bbmodel.BBModelAssets
import net.paulem.krimson.sounds.Sounds
import net.paulem.krimson.ui.UIRegistry
import net.paulem.krimson.ui.font.CustomFontUI
import net.radstevee.packed.core.asset.impl.ResourceAssetResolutionStrategy
import net.radstevee.packed.core.item.definition.BasicItem
import net.radstevee.packed.core.item.definition.ItemDefinition
import net.radstevee.packed.core.key.Key
import net.radstevee.packed.core.pack.ResourcePack
import net.radstevee.packed.core.pack.ResourcePackBuilder.Companion.resourcePack
import java.io.File

private val prettyGson = GsonBuilder().setPrettyPrinting().create()

/**
 * Écrit dans le dossier de sortie du pack :
 * - assets/krimson/textures/<baseName>.png
 * - assets/krimson/items/<modelKeySuffix>.json (un par bone)
 *
 * À appeler dans main() de ce fichier, juste APRES `pack.save(deleteOld = true)`
 * et avant `pack.createZip(zipFile)`.
 */
fun addBBModelAssets(outputDir: File, modelKey: String) {
    val assets = BBModelAssets.REGISTRY[modelKey] ?: return

    val texturesDir = File(outputDir, "assets/krimson/textures")
    texturesDir.mkdirs()
    for ((textureName, pngBytes) in assets.textures) {
        File(texturesDir, "$textureName.png").writeBytes(pngBytes)
    }

    val itemsDir = File(outputDir, "assets/krimson/items")
    itemsDir.mkdirs()
    for ((modelKeySuffix, itemModelJson) in assets.itemModels) {
        File(itemsDir, "$modelKeySuffix.json").writeText(prettyGson.toJson(itemModelJson))
    }
}

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

    // Register sounds from the Krimson API registry
    val soundList = pack.addSounds("krimson") {
        for (namespacedKey in Sounds.REGISTRY.keys()) {
            val sound = Sounds.REGISTRY.getOrThrow(namespacedKey)
            add(Key(sound.key.namespace, sound.key.key)) {
                // The packed library will resolve assets/krimson/sounds/<key>.ogg from classpath
            }
        }
    }

    // Generate custom font definitions for font-based UIs
    for (namespacedKey in UIRegistry.REGISTRY.keys()) {
        val ui = UIRegistry.REGISTRY.getOrThrow(namespacedKey)
        if (ui is CustomFontUI) {
            // Add the font definition
            // Font Key must match CustomFontUI.fontKey, which is e.g. "krimson:mana_font"
            val fontKeyComponents = ui.fontKey.split(":")
            val namespace = fontKeyComponents[0]
            val value = fontKeyComponents[1]

            pack.addFont {
                key = Key(namespace, value)
                bitmap {
                    key = Key(ui.key.namespace, "gui/" + ui.key.key + ".png")
                    height = ui.height.toDouble()
                    ascent = ui.ascent.toDouble()
                    chars = listOf(ui.backgroundCharacter)
                }
            }
        }
    }

    // Save the resource pack - this triggers hook execution and file saves (clears tmpDir first!)
    pack.save(deleteOld = true)

    // Génération des assets BBModel après save() pour éviter qu'ils ne soient supprimés
    for (modelKey in BBModelAssets.REGISTRY.keys) {
        addBBModelAssets(tmpDir, modelKey)
    }

    // Création du ZIP contenant tout ce qui a été écrit dans tmpDir
    pack.createZip(zipFile)
    tmpDir.deleteRecursively()

    return zipFile
}
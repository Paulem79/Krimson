package net.paulem.krimson.resourcepack.creator

import com.google.gson.GsonBuilder
import net.paulem.krimson.items.CustomBlockItem
import net.paulem.krimson.items.Items
import net.paulem.krimson.models.blockbench.old.BBModelAssets
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
import java.io.InputStream
import java.util.zip.ZipInputStream

private val prettyGson = GsonBuilder().setPrettyPrinting().create()

/**
 * Extrait et fusionne le contenu de `resources/pack.zip` dans le dossier cible (`tmpDir`).
 * Cherche d'abord dans les ressources du JAR (/pack.zip ou /resources/pack.zip), puis sur le disque.
 */
fun mergeBasePack(targetDir: File) {
    val stream: InputStream = object {}.javaClass.getResourceAsStream("/pack.zip")
        ?: object {}.javaClass.getResourceAsStream("/resources/pack.zip")
        ?: File("resources/pack.zip").takeIf { it.exists() }?.inputStream()
        ?: return

    ZipInputStream(stream).use { zip ->
        var entry = zip.nextEntry
        while (entry != null) {
            val targetFile = File(targetDir, entry.name)

            // Sécurité contre la vulnérabilité Zip Slip
            if (!targetFile.canonicalPath.startsWith(targetDir.canonicalPath + File.separator)) {
                zip.closeEntry()
                entry = zip.nextEntry
                continue
            }

            if (entry.isDirectory) {
                targetFile.mkdirs()
            } else {
                targetFile.parentFile.mkdirs()
                // N'écrase pas un fichier si le runtime l'a déjà généré (priorité au dynamique)
                if (!targetFile.exists()) {
                    targetFile.outputStream().use { output ->
                        zip.copyTo(output)
                    }
                }
            }
            zip.closeEntry()
            entry = zip.nextEntry
        }
    }
}

fun addBBModelAssets(outputDir: File, modelKey: String) {
    val assets = BBModelAssets.REGISTRY[modelKey] ?: return

    val texturesDir = File(outputDir, "assets/krimson/textures/block")
    texturesDir.mkdirs()
    for ((textureName, pngBytes) in assets.textures) {
        File(texturesDir, "$textureName.png").writeBytes(pngBytes)
    }

    val geometryDir = File(outputDir, "assets/krimson/models/item")
    geometryDir.mkdirs()
    val definitionsDir = File(outputDir, "assets/krimson/items")
    definitionsDir.mkdirs()

    for ((modelKeySuffix, itemModelJson) in assets.itemModels) {
        File(geometryDir, "$modelKeySuffix.json").writeText(prettyGson.toJson(itemModelJson))

        val definition = """
            {
              "model": {
                "type": "minecraft:model",
                "model": "krimson:item/$modelKeySuffix"
              }
            }
        """.trimIndent()
        File(definitionsDir, "$modelKeySuffix.json").writeText(definition)
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

    // Save the resource pack - writes dynamic files to tmpDir (clears tmpDir first!)
    pack.save(deleteOld = true)

    // 1. Extrait et fusionne le pack de base (resources/pack.zip)
    mergeBasePack(tmpDir)

    // 2. Génération des assets BBModel après save() pour éviter qu'ils ne soient supprimés
    for (modelKey in BBModelAssets.REGISTRY.keys) {
        addBBModelAssets(tmpDir, modelKey)
    }

    // Création du ZIP final contenant la fusion des deux packs
    pack.createZip(zipFile)
    tmpDir.deleteRecursively()

    return zipFile
}
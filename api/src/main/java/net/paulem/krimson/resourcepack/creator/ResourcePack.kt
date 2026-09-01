package net.paulem.krimson.resourcepack.creator

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import net.paulem.krimson.blocks.noteblock.NoteBlockState
import net.paulem.krimson.blocks.noteblock.NoteBlockStates
import net.paulem.krimson.items.CustomArmorItem
import net.paulem.krimson.items.CustomBlockItem
import net.paulem.krimson.items.CustomToolItem
import net.paulem.krimson.items.Items
import net.paulem.krimson.models.blockbench.model.BlockbenchModelAssets
import net.paulem.krimson.sounds.Sounds
import net.paulem.krimson.ui.UIRegistry
import net.paulem.krimson.ui.font.CustomFontUI
import net.radstevee.packed.core.asset.impl.ResourceAssetResolutionStrategy
import net.radstevee.packed.core.item.definition.BasicItem
import net.radstevee.packed.core.item.definition.ItemDefinition
import net.radstevee.packed.core.key.Key
import net.radstevee.packed.core.pack.ResourcePack
import net.radstevee.packed.core.pack.ResourcePackBuilder.Companion.resourcePack
import org.bukkit.NamespacedKey
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
    val assets = BlockbenchModelAssets.REGISTRY[modelKey] ?: return

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

/**
 * Écrit `assets/minecraft/blockstates/note_block.json`, la pièce maîtresse des blocs custom à base de
 * noteblocks : chaque état du noteblock (`instrument` x `note` x `powered`) y est associé à un modèle.
 * Les états alloués pointent vers le modèle du bloc custom, tous les autres restent sur le modèle vanilla
 * pour qu'un vrai noteblock continue de ressembler à un noteblock.
 *
 * À appeler **après** `pack.save(deleteOld = true)`, qui vide `outputDir`.
 */
fun writeNoteBlockStates(outputDir: File) {
    val allocations = NoteBlockStates.allocations()
    if (allocations.isEmpty()) return

    val modelByVariant = HashMap<String, String>()
    for ((key, state) in allocations) {
        modelByVariant[state.variantString()] = blockModelKey(key)
    }

    val variants = JsonObject()
    for (index in 0..NoteBlockState.maxIndex()) {
        val variant = NoteBlockState.fromIndex(index).variantString()
        val entry = JsonObject()
        entry.addProperty("model", modelByVariant[variant] ?: "minecraft:block/note_block")
        variants.add(variant, entry)
    }

    val root = JsonObject()
    root.add("variants", variants)

    val blockstatesDir = File(outputDir, "assets/minecraft/blockstates")
    blockstatesDir.mkdirs()
    File(blockstatesDir, "note_block.json").writeText(prettyGson.toJson(root))

    // Filet de sécurité : un bloc noteblock enregistré sans CustomBlockItem n'est pas passé par
    // createBlockModel, et le blockstate pointerait alors vers un modèle inexistant.
    for (key in allocations.keys) {
        ensureCubeAllModel(outputDir, key)
    }
}

/** Le modèle utilisé à la fois comme modèle de bloc et comme modèle d'objet, cf. `ItemUtils.getWithItemModel`. */
private fun blockModelKey(key: NamespacedKey) = "${key.namespace}:block/${key.key}"

private fun ensureCubeAllModel(outputDir: File, key: NamespacedKey) {
    val modelFile = File(outputDir, "assets/${key.namespace}/models/block/${key.key}.json")
    if (modelFile.exists()) return

    modelFile.parentFile.mkdirs()

    val textures = JsonObject()
    textures.addProperty("all", "${key.namespace}:block/${key.key}")

    val model = JsonObject()
    model.addProperty("parent", ParentModel.CUBE_ALL.parent)
    model.add("textures", textures)

    modelFile.writeText(prettyGson.toJson(model))
}

// TODO: Allow for multiface blocks or more? Maybe with an enum based constructor or idk, something that can do the job
fun createBlockModel(
    pack: ResourcePack,
    texture: Key,
) {
    pack.addItemModel(texture) {
        parent = ParentModel.CUBE_ALL.parent
        cubeTexture("all", texture)
    }
    pack.addItemDefinition(ItemDefinition(texture, BasicItem(texture)))
}

/**
 * Modèle plat "à la main" (parent `item/generated`, une seule couche de texture) utilisé pour les
 * [CustomToolItem] et [CustomArmorItem]. La texture PNG elle-même n'est pas générée ici : elle doit déjà
 * exister (pack de base fusionné par [mergeBasePack], ou pipeline Blockbench) à
 * `assets/<namespace>/textures/<texture.value>.png`.
 */
fun createFlatItemModel(
    pack: ResourcePack,
    texture: Key,
    parentModel: ParentModel
) {
    pack.addItemModel(texture) {
        parent = parentModel.parent
        layerTexture(0, texture)
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
        when (val item = Items.REGISTRY.getOrThrow(namespacedKey)) {
            is CustomBlockItem -> {
                val modelPath = item.customBlock.itemDisplayStack.itemMeta?.itemModel ?: continue
                createBlockModel(pack, Key(modelPath.namespace, modelPath.key))
            }
            is CustomToolItem -> createFlatItemModel(pack, Key(item.itemModel.namespace, item.itemModel.key),
                ParentModel.HANDHELD)
            is CustomArmorItem -> createFlatItemModel(pack, Key(item.itemModel.namespace, item.itemModel.key),
                ParentModel.GENERATED)
            else -> {}
        }
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
    for (modelKey in BlockbenchModelAssets.REGISTRY.keys) {
        addBBModelAssets(tmpDir, modelKey)
    }

    // 3. Blockstates du noteblock : c'est ce fichier qui fait apparaître les blocs custom
    writeNoteBlockStates(tmpDir)

    // Création du ZIP final contenant la fusion des deux packs
    pack.createZip(zipFile)
    tmpDir.deleteRecursively()

    return zipFile
}
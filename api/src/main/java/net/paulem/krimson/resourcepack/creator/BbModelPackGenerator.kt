package net.paulem.krimson.resourcepack.creator

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.paulem.krimson.KrimsonPlugin
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

object BbModelPackGenerator {

    fun generateModelAssets(outputDir: File, namespace: String, modelKey: String) {
        val resourcePath = "assets/$namespace/models/$modelKey.bbmodel"
        val inputStream = KrimsonPlugin::class.java.classLoader.getResourceAsStream(resourcePath)
            ?: run {
                KrimsonPlugin.getInstance().logger.warning("Fichier .bbmodel introuvable sur le classpath : $resourcePath")
                return
            }

        val root = JsonParser.parseReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).asJsonObject
        val resolution = root.getAsJsonObject("resolution")
        val resW = if (resolution != null && resolution.has("width")) resolution.get("width").asDouble else 16.0
        val resH = if (resolution != null && resolution.has("height")) resolution.get("height").asDouble else 16.0

        // 1. Charger la table de correspondance des textures
        val textureMap = mutableMapOf<String, String>()
        if (root.has("textures")) {
            val texturesArray = root.getAsJsonArray("textures")
            for (i in 0 until texturesArray.size()) {
                val texObj = texturesArray[i].asJsonObject
                val name = texObj.get("name").asString.replace(".png", "")
                val id = if (texObj.has("id")) texObj.get("id").asString else i.toString()

                val textureKey = "$namespace:model/$name"
                textureMap[id] = textureKey
                textureMap[i.toString()] = textureKey
            }
        }

        // 2. Parcourir les cubes (elements)
        if (!root.has("elements")) return
        val elements = root.getAsJsonArray("elements")

        for (elem in elements) {
            val cubeObj = elem.asJsonObject
            if (cubeObj.has("export") && !cubeObj.get("export").asBoolean) continue

            val uuid = cubeObj.get("uuid").asString
            val from = parseVec3(cubeObj.getAsJsonArray("from"))
            val to = parseVec3(cubeObj.getAsJsonArray("to"))

            val sizeX = to[0] - from[0]
            val sizeY = to[1] - from[1]
            val sizeZ = to[2] - from[2]

            val isBoxUv = cubeObj.has("box_uv") && cubeObj.get("box_uv").asBoolean
            val uvOffset = if (cubeObj.has("uv_offset")) parseVec2(cubeObj.getAsJsonArray("uv_offset")) else doubleArrayOf(0.0, 0.0)

            // Génération du JSON de modèle Minecraft
            val vanillaModelJson = JsonObject()
            vanillaModelJson.addProperty("parent", "minecraft:block/block")

            val texturesJson = JsonObject()
            var primaryTexture = "minecraft:block/stone"
            if (textureMap.isNotEmpty()) {
                primaryTexture = textureMap.values.first()
                var texIdx = 0
                textureMap.values.distinct().forEach { texPath ->
                    texturesJson.addProperty(texIdx.toString(), texPath)
                    texIdx++
                }
            }
            texturesJson.addProperty("particle", primaryTexture)
            vanillaModelJson.add("textures", texturesJson)

            val elementArray = JsonArray()
            val elemJson = JsonObject()
            val fromArr = JsonArray().apply { add(0); add(0); add(0) }
            val toArr = JsonArray().apply { add(sizeX); add(sizeY); add(sizeZ) }
            elemJson.add("from", fromArr)
            elemJson.add("to", toArr)

            val facesJson = JsonObject()
            val facesObj = cubeObj.getAsJsonObject("faces")

            val faceNames = listOf("north", "south", "east", "west", "up", "down")
            for (face in faceNames) {
                val faceData = if (facesObj != null && facesObj.has(face)) facesObj.getAsJsonObject(face) else null

                val uv = calculateFaceUv(face, faceData, isBoxUv, uvOffset, sizeX, sizeY, sizeZ, resW, resH)
                if (uv != null) {
                    val singleFaceJson = JsonObject()
                    val uvArr = JsonArray().apply {
                        add(uv[0])
                        add(uv[1])
                        add(uv[2])
                        add(uv[3])
                    }
                    singleFaceJson.add("uv", uvArr)

                    var texRef = "#0"
                    if (faceData != null && faceData.has("texture") && !faceData.get("texture").isJsonNull) {
                        val texVal = faceData.get("texture").asString
                        texRef = "#${texVal}"
                    }
                    singleFaceJson.addProperty("texture", texRef)

                    if (faceData != null && faceData.has("rotation")) {
                        singleFaceJson.addProperty("rotation", faceData.get("rotation").asInt)
                    }

                    facesJson.add(face, singleFaceJson)
                }
            }

            elemJson.add("faces", facesJson)
            elementArray.add(elemJson)
            vanillaModelJson.add("elements", elementArray)

            val itemKeyStr = "bb_${modelKey}_$uuid"

            // 3. Écriture directe des fichiers dans outputDir
            writeJsonFile(outputDir, "assets/$namespace/models/item/$itemKeyStr.json", vanillaModelJson)

            // Fichier de définition d'item Minecraft 1.21.4+
            val itemDefJson = JsonObject().apply {
                val modelObj = JsonObject().apply {
                    addProperty("type", "minecraft:model")
                    addProperty("model", "$namespace:item/$itemKeyStr")
                }
                add("model", modelObj)
            }
            writeJsonFile(outputDir, "assets/$namespace/items/$itemKeyStr.json", itemDefJson)
        }
    }

    private fun writeJsonFile(outputDir: File, relativePath: String, json: JsonObject) {
        val file = File(outputDir, relativePath)
        file.parentFile.mkdirs()
        file.writeText(json.toString(), StandardCharsets.UTF_8)
    }

    private fun calculateFaceUv(
        face: String,
        faceData: JsonObject?,
        isBoxUv: Boolean,
        uvOffset: DoubleArray,
        dx: Double, dy: Double, dz: Double,
        resW: Double, resH: Double
    ): DoubleArray? {
        val u = uvOffset[0]
        val v = uvOffset[1]

        val rawUv = DoubleArray(4)

        if (!isBoxUv && faceData != null && faceData.has("uv")) {
            val uvArr = faceData.getAsJsonArray("uv")
            if (uvArr.size() == 4) {
                for (i in 0..3) rawUv[i] = uvArr[i].asDouble
            } else {
                return null
            }
        } else if (isBoxUv) {
            when (face) {
                "north" -> { rawUv[0] = u + dz; rawUv[1] = v + dz; rawUv[2] = u + dz + dx; rawUv[3] = v + dz + dy }
                "south" -> { rawUv[0] = u + dz + dx + dz; rawUv[1] = v + dz; rawUv[2] = u + dz + dx + dz + dx; rawUv[3] = v + dz + dy }
                "west"  -> { rawUv[0] = u; rawUv[1] = v + dz; rawUv[2] = u + dz; rawUv[3] = v + dz + dy }
                "east"  -> { rawUv[0] = u + dz + dx; rawUv[1] = v + dz; rawUv[2] = u + dz + dx + dz; rawUv[3] = v + dz + dy }
                "up"    -> { rawUv[0] = u + dz; rawUv[1] = v; rawUv[2] = u + dz + dx; rawUv[3] = v + dz }
                "down"  -> { rawUv[0] = u + dz + dx; rawUv[1] = v; rawUv[2] = u + dz + dx + dx; rawUv[3] = v + dz }
                else    -> return null
            }
        } else {
            return null
        }

        val scaleX = 16.0 / resW
        val scaleY = 16.0 / resH

        return doubleArrayOf(
            rawUv[0] * scaleX,
            rawUv[1] * scaleY,
            rawUv[2] * scaleX,
            rawUv[3] * scaleY
        )
    }

    private fun parseVec3(arr: JsonArray): DoubleArray = doubleArrayOf(arr[0].asDouble, arr[1].asDouble, arr[2].asDouble)
    private fun parseVec2(arr: JsonArray): DoubleArray = doubleArrayOf(arr[0].asDouble, arr[1].asDouble)
}
package io.github.ephym6.plantid.data.identification.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * What the app needs from the ML team's `model/model_metadata.json` and
 * `model/labels.txt` (packaged as assets). Values are read, never hard-coded,
 * so the ML team can ship a new model or retune the decision rule without app
 * code changes.
 */
data class ModelMetadata(
    val modelName: String,
    val modelFile: String,
    val variant: String?,
    val inputSize: Int,
    val labels: List<String>,
    val otherPlantIndex: Int,
    val rule: DecisionRule,
) {
    val version: String get() = listOfNotNull(modelFile, variant).joinToString(" ")

    companion object {
        const val METADATA_ASSET = "model_metadata.json"
        const val LABELS_ASSET = "labels.txt"

        private val json = Json { ignoreUnknownKeys = true }

        /**
         * Parses and cross-checks both files.
         *
         * @throws IllegalArgumentException if they are malformed or inconsistent.
         */
        fun parse(metadataJson: String, labelsTxt: String): ModelMetadata {
            val file = try {
                json.decodeFromString<MetadataFile>(metadataJson)
            } catch (e: kotlinx.serialization.SerializationException) {
                throw IllegalArgumentException("model_metadata.json is malformed", e)
            }
            val labels = labelsTxt.lines().map { it.trim() }.filter { it.isNotEmpty() }
            val shape = file.input.shape
            require(shape.size == 4 && shape[0] == 1 && shape[1] == shape[2] && shape[3] == 3) {
                "unsupported input shape $shape (expected [1, n, n, 3])"
            }
            require(file.input.dtype == "float32" && file.input.colorOrder == "RGB") {
                "unsupported input ${file.input.dtype} ${file.input.colorOrder}"
            }
            require(file.output.shape == listOf(1, labels.size)) {
                "output shape ${file.output.shape} doesn't match ${labels.size} labels"
            }
            require(file.labels == null || file.labels == labels) { "labels.txt and metadata labels differ" }
            require(file.otherPlantIndex in labels.indices && labels[file.otherPlantIndex] == OTHER_PLANT_LABEL) {
                "other_plant_index ${file.otherPlantIndex} doesn't point at '$OTHER_PLANT_LABEL'"
            }
            return ModelMetadata(
                modelName = file.modelName,
                modelFile = file.file,
                variant = file.tfliteVariant,
                inputSize = shape[1],
                labels = labels,
                otherPlantIndex = file.otherPlantIndex,
                rule = DecisionRule(
                    temperature = file.unknownHandling.temperature,
                    threshold = file.unknownHandling.threshold,
                    otherPlantIndex = file.otherPlantIndex,
                ),
            )
        }

        const val OTHER_PLANT_LABEL = "other_plant"
    }

    @Serializable
    private data class MetadataFile(
        @SerialName("model_name") val modelName: String,
        val file: String,
        @SerialName("tflite_variant") val tfliteVariant: String? = null,
        val input: Input,
        val output: Output,
        val labels: List<String>? = null,
        @SerialName("other_plant_index") val otherPlantIndex: Int,
        @SerialName("unknown_handling") val unknownHandling: UnknownHandling,
    )

    @Serializable
    private data class Input(
        val shape: List<Int>,
        val dtype: String,
        @SerialName("color_order") val colorOrder: String,
    )

    @Serializable
    private data class Output(val shape: List<Int>)

    @Serializable
    private data class UnknownHandling(val temperature: Double, val threshold: Double)
}

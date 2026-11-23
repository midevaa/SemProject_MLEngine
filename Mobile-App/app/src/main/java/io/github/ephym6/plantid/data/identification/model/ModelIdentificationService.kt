package io.github.ephym6.plantid.data.identification.model

import android.content.Context
import io.github.ephym6.plantid.domain.identification.ClassScore
import io.github.ephym6.plantid.domain.identification.FailureReason
import io.github.ephym6.plantid.domain.identification.IdentificationOutcome
import io.github.ephym6.plantid.domain.identification.IdentificationService
import io.github.ephym6.plantid.domain.identification.ModelInfo
import io.github.ephym6.plantid.domain.plant.PlantCatalog
import io.github.ephym6.plantid.domain.plant.PlantRepository
import io.github.ephym6.plantid.domain.plant.toSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * The real on-device identifier: the ML team's PlantID model run with LiteRT
 * (TensorFlow Lite). Fulfils the [IdentificationService] contract:
 * - everything runs off the main thread, the model is loaded once (lazily);
 * - it never throws and never fakes a result: problems become `Failed`;
 * - model labels map to plants through the bundled catalog (class label = plant id);
 * - calibrated scores for every class are carried with the outcome; the UI shows
 *   the answer's and the guesses' confidence (DECISIONS D43).
 */
class ModelIdentificationService(
    context: Context,
    private val plants: PlantRepository,
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : IdentificationService {

    private val appContext = context.applicationContext
    private val mutex = Mutex()
    private var loaded: Loaded? = null

    private class Loaded(val metadata: ModelMetadata, val interpreter: Interpreter, val preprocessor: ImagePreprocessor)

    /** Read from the small metadata asset on first use; falls back to a generic name if unreadable. */
    override val modelInfo: ModelInfo by lazy {
        val metadata = runCatching { readMetadata() }.getOrNull()
        ModelInfo(
            name = metadata?.modelName ?: FALLBACK_NAME,
            version = metadata?.version,
            isDemo = false,
        )
    }

    override suspend fun identify(photo: File): IdentificationOutcome = withContext(computeDispatcher) {
        if (!photo.isFile) return@withContext failed(FailureReason.PhotoUnavailable)
        mutex.withLock {
            val model = try {
                loaded ?: load().also { loaded = it }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                return@withLock failed(FailureReason.ModelUnavailable)
            }
            try {
                val input = model.preprocessor.toInput(photo) ?: return@withLock failed(FailureReason.ProcessingFailed)
                val output = Array(1) { FloatArray(model.metadata.labels.size) }
                model.interpreter.run(input, output)
                toOutcome(model.metadata, model.metadata.rule.decide(output[0]))
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                failed(FailureReason.ProcessingFailed)
            }
        }
    }

    private suspend fun toOutcome(metadata: ModelMetadata, decision: DecisionRule.Decision): IdentificationOutcome {
        val catalog = plants.catalog().getOrNull() ?: return failed(FailureReason.ProcessingFailed)
        val scores = calibratedScores(metadata.labels, decision.calibrated)
        return when (decision) {
            is DecisionRule.Decision.Unsupported -> IdentificationOutcome.Unsupported(modelInfo)
            is DecisionRule.Decision.Uncertain -> IdentificationOutcome.Uncertain(
                model = modelInfo,
                scores = scores,
                candidates = decision.guesses.mapNotNull { catalog.summaryFor(metadata.labels[it]) },
            )
            is DecisionRule.Decision.Identified -> {
                val label = metadata.labels[decision.index]
                // A label missing from the catalog means app data and model are out of sync:
                // fail rather than show an unknown name (AI_INTEGRATION.md §2).
                val plant = catalog.summaryFor(label) ?: return failed(FailureReason.ProcessingFailed)
                IdentificationOutcome.Identified(modelInfo, label, plant, scores)
            }
        }
    }

    private fun PlantCatalog.summaryFor(label: String) = byClassLabel(label)?.toSummary()

    /** Every class's calibrated probability (after temperature), highest first. */
    private fun calibratedScores(labels: List<String>, calibrated: DoubleArray): List<ClassScore> =
        calibrated.indices.sortedByDescending { calibrated[it] }
            .map { ClassScore(labels[it], calibrated[it].toFloat()) }

    private fun load(): Loaded {
        val metadata = readMetadata()
        val interpreter = Interpreter(mapAsset(metadata.modelFile), Interpreter.Options().setNumThreads(THREADS))
        val inputShape = interpreter.getInputTensor(0).shape().toList()
        val outputShape = interpreter.getOutputTensor(0).shape().toList()
        check(inputShape == listOf(1, metadata.inputSize, metadata.inputSize, 3)) { "model input $inputShape" }
        check(outputShape == listOf(1, metadata.labels.size)) { "model output $outputShape" }
        return Loaded(metadata, interpreter, ImagePreprocessor(appContext, metadata.inputSize))
    }

    private fun readMetadata(): ModelMetadata {
        val assets = appContext.assets
        val metadata = assets.open(ModelMetadata.METADATA_ASSET).bufferedReader().use { it.readText() }
        val labels = assets.open(ModelMetadata.LABELS_ASSET).bufferedReader().use { it.readText() }
        return ModelMetadata.parse(metadata, labels)
    }

    /** Memory-maps the (uncompressed) model asset. */
    private fun mapAsset(name: String): MappedByteBuffer {
        val descriptor = try {
            appContext.assets.openFd(name)
        } catch (e: IOException) {
            throw IOException("model asset '$name' is missing or compressed", e)
        }
        return descriptor.use { fd ->
            FileInputStream(fd.fileDescriptor).use { stream ->
                stream.channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
            }
        }
    }

    private fun failed(reason: FailureReason) = IdentificationOutcome.Failed(modelInfo, reason)

    private companion object {
        const val FALLBACK_NAME = "PlantID model"
        const val THREADS = 4
    }
}

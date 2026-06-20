package website.ahdesign.vocalis.companion.offline

import android.content.Context
import android.util.Log
import website.ahdesign.vocalis.Engine
import website.ahdesign.vocalis.Turn
import java.io.File
import java.io.InputStream

/**
 * On-device (no internet) path. Two strategies, picked by what's downloaded/feasible:
 *
 *  A) OFFLINE_GEMMA — gemma-4-e4b-it (the model Google AI Edge Gallery runs). It's natively
 *     multimodal incl. AUDIO, so one model does ASR + translation + a short reply. Run via the
 *     LiteRT-LM Kotlin API (preferred) or MediaPipe LLM Inference. Needs a capable phone and a
 *     downloaded model file. gemma-4-e4b-it is pretrained on 140+ languages — hence solid Bulgarian.
 *
 *  B) OFFLINE_MLKIT — lighter fallback: on-device ASR (bundled Whisper, or SpeechRecognizer if a
 *     `bg` pack exists) -> ML Kit on-device translation (Bulgarian supported) -> a short reply.
 */
class OnDeviceEngine(
    private val context: Context,
) {
    /** True once the gemma model file has been downloaded into the app's files dir. */
    fun gemmaReady(): Boolean = modelFile().exists()

    private fun modelFile(): File = File(context.filesDir, GEMMA_MODEL_FILE)

    suspend fun process(
        pcm: InputStream,
        engine: Engine,
        onTurn: suspend (Turn) -> Unit,
    ) {
        when (engine) {
            Engine.OFFLINE_GEMMA -> processGemma(pcm, onTurn)
            Engine.OFFLINE_MLKIT -> processMlKit(pcm, onTurn)
            Engine.ONLINE -> error("OnDeviceEngine called for ONLINE")
        }
    }

    @Suppress("UnusedParameter") // onTurn is emitted by the real implementation (see Later)
    private fun processGemma(
        pcm: InputStream,
        onTurn: suspend (Turn) -> Unit,
    ) {
        // Later: chunk PCM by VAD, feed each utterance as audio to gemma-4-e4b-it with a prompt
        //   asking for {transcript, translation, short reply + phonetic} as JSON; parse via
        //   parseLlmAnswer-style logic in :core, then onTurn(...).
        Log.i(TAG, "OFFLINE_GEMMA: feed audio to $GEMMA_MODEL_FILE (avail=${pcm.available()})")
    }

    @Suppress("UnusedParameter") // onTurn is emitted by the real implementation (see Later)
    private fun processMlKit(
        pcm: InputStream,
        onTurn: suspend (Turn) -> Unit,
    ) {
        // Later: ASR (Whisper/SpeechRecognizer) -> ML Kit Translation (TranslateLanguage.BULGARIAN)
        //   -> a short templated/LLM reply -> onTurn(...).
        Log.i(TAG, "OFFLINE_MLKIT: ASR -> ML Kit translate -> short reply (avail=${pcm.available()})")
    }

    companion object {
        private const val TAG = "OnDeviceEngine"
        private const val GEMMA_MODEL_FILE = "gemma-4-e4b-it.litertlm"
    }
}

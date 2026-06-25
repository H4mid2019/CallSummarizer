package website.ahdesign.vocalis

/**
 * Shared watch <-> phone contract: Data Layer addresses, audio format, and the result types both
 * sides exchange. One definition reused by :app (companion service) and :wear (client).
 */

/** Data Layer addresses. Channel carries audio; messages carry control + results. */
object Paths {
    const val AUDIO_CHANNEL = "/vocalis/audio" // ChannelClient: watch -> phone PCM stream
    const val RESULT = "/vocalis/result" // MessageClient: phone -> watch JSON ResultPayload
    const val LISTEN_START = "/vocalis/listen/start" // MessageClient: watch -> phone
    const val LISTEN_STOP = "/vocalis/listen/stop" // MessageClient: watch -> phone
}

/** Audio format agreed by both sides (matches the phone app's existing 16 kHz mono PCM16). */
object AudioSpec {
    const val SAMPLE_RATE = 16_000
    const val CHANNELS = 1
}

/** Which engine produced a result - surfaced subtly in the UI so the user knows when offline. */
enum class Engine { ONLINE, OFFLINE_GEMMA, OFFLINE_MLKIT }

/**
 * The short, easy-to-say reply. [text] is in the user's chosen [ReplyScript]; [phonetic] is an
 * optional 1-2 word pronunciation hint for a non-native speaker.
 */
data class Suggestion(
    val text: String,
    val phonetic: String? = null,
)

/** One exchange. Persisted in history on the phone; the watch keeps only the last few. */
data class Turn(
    val heard: String, // source-language transcript
    val meaning: String, // translation into the user's language
    val reply: Suggestion, // short reply to say back
    val engine: Engine,
    val tsEpochMs: Long, // stamped on the PHONE
)

/** Wire form sent phone -> watch over [Paths.RESULT]. Kept flat/tiny for the BT link. */
data class ResultPayload(
    val heard: String,
    val meaning: String,
    val replyText: String,
    val replyPhonetic: String?,
    val engine: String, // Engine.name
    val tsEpochMs: Long,
)

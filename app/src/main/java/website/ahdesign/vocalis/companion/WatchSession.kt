package website.ahdesign.vocalis.companion

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-process signal: true while the watch is actively streaming audio to this phone. Set by
 * [WearAudioListenerService]; observed by the phone UI (MainActivity) to show a "watch is
 * listening" banner. Lives in memory only (same process as the listener service + activity).
 */
object WatchSession {
    val listening = MutableStateFlow(false)
}

package website.ahdesign.vocalis.companion.history

import android.content.Context
import android.util.Log
import website.ahdesign.vocalis.Turn

/**
 * Full history lives on the PHONE. The watch only mirrors the last few turns.
 *
 * Storage: Room (suggested). Each row = one [Turn] (heard / meaning / reply / engine / ts).
 * Optional sync: [HistorySync] POSTs new turns to the user's endpoint so a web view can show
 * cross-device history. Off by default.
 */
class HistoryStore(
    context: Context,
) {
    private val sync = HistorySync(context)

    suspend fun save(turn: Turn) {
        // Later: insert into Room.
        Log.i(TAG, "save heard='${turn.heard.take(LOG_PREVIEW)}' engine=${turn.engine}")
        runCatching { sync.maybeUpload(turn) }.onFailure { Log.w(TAG, "history sync deferred", it) }
    }

    suspend fun recent(limit: Int = DEFAULT_LIMIT): List<Turn> {
        // Later: query Room, map entities -> Turn.
        Log.v(TAG, "recent(limit=$limit)")
        return emptyList()
    }

    companion object {
        private const val TAG = "HistoryStore"
        private const val LOG_PREVIEW = 40
        private const val DEFAULT_LIMIT = 50
    }
}

/** Optional cross-device sync. No-op unless the user enables it (endpoint + flag in prefs). */
class HistorySync(
    context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val enabled: Boolean get() = prefs.getBoolean(KEY_ENABLED, false)
    private val endpoint: String? get() = prefs.getString(KEY_ENDPOINT, null)

    suspend fun maybeUpload(turn: Turn) {
        if (!enabled || endpoint.isNullOrBlank()) return
        // Later: POST JSON {heard, meaning, replyText, engine, tsEpochMs} to `endpoint` with OkHttp;
        //   mark the Room row synced on 2xx.
        Log.i(TAG, "would upload turn (heard len=${turn.heard.length}) to $endpoint")
    }

    companion object {
        private const val TAG = "HistorySync"
        private const val PREFS = "history_sync"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_ENDPOINT = "endpoint"
    }
}

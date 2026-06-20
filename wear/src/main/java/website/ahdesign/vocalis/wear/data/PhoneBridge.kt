package website.ahdesign.vocalis.wear.data

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import website.ahdesign.vocalis.Engine
import website.ahdesign.vocalis.Paths
import website.ahdesign.vocalis.Suggestion
import website.ahdesign.vocalis.Turn

/**
 * Watch-side receiver for finished results the phone sends back over [Paths.RESULT]. Exposes the
 * latest turn + a short rolling history (the phone keeps the full log).
 */
class PhoneBridge(
    context: Context,
) : MessageClient.OnMessageReceivedListener {
    private val messageClient = Wearable.getMessageClient(context)

    private val _latest = MutableStateFlow<Turn?>(null)
    val latest: StateFlow<Turn?> = _latest

    private val _recent = MutableStateFlow<List<Turn>>(emptyList())
    val recent: StateFlow<List<Turn>> = _recent

    fun start() = messageClient.addListener(this)

    fun stop() = messageClient.removeListener(this)

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != Paths.RESULT) return
        val turn =
            runCatching { parse(String(event.data)) }
                .onFailure { Log.e(TAG, "bad result payload", it) }
                .getOrNull() ?: return

        _latest.value = turn
        _recent.value = (listOf(turn) + _recent.value).take(MAX_ON_WATCH)
    }

    private fun parse(json: String): Turn {
        val o = JSONObject(json)
        return Turn(
            heard = o.getString("heard"),
            meaning = o.getString("meaning"),
            reply = Suggestion(o.getString("replyText"), o.optString("replyPhonetic").ifBlank { null }),
            engine = runCatching { Engine.valueOf(o.optString("engine")) }.getOrDefault(Engine.ONLINE),
            tsEpochMs = o.optLong("tsEpochMs"),
        )
    }

    companion object {
        private const val TAG = "PhoneBridge"
        private const val MAX_ON_WATCH = 10
    }
}

package website.ahdesign.vocalis.wear.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import website.ahdesign.vocalis.wear.audio.WearMicService
import website.ahdesign.vocalis.wear.data.PhoneBridge

/**
 * Watch entry point. One screen, one big tap target:
 *  - tap to START -> starts [WearMicService] (streams mic to phone)
 *  - tap to STOP  -> stops the service
 * Results stream back via [PhoneBridge] and render in [ListenScreen]. The RECORD_AUDIO prompt is
 * handled inside the composable (rememberLauncherForActivityResult) so the Activity stays thin.
 */
class ListenActivity : ComponentActivity() {
    private val bridge by lazy { PhoneBridge(this) }
    private var listening by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ListenScreen(
                bridge = bridge,
                listening = listening,
                hasMicPermission = ::hasMic,
                onToggle = ::toggleListening,
            )
        }
    }

    override fun onStart() {
        super.onStart()
        bridge.start()
    }

    override fun onStop() {
        bridge.stop()
        super.onStop()
    }

    private fun toggleListening() {
        listening = !listening
        val action = if (listening) WearMicService.ACTION_START else WearMicService.ACTION_STOP
        ContextCompat.startForegroundService(
            this,
            Intent(this, WearMicService::class.java).setAction(action),
        )
    }

    private fun hasMic(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
}

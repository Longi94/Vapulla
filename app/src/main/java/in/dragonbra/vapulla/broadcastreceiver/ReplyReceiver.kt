package `in`.dragonbra.vapulla.broadcastreceiver

import `in`.dragonbra.vapulla.service.SteamService
import android.app.RemoteInput
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.jetbrains.anko.startService

class ReplyReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_ID = "id"
        const val KEY_TEXT_REPLY = "key_text_reply"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val remoteInput = RemoteInput.getResultsFromIntent(intent)

        val id = intent.getLongExtra(EXTRA_ID, 0L)

        if (id == 0L) {
            return
        }

        val message = remoteInput.getCharSequence(KEY_TEXT_REPLY)

        context.startService<SteamService>(
                SteamService.EXTRA_ACTION to "reply",
                SteamService.EXTRA_ID to id,
                SteamService.EXTRA_MESSAGE to message
        )
    }
}

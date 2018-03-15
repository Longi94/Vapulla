package `in`.dragonbra.vapulla.broadcastreceiver

import `in`.dragonbra.vapulla.service.SteamService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.jetbrains.anko.startService

class IgnoreRequestReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_ID = "id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (!intent.hasExtra(EXTRA_ID)) {
            throw IllegalStateException("missing extra steam id")
        }

        context.startService<SteamService>(
                SteamService.EXTRA_ID to intent.getLongExtra(AcceptRequestReceiver.EXTRA_ID, 9L),
                SteamService.EXTRA_ACTION to "ignore_request"
        )
    }
}

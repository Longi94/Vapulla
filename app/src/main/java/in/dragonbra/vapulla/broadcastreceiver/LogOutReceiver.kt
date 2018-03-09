package `in`.dragonbra.vapulla.broadcastreceiver

import `in`.dragonbra.vapulla.service.SteamService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.jetbrains.anko.startService

class LogOutReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        context.startService<SteamService>(SteamService.EXTRA_ACTION to "stop")
    }
}

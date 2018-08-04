package `in`.dragonbra.vapulla

import `in`.dragonbra.vapulla.service.SteamService
import `in`.dragonbra.vapulla.viewmodel.VapullaViewModel.Companion.SERVICE_BOUND_ACTION
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import androidx.navigation.findNavController
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.intentFor

/**
 * The single activity that hosts all the fragment and binds to the steam service.
 */
class VapullaActivity : AppCompatActivity(), AnkoLogger {

    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName) {
            info { "disconnected from steam service" }
            (application as VapullaApplication).steamService = null
            bound = false
        }

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            info { "connected to steam service" }
            val binder = service as SteamService.SteamBinder
            (application as VapullaApplication).steamService = binder.getService()
            bound = true

            // Notify any viewmodel that binding is finished
            info { "sending broadcast" }
            LocalBroadcastManager.getInstance(this@VapullaActivity).sendBroadcast(Intent(SERVICE_BOUND_ACTION))
        }
    }

    private var bound = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Disable splash screen theme
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.vapulla_activity)
    }

    override fun onSupportNavigateUp() = findNavController(R.id.nav_host_fragment).navigateUp()

    override fun onStart() {
        super.onStart()
        bindService(intentFor<SteamService>(), connection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        if (bound) {
            unbindService(connection)
            (application as VapullaApplication).steamService = null
            bound = false
        }
    }
}

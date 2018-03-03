package `in`.dragonbra.vapulla.activity

import `in`.dragonbra.javasteam.steam.steamclient.callbacks.ConnectedCallback
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.DisconnectedCallback
import `in`.dragonbra.javasteam.util.compat.Consumer
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.service.SteamService
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.startService
import java.io.Closeable
import java.util.*

class MainActivity : AppCompatActivity(), AnkoLogger {

    private var bound = false

    private var steamService: SteamService? = null;

    private val subs: MutableList<Closeable?> = LinkedList();

    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            info("Unbound from Steam service")

            subs.forEach { it?.close() }

            bound = false
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            info("Bound to Steam service")
            val binder = service as SteamService.SteamBinder
            steamService = binder.getService()

            subs.add(steamService?.subscribe(ConnectedCallback::class.java, Consumer { runOnUiThread { onConnected() } }))
            subs.add(steamService?.subscribe(DisconnectedCallback::class.java, Consumer { runOnUiThread { onDisconnected() } }))

            if (!steamService?.isRunning()!!) {
                steamService?.connect()
            } else {
                onConnected()
            }

            bound = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        info("Starting steam service...")
        startService<SteamService>()
    }

    override fun onStart() {
        super.onStart()
        bindService(intentFor<SteamService>(), connection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        unbindService(connection)
        bound = false
    }

    fun onConnected() {
        loading_layout.visibility = View.GONE
        username.visibility = View.VISIBLE
        password.visibility = View.VISIBLE
        login.visibility = View.VISIBLE
    }

    fun onDisconnected() {
        Toast.makeText(this, "Disconnected from Steam", Toast.LENGTH_LONG).show()
    }
}

package `in`.dragonbra.vapulla.presenter

import `in`.dragonbra.javasteam.steam.steamclient.callbacks.ConnectedCallback
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.DisconnectedCallback
import `in`.dragonbra.javasteam.util.compat.Consumer
import `in`.dragonbra.vapulla.service.SteamService
import `in`.dragonbra.vapulla.view.LoginView
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import com.hannesdorfmann.mosby3.mvp.MvpBasePresenter
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.startService
import java.io.Closeable
import java.util.*

class LoginPresenter(val context: Context) : MvpBasePresenter<LoginView>(), AnkoLogger {

    private var bound = false

    private var steamService: SteamService? = null

    private val subs: MutableList<Closeable?> = LinkedList()

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

            subs.add(steamService?.subscribe(ConnectedCallback::class.java, Consumer { onConnected() }))
            subs.add(steamService?.subscribe(DisconnectedCallback::class.java, Consumer { onDisconnected() }))

            if (!steamService?.isRunning()!!) {
                steamService?.connect()
            } else {
                onConnected()
            }

            bound = true
        }
    }

    fun onStart() {
        info("Starting steam service...")
        context.startService<SteamService>()
        context.bindService(context.intentFor<SteamService>(), connection, Context.BIND_AUTO_CREATE)
    }

    fun onStop() {
        context.unbindService(connection)
        bound = false
    }

    fun onConnected() {
        ifViewAttached { it.showLoginScreen() }
    }

    fun onDisconnected() {
        ifViewAttached { it.onDisconnected() }
    }
}
package `in`.dragonbra.vapulla.presenter

import `in`.dragonbra.javasteam.steam.handlers.steamuser.SteamUser
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.DisconnectedCallback
import `in`.dragonbra.vapulla.extension.runOnBackgroundThread
import `in`.dragonbra.vapulla.service.SteamService
import `in`.dragonbra.vapulla.view.HomeView
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import com.hannesdorfmann.mosby3.mvp.MvpBasePresenter
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.intentFor
import java.io.Closeable
import java.util.*

class HomePresenter(val context: Context) : MvpBasePresenter<HomeView>(), AnkoLogger {

    private var bound = false

    private var steamService: SteamService? = null

    private val subs: MutableList<Closeable?> = LinkedList()

    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            info("Unbound from Steam service")

            subs.forEach { it?.close() }
            subs.clear()

            bound = false
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            info("Bound to Steam service")
            val binder = service as SteamService.SteamBinder
            steamService = binder.getService()

            subs.add(steamService?.subscribe<DisconnectedCallback>({ onDisconnected() }))

            bound = true
        }
    }

    private fun onDisconnected() {
        ifViewAttached {
            it.showLoginScreen()
        }
    }

    fun onStart() {
        context.bindService(context.intentFor<SteamService>(), connection, Context.BIND_AUTO_CREATE)
    }

    fun onStop() {
        context.unbindService(connection)
        bound = false
    }

    fun logOut() {
        runOnBackgroundThread { steamService?.getHandler<SteamUser>()?.logOff() }
    }
}
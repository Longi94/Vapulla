package `in`.dragonbra.vapulla.presenter

import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.steam.handlers.steamuser.LogOnDetails
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOnCallback
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.ConnectedCallback
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.DisconnectedCallback
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.service.SteamService
import `in`.dragonbra.vapulla.threading.runOnBackgroundThread
import `in`.dragonbra.vapulla.view.LoginView
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import com.hannesdorfmann.mosby3.mvp.MvpBasePresenter
import org.jetbrains.anko.*
import java.io.Closeable
import java.util.*

class LoginPresenter(val context: Context) : MvpBasePresenter<LoginView>(), AnkoLogger {

    private var bound = false

    private var steamService: SteamService? = null

    private val subs: MutableList<Closeable?> = LinkedList()

    private val logOnDetails = LogOnDetails()

    private val account = AccountManager(context)

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

            subs.add(steamService?.subscribe<ConnectedCallback>({ onConnected() }))
            subs.add(steamService?.subscribe<DisconnectedCallback>({ onDisconnected() }))
            subs.add(steamService?.subscribe<LoggedOnCallback>({ onLoggedOn(it) }))

            if (account.hasLoginKey()) {
                logOnDetails.username = account.username
                logOnDetails.password = null
                logOnDetails.loginKey = account.loginKey
                startSteamService()
            }

            bound = true
        }
    }

    fun onStart() {
        context.bindService(context.intentFor<SteamService>(), connection, Context.BIND_AUTO_CREATE)
    }

    fun onStop() {
        context.unbindService(connection)
        bound = false
    }

    private fun onConnected() {
        if (steamService?.isLoggedIn!!) {
            ifViewAttached {
                it.loginSuccess()
            }
            return
        }

        runOnBackgroundThread {
            steamService?.logOn(logOnDetails)
        }

        ifViewAttached {
            it.showLoading("Logging in...")
        }
    }

    private fun onDisconnected() {
        ifViewAttached { it.onDisconnected() }
    }

    private fun onLoggedOn(callback: LoggedOnCallback) {
        if (callback.result != EResult.OK) {
            if (callback.result == EResult.AccountLogonDenied) {
                // TODO SteamGuard
            } else {
                warn { "Failed to log in ${callback.result}" }
                ifViewAttached {
                    it.onDisconnected()
                }
                return
            }
        }

        ifViewAttached {
            account.username = logOnDetails.username
            it.loginSuccess()
        }
    }

    fun login(username: String, password: String) {
        logOnDetails.username = username
        logOnDetails.password = password
        logOnDetails.loginKey = null

        startSteamService()
    }

    private fun startSteamService() {
        info("Starting steam service...")
        context.startService<SteamService>()

        if (!steamService?.isRunning!!) {
            steamService?.connect()

            ifViewAttached {
                it.showLoading("Connecting to Steam...")
            }
        } else {
            onConnected()
        }

    }
}
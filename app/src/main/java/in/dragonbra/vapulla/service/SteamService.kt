package `in`.dragonbra.vapulla.service

import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.handlers.ClientMsgHandler
import `in`.dragonbra.javasteam.steam.handlers.steamuser.LogOnDetails
import `in`.dragonbra.javasteam.steam.handlers.steamuser.SteamUser
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOffCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOnCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoginKeyCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.UpdateMachineAuthCallback
import `in`.dragonbra.javasteam.steam.steamclient.SteamClient
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackManager
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.ICallbackMsg
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.ConnectedCallback
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.DisconnectedCallback
import `in`.dragonbra.javasteam.util.compat.Consumer
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.manager.AccountManager
import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import java.io.Closeable
import java.util.*

class SteamService : Service(), AnkoLogger {

    companion object {
        private const val ONGOING_NOTIFICATION_ID = 100
    }

    private val binder: SteamBinder = SteamBinder()

    var steamClient: SteamClient? = null

    var callbackMgr: CallbackManager? = null

    private val subscriptions: MutableList<Closeable?> = LinkedList()

    lateinit var account: AccountManager

    @Volatile
    var isRunning: Boolean = false

    @Volatile
    var isLoggedIn: Boolean = false

    override fun onCreate() {
        super.onCreate()
        info("onCreate")

        steamClient = SteamClient()
        callbackMgr = CallbackManager(steamClient)

        subscriptions.add(callbackMgr?.subscribe(DisconnectedCallback::class.java, onDisconnected))
        subscriptions.add(callbackMgr?.subscribe(ConnectedCallback::class.java, onConnected))
        subscriptions.add(callbackMgr?.subscribe(LoggedOnCallback::class.java, onLoggedOn))
        subscriptions.add(callbackMgr?.subscribe(LoggedOffCallback::class.java, onLoggedOff))
        subscriptions.add(callbackMgr?.subscribe(LoginKeyCallback::class.java, onNewLoginKey))
        subscriptions.add(callbackMgr?.subscribe(UpdateMachineAuthCallback::class.java, onUpdateMachineAuth))

        account = AccountManager(this)
    }

    override fun onBind(intent: Intent): IBinder? {
        info("onBind")
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        info("onStartCommand")

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        info("onDestroy")
        subscriptions.forEach { it?.close() }
        subscriptions.clear()
        steamClient?.disconnect()
    }

    private fun getNotification(text: String): Notification {
        val builder = NotificationCompat.Builder(this, "vapulla-service")
                .setDefaults(0)
                .setContentTitle("Vapulla")
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_chat_bubble_white_24dp)
                .setVibrate(longArrayOf(-1L))
                .setSound(null)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.priority = NotificationManager.IMPORTANCE_LOW
        } else {
            @Suppress("DEPRECATION")
            builder.priority = Notification.PRIORITY_LOW
        }
        return builder.build()
    }

    fun connect() {
        if (!isRunning) {
            Thread(steamThread, "Steam Thread").start()
            startForeground(ONGOING_NOTIFICATION_ID, getNotification("Connecting to Steam..."))
        }
    }

    fun disconnect() {
        steamClient?.disconnect()
    }

    fun logOn(details: LogOnDetails) {
        if (isLoggedIn) {
            return
        }
        details.isShouldRememberPassword = true
        if (account.hasSentryFile()) {
            details.sentryFileHash = account.sentry
        }
        getHandler<SteamUser>()?.logOn(details)
    }

    inline fun <reified T : ICallbackMsg> subscribe(crossinline callbackFunc: (T) -> Unit):
            Closeable? = callbackMgr?.subscribe(T::class.java, { callbackFunc(it) })

    private val steamThread: Runnable = Runnable {
        info("Connecting to steam...")
        isRunning = true
        steamClient?.connect()

        while (isRunning) {
            callbackMgr?.runWaitAllCallbacks(1000)
        }

        info("Steam thread stopped")
    }

    inner class SteamBinder : Binder() {
        fun getService(): SteamService = this@SteamService
    }

    inline fun <reified T : ClientMsgHandler> getHandler() = this.steamClient?.getHandler(T::class.java)

    //region Callback handlers

    private val onDisconnected: Consumer<DisconnectedCallback> = Consumer {
        info("disconnected from steam")
        stopForeground(true)
        isRunning = false
        isLoggedIn = false
    }

    private val onConnected: Consumer<ConnectedCallback> = Consumer {
        info("connected to steam")
        startForeground(ONGOING_NOTIFICATION_ID, getNotification("Connected to Steam"))
    }

    private val onLoggedOn: Consumer<LoggedOnCallback> = Consumer {
        when (it.result) {
            EResult.OK -> isLoggedIn = true
            EResult.InvalidPassword -> account.loginKey = null
            else -> {
            }
        }
    }

    private val onLoggedOff: Consumer<LoggedOffCallback> = Consumer {
        steamClient?.disconnect()
    }

    private val onNewLoginKey: Consumer<LoginKeyCallback> = Consumer {
        info { "received login key" }
        account.loginKey = it.loginKey
        account.uniqueId = it.uniqueID
    }

    private val onUpdateMachineAuth: Consumer<UpdateMachineAuthCallback> = Consumer {
        info { "received sentry file called ${it.fileName}" }
        account.sentry = it.data
    }

    //endregion
}

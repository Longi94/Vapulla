package `in`.dragonbra.vapulla.service

import `in`.dragonbra.javasteam.steam.steamclient.SteamClient
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackManager
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.ICallbackMsg
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.ConnectedCallback
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.DisconnectedCallback
import `in`.dragonbra.javasteam.util.compat.Consumer
import `in`.dragonbra.vapulla.R
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

    private var steamClient: SteamClient? = null

    private var callbackMgr: CallbackManager? = null

    private val subscriptions: MutableList<Closeable?> = LinkedList()

    @Volatile
    private var isRunning: Boolean = false

    private val onDisconnected: Consumer<DisconnectedCallback> = Consumer {
        info("disconnected from steam")
        stopForeground(true)
        isRunning = false
    }

    private val onConnected: Consumer<ConnectedCallback> = Consumer {
        info("connected to steam")
        startForeground(ONGOING_NOTIFICATION_ID, getNotification("Connected to Steam"))
    }

    override fun onCreate() {
        super.onCreate()
        info("onCreate")

        steamClient = SteamClient()
        callbackMgr = CallbackManager(steamClient)

        subscriptions.add(callbackMgr?.subscribe(DisconnectedCallback::class.java, onDisconnected))
        subscriptions.add(callbackMgr?.subscribe(ConnectedCallback::class.java, onConnected))
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

    fun <T : ICallbackMsg> subscribe(callbackType: Class<out T>, callbackFunc: Consumer<T>):
            Closeable? = callbackMgr?.subscribe(callbackType, callbackFunc)

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

    fun isRunning(): Boolean = isRunning
}

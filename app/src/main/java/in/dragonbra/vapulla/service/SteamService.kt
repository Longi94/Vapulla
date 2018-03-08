package `in`.dragonbra.vapulla.service

import `in`.dragonbra.javasteam.enums.EChatEntryType
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.handlers.ClientMsgHandler
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.FriendMsgCallback
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.FriendMsgHistoryCallback
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.FriendsListCallback
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.PersonaStatesCallback
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
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.javasteam.util.compat.Consumer
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.activity.ChatActivity
import `in`.dragonbra.vapulla.activity.HomeActivity
import `in`.dragonbra.vapulla.data.VapullaDatabase
import `in`.dragonbra.vapulla.data.entity.ChatMessage
import `in`.dragonbra.vapulla.data.entity.SteamFriend
import `in`.dragonbra.vapulla.extension.vapulla
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.util.PersonaStateBuffer
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.*
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import org.jetbrains.anko.*
import java.io.Closeable
import javax.inject.Inject

class SteamService : Service(), AnkoLogger {

    companion object {
        private const val ONGOING_NOTIFICATION_ID = 100

        private const val MAX_RETRY_COUNT = 5
    }

    private val binder: SteamBinder = SteamBinder()

    var steamClient: SteamClient? = null

    var callbackMgr: CallbackManager? = null

    private val subscriptions: MutableSet<Closeable?> = mutableSetOf()

    val disconnectedSubs: MutableSet<(DisconnectedCallback) -> Unit> = mutableSetOf()

    private val handlerThread = HandlerThread("SteamService Handler")

    private lateinit var handler: Handler

    @Inject
    lateinit var db: VapullaDatabase

    @Inject
    lateinit var account: AccountManager

    @Inject
    lateinit var notificationManager: NotificationManagerCompat

    lateinit var stateBuffer: PersonaStateBuffer

    @Volatile
    var isRunning: Boolean = false

    @Volatile
    var isLoggedIn: Boolean = false

    @Volatile
    var isActivityRunning: Boolean = false

    @Volatile
    var expectDisconnect = false

    var retryCount = 0

    /**
     * id of the friend whose chat is currently open, null if no chat open
     */
    @Volatile
    var chatFriendId: Long? = null

    override fun onCreate() {
        vapulla().graph.inject(this)
        super.onCreate()
        info("onCreate")

        handlerThread.start()
        handler = Handler(handlerThread.looper)

        stateBuffer = PersonaStateBuffer(db.steamFriendDao())
        steamClient = SteamClient()
        callbackMgr = CallbackManager(steamClient)

        subscriptions.add(callbackMgr?.subscribe(DisconnectedCallback::class.java, onDisconnected))
        subscriptions.add(callbackMgr?.subscribe(ConnectedCallback::class.java, onConnected))
        subscriptions.add(callbackMgr?.subscribe(LoggedOnCallback::class.java, onLoggedOn))
        subscriptions.add(callbackMgr?.subscribe(LoggedOffCallback::class.java, onLoggedOff))
        subscriptions.add(callbackMgr?.subscribe(LoginKeyCallback::class.java, onNewLoginKey))
        subscriptions.add(callbackMgr?.subscribe(UpdateMachineAuthCallback::class.java, onUpdateMachineAuth))
        subscriptions.add(callbackMgr?.subscribe(PersonaStatesCallback::class.java, onPersonaState))
        subscriptions.add(callbackMgr?.subscribe(FriendsListCallback::class.java, onFriendsList))
        subscriptions.add(callbackMgr?.subscribe(FriendMsgHistoryCallback::class.java, onFriendMsgHistory))
        subscriptions.add(callbackMgr?.subscribe(FriendMsgCallback::class.java, onFriendMsg))
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
        handlerThread.quit()
    }

    private fun setNotification(text: String) {
        val builder = NotificationCompat.Builder(this, "vapulla-service")
                .setDefaults(0)
                .setContentTitle("Vapulla")
                .setContentText(text)
                .setContentIntent(PendingIntent.getActivity(this, 0, intentFor<HomeActivity>(), 0))
                .setSmallIcon(R.drawable.ic_chat_bubble_white_24dp)
                .setVibrate(longArrayOf(-1L))
                .setSound(null)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.priority = NotificationManager.IMPORTANCE_LOW
        } else {
            @Suppress("DEPRECATION")
            builder.priority = Notification.PRIORITY_LOW
        }

        startForeground(ONGOING_NOTIFICATION_ID, builder.build())
    }

    fun connect() {
        if (!isRunning) {
            expectDisconnect = false
            retryCount = 0
            stateBuffer.start()
            Thread(steamThread, "Steam Thread").start()
            setNotification("Connecting to Steam...")
        }
    }

    fun disconnect() {
        expectDisconnect = true
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

    private fun postMessageNotification(friendId: SteamID, message: String) {
        val intent = intentFor<ChatActivity>(ChatActivity.INTENT_STEAM_ID to friendId.convertToUInt64())
                .newTask().clearTask()
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
        val friend = db.steamFriendDao().find(friendId.convertToUInt64())
        val notification = NotificationCompat.Builder(this, "vapulla-message")
                .setDefaults(Notification.DEFAULT_SOUND or Notification.DEFAULT_VIBRATE)
                .setContentTitle(friend?.name)
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_chat_bubble_white_24dp)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(Notification.PRIORITY_HIGH)
                .build()

        notificationManager.notify(friendId.convertToUInt64().toInt(), notification)

        if (isActivityRunning) {
            // TODO delayed notification remove?
            //removeNotifications()
        }
    }

    fun removeNotifications() {
        notificationManager.cancelAll()
    }

    inline fun <reified T : ICallbackMsg> subscribe(noinline callbackFunc: (T) -> Unit): Closeable? =
            when (T::class) {
                DisconnectedCallback::class -> {
                    @Suppress("UNCHECKED_CAST")
                    disconnectedSubs.add(callbackFunc as (DisconnectedCallback) -> Unit)
                    Closeable {
                        disconnectedSubs.remove(callbackFunc)
                    }
                }
                else -> callbackMgr?.subscribe(T::class.java, { callbackFunc(it) })
            }

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

    private val onDisconnected: Consumer<DisconnectedCallback> = Consumer { cb ->
        if (expectDisconnect || retryCount >= MAX_RETRY_COUNT) {
            info("disconnected from steam")
            stopForeground(true)
            isRunning = false
            isLoggedIn = false
            expectDisconnect = false
            stateBuffer.stop()
            disconnectedSubs.forEach { it.invoke(cb) }
        } else {
            info("failed to connect to steam ${++retryCount} times, trying again...")
            handler.postDelayed({ steamClient?.connect() }, 1000L)
            setNotification("Lost connection to Steam, reconnecting...")
        }
    }

    private val onConnected: Consumer<ConnectedCallback> = Consumer {
        info("connected to steam")
        retryCount = 0
        setNotification("Connected to Steam")
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

    private val onPersonaState: Consumer<PersonaStatesCallback> = Consumer {
        it.personaStates.forEach {
            if (!it.friendID.isIndividualAccount) {
                return@forEach
            }

            if (it.friendID == steamClient?.steamID) {
                account.saveLocalUser(it)
                return@forEach
            }

            info("${it.state} - ${it.name} - ${it.lastLogOff.time} - ${it.lastLogOn.time}")

            stateBuffer.push(it)
        }
    }

    private val onFriendsList: Consumer<FriendsListCallback> = Consumer {
        val dao = db.steamFriendDao()
        it.friendList.forEach {
            if (!it.steamID.isIndividualAccount) {
                return@forEach
            }

            var friend = dao.find(it.steamID.convertToUInt64())

            if (friend == null) {
                friend = SteamFriend(it.steamID.convertToUInt64())
                friend.relation = it.relationship.code()
                dao.insert(friend)
            } else {
                friend.relation = it.relationship.code()
                dao.update(friend)
            }

        }
    }

    private val onFriendMsgHistory: Consumer<FriendMsgHistoryCallback> = Consumer { cb ->
        cb.messages.forEach {
            val fromLocal = cb.steamID != it.steamID
            val friendId = cb.steamID.convertToUInt64()
            val timestamp = it.timestamp.time
            val confirmedMessage = db.chatMessageDao().find(it.message, timestamp, friendId, fromLocal, true)

            if (confirmedMessage != null) {
                return@forEach
            }

            val unconfirmedMessages = db.chatMessageDao().find(it.message, friendId, fromLocal, false)
                    .sortedWith(kotlin.Comparator { o1, o2 ->
                        (Math.abs(timestamp - o1.timestamp) - Math.abs(timestamp - o2.timestamp)).toInt()
                    })

            if (unconfirmedMessages.isNotEmpty()) {
                unconfirmedMessages[0].timestamp = timestamp
                unconfirmedMessages[0].timestampConfirmed = true

                db.chatMessageDao().update(unconfirmedMessages[0])
            } else {
                db.chatMessageDao().insert(ChatMessage(
                        it.message,
                        timestamp,
                        friendId,
                        fromLocal,
                        !it.isUnread,
                        true
                ))
            }
        }
    }

    private val onFriendMsg: Consumer<FriendMsgCallback> = Consumer {
        when (it.entryType) {
            EChatEntryType.ChatMsg -> {
                db.chatMessageDao().insert(ChatMessage(
                        it.message,
                        System.currentTimeMillis(),
                        it.sender.convertToUInt64(),
                        it.sender == steamClient?.steamID,
                        true,
                        false
                ))

                if (it.sender.convertToUInt64() != chatFriendId) {
                    postMessageNotification(it.sender, it.message)
                }
            }
            else -> {
            }
        }
    }

    //endregion
}

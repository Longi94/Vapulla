package `in`.dragonbra.vapulla.service

import `in`.dragonbra.javasteam.enums.EChatEntryType
import `in`.dragonbra.javasteam.enums.EFriendRelationship
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.handlers.ClientMsgHandler
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.PersonaState
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.SteamFriends
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.*
import `in`.dragonbra.javasteam.steam.handlers.steamnotifications.SteamNotifications
import `in`.dragonbra.javasteam.steam.handlers.steamnotifications.callback.OfflineMessageNotificationCallback
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
import `in`.dragonbra.vapulla.broadcastreceiver.*
import `in`.dragonbra.vapulla.broadcastreceiver.ReplyReceiver.Companion.KEY_TEXT_REPLY
import `in`.dragonbra.vapulla.data.VapullaDatabase
import `in`.dragonbra.vapulla.data.entity.ChatMessage
import `in`.dragonbra.vapulla.data.entity.SteamFriend
import `in`.dragonbra.vapulla.extension.vapulla
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.threading.runOnBackgroundThread
import `in`.dragonbra.vapulla.util.PersonaStateBuffer
import `in`.dragonbra.vapulla.util.Utils
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.os.*
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationCompat.MessagingStyle.Message
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.app.RemoteInput
import com.bumptech.glide.Glide
import org.jetbrains.anko.*
import org.spongycastle.util.encoders.Hex
import java.io.Closeable
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SteamService : Service(), AnkoLogger {

    companion object {
        private const val ONGOING_NOTIFICATION_ID = 100
        private const val MAX_RETRY_COUNT = 5

        const val EXTRA_ACTION = "action"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_ID = "id"
    }

    private val binder: SteamBinder = SteamBinder()

    var steamClient: SteamClient? = null

    var callbackMgr: CallbackManager? = null

    private val subscriptions: MutableSet<Closeable?> = mutableSetOf()

    private val newMessages: MutableMap<SteamID, MutableList<Message>> = mutableMapOf()

    val disconnectedSubs: MutableSet<(DisconnectedCallback) -> Unit> = mutableSetOf()

    private val requestsToNotify: MutableSet<SteamID> =  mutableSetOf()

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
    private var chatFriendId: Long? = null

    lateinit var remoteInput: RemoteInput

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
        subscriptions.add(callbackMgr?.subscribe(NicknameListCallback::class.java, onNicknameList))
        subscriptions.add(callbackMgr?.subscribe(OfflineMessageNotificationCallback::class.java, onOfflineMessageNotification))

        remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
                .setLabel("Reply")
                .build()
    }

    override fun onBind(intent: Intent): IBinder? {
        info("onBind")
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        info("onStartCommand")

        if (isRunning && intent != null && intent.hasExtra(EXTRA_ACTION)) {
            val id = SteamID(intent.getLongExtra(EXTRA_ID, 0L))
            val action = intent.getStringExtra(EXTRA_ACTION)

            info("Received $action action message")

            when (intent.getStringExtra(EXTRA_ACTION)) {
                "reply" -> {
                    val message = intent.getStringExtra(EXTRA_MESSAGE)

                    runOnBackgroundThread {
                        db.chatMessageDao().insert(ChatMessage(
                                message,
                                System.currentTimeMillis(),
                                id.convertToUInt64(),
                                true,
                                true,
                                false
                        ))

                        getHandler<SteamFriends>()?.sendChatMessage(id, EChatEntryType.ChatMsg, message)
                    }

                    notificationManager.cancel(id.convertToUInt64().toInt())
                }
                "stop" -> {
                    stopSelf()
                }
                "accept_request" -> {
                    runOnBackgroundThread {
                        getHandler<SteamFriends>()?.addFriend(id)
                    }
                    notificationManager.cancel(id.convertToUInt64().toInt())
                }
                "ignore_request" -> {
                    runOnBackgroundThread {
                        getHandler<SteamFriends>()?.removeFriend(id)
                    }
                    notificationManager.cancel(id.convertToUInt64().toInt())
                }
                "block_request" -> {
                    runOnBackgroundThread {
                        getHandler<SteamFriends>()?.ignoreFriend(id)
                    }
                    notificationManager.cancel(id.convertToUInt64().toInt())
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        info("onDestroy")
        disconnect()
        handlerThread.quit()
    }

    private fun setNotification(text: String) {
        val logOutIntent = intentFor<LogOutReceiver>()
        val pendingIntent = PendingIntent.getBroadcast(applicationContext, 0, logOutIntent, 0)

        val builder = NotificationCompat.Builder(this, "vapulla-service")
                .setDefaults(0)
                .setShowWhen(false)
                .setContentTitle("Vapulla")
                .setContentText(text)
                .setContentIntent(PendingIntent.getActivity(this, 0, intentFor<HomeActivity>(), 0))
                .setSmallIcon(R.drawable.ic_message)
                .setVibrate(longArrayOf(-1L))
                .setSound(null)
                .addAction(R.drawable.ic_exit_to_app, "LOG OUT", pendingIntent)

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
        val friend = db.steamFriendDao().find(friendId.convertToUInt64()) ?: return

        if (!newMessages.containsKey(friendId)) {
            newMessages[friendId] = LinkedList()
        }

        val newMessage = Message(message, System.currentTimeMillis(), friend.name)
        newMessages[friendId]?.add(newMessage)

        val style = NotificationCompat.MessagingStyle(friend.name ?: "")

        newMessages[friendId]?.forEach {
            style.addMessage(it)
        }

        var bitmap: Bitmap? = null

        try {
            bitmap = Glide.with(applicationContext)
                    .asBitmap()
                    .load(Utils.getAvatarUrl(friend.avatar))
                    .apply(Utils.avatarOptions)
                    .submit()
                    .get(5, TimeUnit.SECONDS)
        } catch (ignored: Exception) {
        }

        val replyPendingIntent = PendingIntent.getBroadcast(
                applicationContext,
                friendId.convertToUInt64().toInt(),
                getMessageReplyIntent(friendId.convertToUInt64()),
                PendingIntent.FLAG_UPDATE_CURRENT
        )

        val replyAction = NotificationCompat.Action.Builder(
                R.drawable.ic_send,
                "REPLY",
                replyPendingIntent
        ).addRemoteInput(remoteInput).build()

        val intent = intentFor<ChatActivity>(ChatActivity.INTENT_STEAM_ID to friendId.convertToUInt64())
                .newTask().clearTask()
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
        val notification = NotificationCompat.Builder(this, "vapulla-message")
                .setDefaults(Notification.DEFAULT_SOUND or Notification.DEFAULT_VIBRATE)
                .setStyle(style)
                .setSmallIcon(R.drawable.ic_message)
                .setLargeIcon(bitmap)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(Notification.PRIORITY_HIGH)
                .addAction(replyAction)
                .build()

        notificationManager.notify(friendId.convertToUInt64().toInt(), notification)

        if (isActivityRunning) {
            // TODO delayed notification remove?
            //removeNotifications()
        }
    }

    private fun postFriendRequestNotification(state: PersonaState) {
        var bitmap: Bitmap? = null

        try {
            bitmap = Glide.with(applicationContext)
                    .asBitmap()
                    .load(Utils.getAvatarUrl(Hex.toHexString(state.avatarHash)))
                    .apply(Utils.avatarOptions)
                    .submit()
                    .get(5, TimeUnit.SECONDS)
        } catch (ignored: Exception) {
        }

        val acceptPendingIntent = PendingIntent.getBroadcast(
                applicationContext,
                state.friendID.convertToUInt64().toInt(),
                intentFor<AcceptRequestReceiver>(
                        AcceptRequestReceiver.EXTRA_ID to state.friendID.convertToUInt64()
                ),
                PendingIntent.FLAG_UPDATE_CURRENT
        )

        val ignorePendingIntent = PendingIntent.getBroadcast(
                applicationContext,
                state.friendID.convertToUInt64().toInt(),
                intentFor<IgnoreRequestReceiver>(
                        IgnoreRequestReceiver.EXTRA_ID to state.friendID.convertToUInt64()
                ),
                PendingIntent.FLAG_UPDATE_CURRENT
        )

        val blockPendingIntent = PendingIntent.getBroadcast(
                applicationContext,
                state.friendID.convertToUInt64().toInt(),
                intentFor<BlockRequestReceiver>(
                        IgnoreRequestReceiver.EXTRA_ID to state.friendID.convertToUInt64()
                ),
                PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, "vapulla-friend-request")
                .setDefaults(Notification.DEFAULT_SOUND or Notification.DEFAULT_VIBRATE)
                .setSmallIcon(R.drawable.ic_add_friend)
                .setLargeIcon(bitmap)
                .setContentText("${state.name} has added to their friends list!")
                .setContentTitle("New friend request")
                .setAutoCancel(true)
                .setPriority(Notification.PRIORITY_DEFAULT)
                .setContentIntent(PendingIntent.getActivity(this, 0, intentFor<HomeActivity>(), 0))
                .addAction(R.drawable.ic_check, "ACCEPT", acceptPendingIntent)
                .addAction(R.drawable.ic_close, "IGNORE", ignorePendingIntent)
                .addAction(R.drawable.ic_block, "BLOCK", blockPendingIntent)
                .build()

        notificationManager.notify(state.friendID.convertToUInt64().toInt(), notification)
    }

    private fun getMessageReplyIntent(id: Long): Intent =
            intentFor<ReplyReceiver>(ReplyReceiver.EXTRA_ID to id)

    fun setChatFriendId(id: SteamID) {
        chatFriendId = id.convertToUInt64()
        newMessages[id]?.clear()
        newMessages.remove(id)

        notificationManager.cancel(id.convertToUInt64().toInt())
    }

    fun removeChatFriendId() {
        chatFriendId = null
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

        if (isLoggedIn) {
            val details = LogOnDetails()
            details.username = account.username
            details.loginKey = account.loginKey
            details.isShouldRememberPassword = true
            if (account.hasSentryFile()) {
                details.sentryFileHash = account.sentry
            }
            getHandler<SteamUser>()?.logOn(details)
        }
    }

    private val onLoggedOn: Consumer<LoggedOnCallback> = Consumer {
        when (it.result) {
            EResult.OK -> {
                isLoggedIn = true
                getHandler<SteamNotifications>()?.requestOfflineMessageCount()
            }
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

        getHandler<SteamUser>()?.acceptNewLoginKey(it)
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

            if (requestsToNotify.contains(it.friendID)) {
                postFriendRequestNotification(it)
                requestsToNotify.remove(it.friendID)
            }
        }
    }

    private val onFriendsList: Consumer<FriendsListCallback> = Consumer {
        val dao = db.steamFriendDao()
        val inc = it.isIncremental
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

            if (inc && friend.relation == EFriendRelationship.RequestRecipient.code()) {
                requestsToNotify.add(it.steamID)
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
            EChatEntryType.Typing -> {
                val friend = db.steamFriendDao().find(it.sender.convertToUInt64())

                if (friend != null) {
                    friend.typingTs = System.currentTimeMillis()
                    db.steamFriendDao().update(friend)
                }
            }
            else -> {
            }
        }
    }

    private val onNicknameList: Consumer<NicknameListCallback> = Consumer {
        db.steamFriendDao().clearNicknames()

        it.nicknames.forEach {
            val friend = db.steamFriendDao().find(it.steamID.convertToUInt64())

            if (friend != null) {
                friend.nickname = it.nickname
                db.steamFriendDao().update(friend)
            }
        }
    }

    private val onOfflineMessageNotification: Consumer<OfflineMessageNotificationCallback> = Consumer {
        if (it.messageCount > 0) {
            getHandler<SteamFriends>()?.requestOfflineMessages()
        }
    }

    //endregion
}

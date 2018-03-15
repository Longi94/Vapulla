package `in`.dragonbra.vapulla.presenter

import `in`.dragonbra.javasteam.enums.EChatEntryType
import `in`.dragonbra.javasteam.enums.EClientPersonaStateFlag
import `in`.dragonbra.javasteam.enums.EFriendRelationship
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.SteamFriends
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.DisconnectedCallback
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.javasteam.util.Strings
import `in`.dragonbra.vapulla.activity.ChatActivity
import `in`.dragonbra.vapulla.adapter.FriendListItem
import `in`.dragonbra.vapulla.data.dao.ChatMessageDao
import `in`.dragonbra.vapulla.data.dao.SteamFriendDao
import `in`.dragonbra.vapulla.data.entity.ChatMessage
import `in`.dragonbra.vapulla.service.SteamService
import `in`.dragonbra.vapulla.threading.runOnBackgroundThread
import `in`.dragonbra.vapulla.view.ChatView
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.arch.paging.LivePagedListBuilder
import android.arch.paging.PagedList
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import org.jetbrains.anko.info
import org.jetbrains.anko.intentFor
import java.io.Closeable
import java.util.*

class ChatPresenter(val context: Context,
                    private val chatMessageDao: ChatMessageDao,
                    private val steamFriendsDao: SteamFriendDao,
                    val steamId: SteamID) : VapullaPresenter<ChatView>() {

    companion object {
        val REQUESTED_INFO = EClientPersonaStateFlag.code(EnumSet.of(
                EClientPersonaStateFlag.Status,
                EClientPersonaStateFlag.LastSeen,
                EClientPersonaStateFlag.PlayerName,
                EClientPersonaStateFlag.Presence
        ))

        const val UPDATE_INTERVAL = 1000L
        const val TYPING_INTERVAL = 20000L
    }

    private var bound = false

    private var lastTypingMessage = 0L

    private var steamService: SteamService? = null

    private val subs: MutableList<Closeable?> = LinkedList()

    private lateinit var chatData: LiveData<PagedList<ChatMessage>>

    private lateinit var friendData: LiveData<FriendListItem>

    private val updateHandler: Handler = Handler()

    private val chatObserver = Observer<PagedList<ChatMessage>> { list ->
        ifViewAttached { it.showChat(list) }
    }

    private val friendObserver = Observer<FriendListItem> { friend ->
        ifViewAttached { v ->
            friend?.let {
                if (it.relation == EFriendRelationship.Friend.code()) {
                    v.updateFriendData(friend)
                } else {
                    v.navigateUp()
                }
            }
        }
    }

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
            steamService?.setChatFriendId(steamId)
            steamService?.isActivityRunning = true

            getMessageHistory()
        }
    }

    override fun onStart() {
        context.bindService(context.intentFor<SteamService>(), connection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        context.unbindService(connection)
        bound = false
    }

    override fun onResume() {
        if (bound) {
            steamService?.setChatFriendId(steamId)
            steamService?.isActivityRunning = true
            getMessageHistory()
        }

        chatData = LivePagedListBuilder(chatMessageDao.findLivePaged(steamId.convertToUInt64()), 50).build()
        chatData.observe(view as ChatActivity, chatObserver)

        friendData = steamFriendsDao.findLive(steamId.convertToUInt64())
        friendData.observe(view as ChatActivity, friendObserver)

        ifViewAttached { it.showChat(chatData.value) }
        ifViewAttached { it.updateFriendData(friendData.value) }

        updateHandler.postDelayed({ updateFriend() }, UPDATE_INTERVAL)
    }

    override fun onPause() {
        if (bound) {
            steamService?.isActivityRunning = false
            steamService?.removeChatFriendId()
        }

        chatData.removeObserver(chatObserver)
        friendData.removeObserver(friendObserver)

        updateHandler.removeCallbacksAndMessages(null)
    }

    private fun updateFriend() {
        ifViewAttached { it.updateFriendData(friendData.value) }
        updateHandler.postDelayed({ updateFriend() }, UPDATE_INTERVAL)
    }

    fun getMessageHistory() {
        runOnBackgroundThread {
            steamService?.getHandler<SteamFriends>()?.requestMessageHistory(steamId)
        }
    }

    private fun onDisconnected() {
        ifViewAttached {
            it.closeApp()
        }
    }

    fun sendMessage(message: String) {
        if (Strings.isNullOrEmpty(message)) {
            return
        }
        lastTypingMessage = 0L

        runOnBackgroundThread {
            steamService?.getHandler<SteamFriends>()?.sendChatMessage(steamId, EChatEntryType.ChatMsg, message)

            chatMessageDao.insert(ChatMessage(
                    message,
                    System.currentTimeMillis(),
                    steamId.convertToUInt64(),
                    true,
                    true,
                    false
            ))
        }
    }

    fun typing() {
        if (lastTypingMessage < System.currentTimeMillis() - TYPING_INTERVAL) {
            lastTypingMessage = System.currentTimeMillis()

            runOnBackgroundThread {
                steamService?.getHandler<SteamFriends>()?.sendChatMessage(steamId, EChatEntryType.Typing, "")
            }
        }
    }

    fun removeFriend() {
        ifViewAttached { it.showRemoveFriendDialog(friendData.value?.name ?: "") }
    }

    fun confirmRemoveFriend() {
        runOnBackgroundThread {
            steamService?.getHandler<SteamFriends>()?.removeFriend(steamId)
        }
    }

    fun blockFriend() {
        ifViewAttached { it.showBlockFriendDialog(friendData.value?.name ?: "") }
    }

    fun confirmBlockFriend() {
        runOnBackgroundThread {
            steamService?.getHandler<SteamFriends>()?.ignoreFriend(steamId)
        }
    }

    fun nicknameMenuClicked() {
        ifViewAttached { it.showNicknameDialog(friendData.value?.nickname ?: "") }
    }

    fun setNickname(nickname: String) {
        runOnBackgroundThread {
            steamService?.getHandler<SteamFriends>()?.setFriendNickname(steamId, nickname)
            val friend = steamFriendsDao.find(steamId.convertToUInt64())

            if (friend != null) {
                friend.nickname = nickname
                steamFriendsDao.update(friend)
            }
        }
    }
}
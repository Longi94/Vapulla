package `in`.dragonbra.vapulla.presenter

import `in`.dragonbra.javasteam.enums.EChatEntryType
import `in`.dragonbra.javasteam.enums.EFriendRelationship
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.SteamFriends
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.javasteam.util.Strings
import `in`.dragonbra.vapulla.activity.ChatActivity
import `in`.dragonbra.vapulla.adapter.FriendListItem
import `in`.dragonbra.vapulla.data.dao.ChatMessageDao
import `in`.dragonbra.vapulla.data.dao.SteamFriendDao
import `in`.dragonbra.vapulla.data.entity.ChatMessage
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

class ChatPresenter(context: Context,
                    private val chatMessageDao: ChatMessageDao,
                    private val steamFriendsDao: SteamFriendDao,
                    val steamId: SteamID) : VapullaPresenter<ChatView>(context) {

    companion object {
        const val UPDATE_INTERVAL = 1000L
        const val TYPING_INTERVAL = 20000L
    }

    private var lastTypingMessage = 0L

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
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            info("Bound to Steam service")
            steamService?.setChatFriendId(steamId)
            steamService?.isActivityRunning = true
            getMessageHistory()
        }
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

    override fun onDisconnected() {
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

    fun viewAccountMenuClicked() {
        ifViewAttached { it.browseUrl("http://steamcommunity.com/profiles/${steamId.convertToUInt64()}") }
    }
}
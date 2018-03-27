package `in`.dragonbra.vapulla.presenter

import `in`.dragonbra.javasteam.enums.EChatEntryType
import `in`.dragonbra.javasteam.enums.EFriendRelationship
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.SteamFriends
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.AliasHistoryCallback
import `in`.dragonbra.javasteam.types.JobID
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.javasteam.util.Strings
import `in`.dragonbra.vapulla.activity.ChatActivity
import `in`.dragonbra.vapulla.adapter.FriendListItem
import `in`.dragonbra.vapulla.data.dao.ChatMessageDao
import `in`.dragonbra.vapulla.data.dao.EmoticonDao
import `in`.dragonbra.vapulla.data.dao.SteamFriendDao
import `in`.dragonbra.vapulla.data.entity.ChatMessage
import `in`.dragonbra.vapulla.data.entity.Emoticon
import `in`.dragonbra.vapulla.steam.VapullaHandler
import `in`.dragonbra.vapulla.threading.runOnBackgroundThread
import `in`.dragonbra.vapulla.view.ChatView
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.arch.paging.LivePagedListBuilder
import android.arch.paging.PagedList
import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.IBinder
import org.jetbrains.anko.info
import java.util.regex.Pattern

class ChatPresenter(context: Context,
                    private val chatMessageDao: ChatMessageDao,
                    private val steamFriendsDao: SteamFriendDao,
                    private val emoticonDao: EmoticonDao,
                    private val steamId: SteamID) : VapullaPresenter<ChatView>(context) {

    companion object {
        const val UPDATE_INTERVAL = 1000L
        const val TYPING_INTERVAL = 20000L

        val EMOTE_PATTERN = Pattern.compile(":([a-zA-Z0-9]+):")
    }

    private var lastTypingMessage = 0L

    private var aliasJobId: JobID? = null

    private lateinit var chatData: LiveData<PagedList<ChatMessage>>

    private lateinit var friendData: LiveData<FriendListItem>

    private lateinit var emoticonData: LiveData<List<Emoticon>>

    private var emoteSet: Set<String> = setOf()

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

    private val emoteObserver = Observer<List<Emoticon>> { list ->
        ifViewAttached { it.showEmotes(list ?: emptyList()) }
        emoteSet = (list ?: emptyList()).map { it.name }.toSet()
    }

    override fun onServiceDisconnected(name: ComponentName) {
        info("Unbound from Steam service")
    }

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        info("Bound to Steam service")

        subscribe(steamService?.subscribe<AliasHistoryCallback> { onAliasHistory(it) })

        steamService?.setChatFriendId(steamId)
        steamService?.isActivityRunning = true
        getMessageHistory()
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

        emoticonData = emoticonDao.getLive()
        emoticonData.observe(view as ChatActivity, emoteObserver)

        ifViewAttached {
            it.showChat(chatData.value)
            it.updateFriendData(friendData.value)
            it.showEmotes(emoticonData.value ?: emptyList())
        }

        emoteSet = (emoticonData.value ?: emptyList()).map { it.name }.toSet()

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

    fun onAliasHistory(callback: AliasHistoryCallback) {
        if (aliasJobId == callback.jobID) {
            ifViewAttached {
                val list = callback.responses[0].names.toMutableList()
                list.sortByDescending { it.nameSince }
                val names = list.map { it.name }
                it.showAliases(names)
            }
        }
    }

    fun sendMessage(message: String) {
        if (Strings.isNullOrEmpty(message)) {
            return
        }
        lastTypingMessage = 0L

        val emoteMessage = findEmotes(message.replace('\u02D0', ':'))

        runOnBackgroundThread {
            steamService?.getHandler<SteamFriends>()?.sendChatMessage(steamId, EChatEntryType.ChatMsg, message)

            chatMessageDao.insert(ChatMessage(
                    emoteMessage,
                    System.currentTimeMillis(),
                    steamId.convertToUInt64(),
                    true,
                    true,
                    false
            ))
        }
    }

    private fun findEmotes(message: String): String {
        val matcher = EMOTE_PATTERN.matcher(message)

        if (matcher.find()) {
            val result = matcher.toMatchResult()

            val emote = result.group(1)

            if (emoteSet.contains(emote)) {
                val builder = StringBuilder(message)
                builder.setCharAt(result.start(), '\u02D0')
                builder.setCharAt(result.end() - 1, '\u02D0')

                return findEmotes(builder.toString())
            } else {
                return message.substring(0, result.end() - 1) + findEmotes(message.substring(result.end() - 1))
            }
        } else {
            return message
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

    fun viewAliasesMenuClicked() {
        runOnBackgroundThread { aliasJobId = steamService?.getHandler<SteamFriends>()?.requestAliasHistory(steamId) }
    }

    fun requestEmotes() {
        runOnBackgroundThread { steamService?.getHandler<VapullaHandler>()?.getEmoticonList() }
    }
}
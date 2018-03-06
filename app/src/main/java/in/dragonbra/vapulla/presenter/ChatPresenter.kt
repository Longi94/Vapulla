package `in`.dragonbra.vapulla.presenter

import `in`.dragonbra.javasteam.enums.EChatEntryType
import `in`.dragonbra.javasteam.enums.EClientPersonaStateFlag
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.SteamFriends
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.DisconnectedCallback
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.activity.ChatActivity
import `in`.dragonbra.vapulla.data.dao.ChatMessageDao
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
import android.os.IBinder
import org.jetbrains.anko.info
import org.jetbrains.anko.intentFor
import java.io.Closeable
import java.util.*

class ChatPresenter(val context: Context,
                    val chatMessageDao: ChatMessageDao,
                    val steamId: SteamID) : VapullaPresenter<ChatView>(), Observer<PagedList<ChatMessage>> {

    companion object {
        val REQUESTED_INFO = EClientPersonaStateFlag.code(EnumSet.of(
                EClientPersonaStateFlag.Status,
                EClientPersonaStateFlag.LastSeen,
                EClientPersonaStateFlag.PlayerName,
                EClientPersonaStateFlag.Presence
        ))
    }

    private var bound = false

    private var steamService: SteamService? = null

    private val subs: MutableList<Closeable?> = LinkedList()

    private var chatData: LiveData<PagedList<ChatMessage>>? = null

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
            steamService?.removeNotifications()
            steamService?.isActivityRunning = true
            steamService?.chatFriendId = steamId.convertToUInt64()

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
            steamService?.removeNotifications()
            steamService?.isActivityRunning = true
            steamService?.chatFriendId = steamId.convertToUInt64()
            getMessageHistory()
        }

        chatData = LivePagedListBuilder(chatMessageDao.findLivePaged(steamId.convertToUInt64()), 50).build()
        chatData?.observe(view as ChatActivity, this)

        ifViewAttached { it.showChat(chatData?.value) }
    }

    override fun onPause() {
        if (bound) {
            steamService?.isActivityRunning = false
            steamService?.chatFriendId = null
        }

    }

    override fun onChanged(t: PagedList<ChatMessage>?) {
        ifViewAttached { it.showChat(t) }
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
}
package `in`.dragonbra.vapulla.presenter

import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.SteamFriends
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.DisconnectedCallback
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.activity.HomeActivity
import `in`.dragonbra.vapulla.adapter.FriendListItem
import `in`.dragonbra.vapulla.data.dao.SteamFriendDao
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.service.SteamService
import `in`.dragonbra.vapulla.threading.runOnBackgroundThread
import `in`.dragonbra.vapulla.view.HomeView
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import org.jetbrains.anko.info
import org.jetbrains.anko.intentFor
import java.io.Closeable
import java.util.*

class HomePresenter(val context: Context,
                    private val steamFriendDao: SteamFriendDao,
                    private val account: AccountManager) : VapullaPresenter<HomeView>(), AccountManager.AccountManagerListener {

    private var bound = false

    private var steamService: SteamService? = null

    private val subs: MutableList<Closeable?> = LinkedList()

    private lateinit var friendsData: LiveData<List<FriendListItem>>

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
            steamService?.isActivityRunning = true
        }
    }

    private fun onDisconnected() {
        ifViewAttached {
            it.closeApp()
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
            steamService?.isActivityRunning = true
        }

        friendsData = steamFriendDao.getLive()
        friendsData.observe(view as HomeActivity, dataObserver)

        account.addListener(this)

        ifViewAttached {
            it.showAccount(account)
            if (friendsData.value == null) {
                it.showFriends(emptyList())
            } else {
                it.showFriends(friendsData.value!!)
            }
        }
    }

    override fun onPause() {
        if (bound) {
            steamService?.isActivityRunning = false
        }

        friendsData.removeObserver(dataObserver)
        account.removeListener(this)
    }

    override fun unAccountUpdate(account: AccountManager) {
        ifViewAttached { it.showAccount(account) }
    }

    private val dataObserver: Observer<List<FriendListItem>> = Observer { list ->
        ifViewAttached { it.showFriends(list!!) }
    }

    fun disconnect() {
        runOnBackgroundThread { steamService?.disconnect() }
    }

    fun changeStatus(state: EPersonaState) {
        if (account.state != state) {
            runOnBackgroundThread { steamService?.getHandler<SteamFriends>()?.personaState = state }
        }
    }

    fun acceptRequest(friend: FriendListItem) {
        runOnBackgroundThread { steamService?.getHandler<SteamFriends>()?.addFriend(SteamID(friend.id)) }
    }

    fun ignoreRequest(friend: FriendListItem) {
        runOnBackgroundThread { steamService?.getHandler<SteamFriends>()?.removeFriend(SteamID(friend.id)) }
    }

    fun blockRequest(friend: FriendListItem) {
        ifViewAttached { it.showBlockFriendDialog(friend) }
    }

    fun confirmBlockFriend(friend: FriendListItem) {
        runOnBackgroundThread {
            runOnBackgroundThread { steamService?.getHandler<SteamFriends>()?.ignoreFriend(SteamID(friend.id)) }
        }
    }
}
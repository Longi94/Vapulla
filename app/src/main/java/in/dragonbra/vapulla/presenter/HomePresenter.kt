package `in`.dragonbra.vapulla.presenter

import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.SteamFriends
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.activity.HomeActivity
import `in`.dragonbra.vapulla.adapter.FriendListItem
import `in`.dragonbra.vapulla.data.dao.SteamFriendDao
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.threading.runOnBackgroundThread
import `in`.dragonbra.vapulla.view.HomeView
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.content.ComponentName
import android.content.Context
import android.os.IBinder
import org.jetbrains.anko.info

class HomePresenter(context: Context,
                    private val steamFriendDao: SteamFriendDao,
                    private val account: AccountManager) :
        VapullaPresenter<HomeView>(context), AccountManager.AccountManagerListener {

    private lateinit var friendsData: LiveData<List<FriendListItem>>

    override fun onServiceDisconnected(name: ComponentName) {
        info("Unbound from Steam service")
    }

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        info("Bound to Steam service")
        steamService?.isActivityRunning = true
    }

    override fun onDisconnected() {
        ifViewAttached {
            it.closeApp()
        }
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
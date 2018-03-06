package `in`.dragonbra.vapulla.activity

import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.adapter.FriendListAdapter
import `in`.dragonbra.vapulla.adapter.FriendListItem
import `in`.dragonbra.vapulla.extension.click
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.presenter.HomePresenter
import `in`.dragonbra.vapulla.util.Utils
import `in`.dragonbra.vapulla.view.HomeView
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.PopupMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import kotlinx.android.synthetic.main.activity_home.*
import org.jetbrains.anko.startActivity
import javax.inject.Inject

class HomeActivity : VapullaBaseActivity<HomeView, HomePresenter>(), HomeView, PopupMenu.OnMenuItemClickListener, FriendListAdapter.OnItemSelectedListener {

    @Inject
    lateinit var homePresenter: HomePresenter

    lateinit var friendListAdapter: FriendListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        vapulla().graph.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        friendListAdapter = FriendListAdapter(this)
        friendListAdapter.listener = this

        friendList.layoutManager = LinearLayoutManager(this)
        friendList.adapter = friendListAdapter

        moreButton.click {
            val popup = PopupMenu(this@HomeActivity, it)
            popup.menuInflater.inflate(R.menu.menu_home, popup.menu)
            popup.show()
            popup.setOnMenuItemClickListener(this@HomeActivity)
        }

        statusButton.click {
            statusLayout.visibility = if (statusLayout.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
    }

    override fun createPresenter(): HomePresenter = homePresenter

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return true
    }

    override fun onMenuItemClick(item: MenuItem?) = when (item?.itemId) {
        R.id.logOut -> {
            presenter.disconnect()
            true
        }
        else -> false
    }

    override fun closeApp() {
        runOnUiThread {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.addCategory(Intent.CATEGORY_HOME)
            startActivity(intent)
            finish()
        }
    }

    override fun showFriends(list: List<FriendListItem>?) {
        friendListAdapter.swap(list)
    }

    override fun showAccount(account: AccountManager) {
        runOnUiThread {
            localUsername.text = account.nickname
            localStatus.text = account.state.toString()

            Glide.with(this@HomeActivity)
                    .load(Utils.getAvatarUrl(account.avatarHash))
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .apply(Utils.avatarOptions)
                    .into(localAvatar)
        }
    }

    override fun onItemSelected(friend: FriendListItem) {
        startActivity<ChatActivity>(ChatActivity.INTENT_STEAM_ID to friend.id)
    }

    fun changeStatus(v: View) {
        presenter.changeStatus(when (v.id) {
            R.id.onlineButton -> EPersonaState.Online
            R.id.awayButton -> EPersonaState.Away
            R.id.busyButton -> EPersonaState.Busy
            R.id.lookingToPlayButton -> EPersonaState.LookingToPlay
            R.id.lookingToTradeButton -> EPersonaState.LookingToTrade
            R.id.offlineButton -> EPersonaState.Offline
            else -> throw IllegalArgumentException("change status called by unknown view")
        })
    }
}

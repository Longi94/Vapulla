package `in`.dragonbra.vapulla.activity

import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.adapter.FriendListAdapter
import `in`.dragonbra.vapulla.adapter.FriendListItem
import `in`.dragonbra.vapulla.chat.PaperPlane
import `in`.dragonbra.vapulla.extension.click
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.manager.GameSchemaManager
import `in`.dragonbra.vapulla.presenter.HomePresenter
import `in`.dragonbra.vapulla.util.OfflineStatusUpdater
import `in`.dragonbra.vapulla.util.Utils
import `in`.dragonbra.vapulla.view.HomeView
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActivityOptionsCompat
import android.support.v4.util.Pair
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.PopupMenu
import android.text.format.DateUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.list_friend.view.*
import org.jetbrains.anko.startActivity
import javax.inject.Inject

class HomeActivity : VapullaBaseActivity<HomeView, HomePresenter>(), HomeView, PopupMenu.OnMenuItemClickListener, FriendListAdapter.OnItemSelectedListener {

    companion object {
        const val UPDATE_INTERVAL = DateUtils.MINUTE_IN_MILLIS
    }

    @Inject
    lateinit var homePresenter: HomePresenter

    @Inject
    lateinit var gameSchemaManager: GameSchemaManager

    private lateinit var paperPlane: PaperPlane

    private lateinit var offlineStatusUpdater: OfflineStatusUpdater

    private lateinit var friendListAdapter: FriendListAdapter

    private val updateHandler: Handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        vapulla().graph.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        paperPlane = PaperPlane(this, 14.0f)
        offlineStatusUpdater = OfflineStatusUpdater(this)
        friendListAdapter = FriendListAdapter(this, gameSchemaManager, paperPlane, offlineStatusUpdater)
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
            val popup = PopupMenu(this@HomeActivity, it)
            popup.menuInflater.inflate(R.menu.menu_status, popup.menu)
            popup.setOnMenuItemClickListener({
                val status = when (it.itemId) {
                    R.id.online -> EPersonaState.Online
                    R.id.away -> EPersonaState.Away
                    R.id.busy -> EPersonaState.Busy
                    R.id.lookingToTrade -> EPersonaState.LookingToTrade
                    R.id.lookingToPlay -> EPersonaState.LookingToPlay
                    else -> EPersonaState.Offline
                }
                presenter.changeStatus(status)
                true
            })
            popup.show()
        }
    }

    override fun onResume() {
        super.onResume()
        updateHandler.postDelayed({ updateList() }, UPDATE_INTERVAL)
    }

    override fun onPause() {
        super.onPause()
        updateHandler.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        paperPlane.clearAll()
        offlineStatusUpdater.clear()
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
        R.id.settings -> {
            startActivity<SettingsActivity>()
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

    override fun showFriends(list: List<FriendListItem>) {
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

    override fun onItemSelected(friend: FriendListItem, v: View) {
        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(this,
                Pair.create(v.avatar as View, "avatar"),
                Pair.create(v.status as View, "status"),
                Pair.create(v.nickname as View, "nickname"),
                Pair.create(v.username as View, "name"))

        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra(ChatActivity.INTENT_STEAM_ID, friend.id)
        startActivity(intent, options.toBundle())
    }

    override fun onRequestAccept(friend: FriendListItem) {
        presenter.acceptRequest(friend)
    }

    override fun onRequestIgnore(friend: FriendListItem) {
        presenter.ignoreRequest(friend)
    }

    override fun onRequestBlock(friend: FriendListItem) {
        presenter.blockRequest(friend)
    }

    override fun showBlockFriendDialog(friend: FriendListItem) {
        val name = friend.name ?: ""
        val builder = AlertDialog.Builder(this)

        builder.setMessage(getString(R.string.dialogMessageBlockFriend, name))
                .setTitle(getString(R.string.dialogTitleBlockFriend, name))
                .setPositiveButton(R.string.dialogYes, { _, _ -> presenter.confirmBlockFriend(friend) })
                .setNegativeButton(R.string.dialogNo, null)

        builder.create().show()
    }

    private fun updateList() {
        offlineStatusUpdater.updateAll()
        updateHandler.postDelayed({ updateList() }, UPDATE_INTERVAL)
    }
}

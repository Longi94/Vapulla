package `in`.dragonbra.vapulla.activity

import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.adapter.FriendListAdapter
import `in`.dragonbra.vapulla.data.entity.SteamFriend
import `in`.dragonbra.vapulla.presenter.HomePresenter
import `in`.dragonbra.vapulla.view.HomeView
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_home.*
import javax.inject.Inject

class HomeActivity : VapullaBaseActivity<HomeView, HomePresenter>(), HomeView {

    @Inject
    lateinit var homePresenter: HomePresenter

    lateinit var friendListAdapter: FriendListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        vapulla().graph.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        friendListAdapter = FriendListAdapter(this)

        friendList.layoutManager = LinearLayoutManager(this)
        friendList.adapter = friendListAdapter
    }

    override fun createPresenter(): HomePresenter = homePresenter

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_home, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?) = when (item?.itemId) {
        R.id.logOut -> {
            presenter.disconnect()
            true
        }
        else -> super.onOptionsItemSelected(item)
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

    override fun showFriends(list: List<SteamFriend>?) {
        friendListAdapter.swap(list)
    }
}

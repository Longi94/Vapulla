package `in`.dragonbra.vapulla.activity

import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.presenter.HomePresenter
import `in`.dragonbra.vapulla.view.HomeView
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import org.jetbrains.anko.clearTask
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.newTask

class HomeActivity : VapullaBaseActivity<HomeView, HomePresenter>(), HomeView {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
    }

    override fun createPresenter(): HomePresenter = HomePresenter(this)

    override fun onStart() {
        super.onStart()
        presenter.onStart()
    }

    override fun onStop() {
        super.onStop()
        presenter.onStop()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_home, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?) = when (item?.itemId) {
        R.id.logOut -> {
            presenter.logOut()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun showLoginScreen() {
        runOnUiThread { startActivity(intentFor<LoginActivity>().newTask().clearTask()) }
    }
}

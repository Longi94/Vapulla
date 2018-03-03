package `in`.dragonbra.vapulla.activity

import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.presenter.HomePresenter
import `in`.dragonbra.vapulla.view.HomeView
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem

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
}

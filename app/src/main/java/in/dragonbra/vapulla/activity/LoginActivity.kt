package `in`.dragonbra.vapulla.activity

import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.presenter.LoginPresenter
import `in`.dragonbra.vapulla.view.LoginView
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.hannesdorfmann.mosby3.mvp.MvpActivity
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.AnkoLogger

class LoginActivity : MvpActivity<LoginView, LoginPresenter>(), LoginView, AnkoLogger {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onStart() {
        super.onStart()
        presenter.onStart()
    }

    override fun onStop() {
        super.onStop()
        presenter.onStop()
    }

    override fun createPresenter(): LoginPresenter = LoginPresenter(this)

    override fun showLoginScreen() {
        runOnUiThread {
            loading_layout.visibility = View.GONE
            username.visibility = View.VISIBLE
            password.visibility = View.VISIBLE
            login.visibility = View.VISIBLE
        }
    }

    override fun onDisconnected() {
        Toast.makeText(this, "Disconnected from Steam", Toast.LENGTH_LONG).show()
    }
}

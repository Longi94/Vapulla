package `in`.dragonbra.vapulla.activity

import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.presenter.LoginPresenter
import `in`.dragonbra.vapulla.view.LoginView
import android.os.Bundle
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_login.*
import org.jetbrains.anko.startActivity

class LoginActivity : VapullaBaseActivity<LoginView, LoginPresenter>(), LoginView {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        login.click { presenter.login(username.text.toString(), password.text.toString()) }
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

    override fun onDisconnected() {
        Toast.makeText(this, "Disconnected from Steam", Toast.LENGTH_LONG).show()

        runOnUiThread {
            loadingLayout.visibility = View.GONE
            username.visibility = View.VISIBLE
            password.visibility = View.VISIBLE
            login.visibility = View.VISIBLE
        }
    }

    override fun showLoading(text: String) {
        runOnUiThread {
            loadingLayout.visibility = View.VISIBLE
            loadingText.text = text
            username.visibility = View.GONE
            password.visibility = View.GONE
            login.visibility = View.GONE
        }
    }

    override fun loginSuccess() {
        startActivity<HomeActivity>()
    }
}

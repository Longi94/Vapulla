package `in`.dragonbra.vapulla.activity

import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.extension.click
import `in`.dragonbra.vapulla.presenter.LoginPresenter
import `in`.dragonbra.vapulla.view.LoginView
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.view.View
import kotlinx.android.synthetic.main.activity_login.*
import org.jetbrains.anko.clearTask
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.newTask
import javax.inject.Inject

class LoginActivity : VapullaBaseActivity<LoginView, LoginPresenter>(), LoginView {

    @Inject
    lateinit var loginPresenter: LoginPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        vapulla().graph.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        login.click { presenter.login(username.text.toString(), password.text.toString()) }

        steamGuardButton.click {
            val code = steamGuardInput.text.toString()

            if (code.length < 5) {
                Snackbar.make(steamGuardLayout, "Code must be 5 characters long", Snackbar.LENGTH_LONG).show()
                return@click
            }

            presenter.login(code)
        }
    }

    override fun createPresenter(): LoginPresenter = loginPresenter

    override fun onDisconnected() {
        runOnUiThread {
            Snackbar.make(steamGuardLayout, "Disconnected from Steam", Snackbar.LENGTH_LONG).show()
            loadingLayout.visibility = View.GONE
            username.visibility = View.VISIBLE
            password.visibility = View.VISIBLE
            login.visibility = View.VISIBLE
            steamGuardLayout.visibility = View.GONE
        }
    }

    override fun showLoading(text: String) {
        runOnUiThread {
            loadingLayout.visibility = View.VISIBLE
            loadingText.text = text
            username.visibility = View.GONE
            password.visibility = View.GONE
            login.visibility = View.GONE
            steamGuardLayout.visibility = View.GONE
        }
    }

    override fun loginSuccess() {
        startActivity(intentFor<HomeActivity>().newTask().clearTask())
    }

    override fun showSteamGuard() {
        runOnUiThread {
            loadingLayout.visibility = View.GONE
            username.visibility = View.GONE
            password.visibility = View.GONE
            login.visibility = View.GONE
            steamGuardLayout.visibility = View.VISIBLE
        }
    }
}

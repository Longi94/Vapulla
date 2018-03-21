package `in`.dragonbra.vapulla.activity

import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.extension.click
import `in`.dragonbra.vapulla.extension.hide
import `in`.dragonbra.vapulla.extension.show
import `in`.dragonbra.vapulla.presenter.LoginPresenter
import `in`.dragonbra.vapulla.util.Utils
import `in`.dragonbra.vapulla.view.LoginView
import android.os.Bundle
import android.support.design.widget.Snackbar
import kotlinx.android.synthetic.main.login_form.*
import kotlinx.android.synthetic.main.login_loading.*
import kotlinx.android.synthetic.main.login_retry.*
import kotlinx.android.synthetic.main.login_steam_guard.*
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

        login.click {
            Utils.hideKeyboardFrom(this@LoginActivity, it)
            presenter.login(username.text.toString(), password.text.toString())
        }

        steamGuardButton.click {
            val code = steamGuardInput.text.toString()

            if (code.length < 5) {
                Snackbar.make(steamGuardLayout, "Code must be 5 characters long", Snackbar.LENGTH_LONG).show()
                return@click
            }

            Utils.hideKeyboardFrom(this@LoginActivity, it)
            presenter.login(code)
        }

        retryButton.click {
            presenter.retry();
        }
    }

    override fun createPresenter(): LoginPresenter = loginPresenter

    override fun showLoading(text: String) {
        runOnUiThread {
            loadingLayout.show()
            loadingText.text = text
            loginLayout.hide()
            steamGuardLayout.hide()
            failedLayout.hide()
        }
    }

    override fun loginSuccess() {
        startActivity(intentFor<HomeActivity>().newTask().clearTask())
    }

    override fun showSteamGuard() {
        runOnUiThread {
            loadingLayout.hide()
            loginLayout.hide()
            steamGuardLayout.show()
            failedLayout.hide()
        }
    }

    override fun showLoginForm() {
        runOnUiThread {
            loadingLayout.hide()
            loginLayout.show()
            steamGuardLayout.hide()
            failedLayout.hide()
        }
    }

    override fun showFailedScreen() {
        runOnUiThread {
            loadingLayout.hide()
            loginLayout.hide()
            steamGuardLayout.hide()
            failedLayout.show()
        }
    }
}

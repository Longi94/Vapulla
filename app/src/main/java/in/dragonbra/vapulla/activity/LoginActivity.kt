package `in`.dragonbra.vapulla.activity

import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.extension.click
import `in`.dragonbra.vapulla.extension.hide
import `in`.dragonbra.vapulla.extension.show
import `in`.dragonbra.vapulla.presenter.LoginPresenter
import `in`.dragonbra.vapulla.view.LoginView
import android.os.Bundle
import android.support.design.widget.Snackbar
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
            loadingLayout.hide()
            username.show()
            password.show()
            login.show()
            steamGuardLayout.hide()
        }
    }

    override fun showLoading(text: String) {
        runOnUiThread {
            loadingLayout.show()
            loadingText.text = text
            username.hide()
            password.hide()
            login.hide()
            steamGuardLayout.hide()
        }
    }

    override fun loginSuccess() {
        startActivity(intentFor<HomeActivity>().newTask().clearTask())
    }

    override fun showSteamGuard() {
        runOnUiThread {
            loadingLayout.hide()
            username.hide()
            password.hide()
            login.hide()
            steamGuardLayout.show()
        }
    }
}

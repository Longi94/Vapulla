package `in`.dragonbra.vapulla.activity

import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.extension.click
import `in`.dragonbra.vapulla.extension.hide
import `in`.dragonbra.vapulla.extension.show
import `in`.dragonbra.vapulla.presenter.LoginPresenter
import `in`.dragonbra.vapulla.util.Utils
import `in`.dragonbra.vapulla.view.LoginView
import android.graphics.drawable.Animatable
import android.graphics.drawable.Animatable2
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.Snackbar
import android.support.graphics.drawable.Animatable2Compat
import android.support.graphics.drawable.AnimatedVectorDrawableCompat
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

    fun startLoadingAnimation() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            val d = vapullaLogoMiddle.drawable as AnimatedVectorDrawableCompat
            val d2 = vapullaLogoBottom.drawable as AnimatedVectorDrawableCompat
            vapullaLogoMiddle.setImageDrawable(d)
            d.registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
                override fun onAnimationEnd(drawable: Drawable?) {
                    d.start()
                    Handler().postDelayed({
                        d2.stop()
                        d2.start()
                    }, 300)
                }
            })
            d.start()
            Handler().postDelayed({ d2.start() }, 400)
        } else {
            val d = vapullaLogoMiddle.drawable as AnimatedVectorDrawable
            val d2 = vapullaLogoBottom.drawable as AnimatedVectorDrawable
            d.registerAnimationCallback(object : Animatable2.AnimationCallback() {
                override fun onAnimationEnd(drawable: Drawable?) {
                    d.start()
                    Handler().postDelayed({
                        d2.stop()
                        d2.start()
                    }, 300)
                }
            })
            d.start()
            Handler().postDelayed({ d2.start() }, 400)
        }
    }

    fun stopLoadingAnimation() {
        val d = vapullaLogoMiddle.drawable as Animatable
        val d2 = vapullaLogoBottom.drawable as Animatable

        d.stop()
        d2.stop()
    }
}

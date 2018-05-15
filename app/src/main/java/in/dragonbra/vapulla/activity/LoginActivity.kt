package `in`.dragonbra.vapulla.activity

import `in`.dragonbra.javasteam.util.Strings
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.anim.AutoParallelTransition
import `in`.dragonbra.vapulla.anim.TransitionListener
import `in`.dragonbra.vapulla.anim.VectorAnimCompat
import `in`.dragonbra.vapulla.extension.*
import `in`.dragonbra.vapulla.presenter.LoginPresenter
import `in`.dragonbra.vapulla.view.LoginView
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.support.constraint.ConstraintSet
import android.support.graphics.drawable.Animatable2Compat
import android.support.transition.Transition
import android.support.transition.TransitionManager
import android.view.animation.AnimationUtils
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.login_button.*
import kotlinx.android.synthetic.main.login_error_text.*
import kotlinx.android.synthetic.main.login_loading_text.*
import kotlinx.android.synthetic.main.login_logo_top.*
import kotlinx.android.synthetic.main.login_password.*
import kotlinx.android.synthetic.main.login_retry_button.*
import kotlinx.android.synthetic.main.login_steam_guard.*
import kotlinx.android.synthetic.main.login_steam_guard_button.*
import kotlinx.android.synthetic.main.login_steam_guard_cancel.*
import kotlinx.android.synthetic.main.login_username.*
import org.jetbrains.anko.clearTask
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.newTask
import javax.inject.Inject


class LoginActivity : VapullaBaseActivity<LoginView, LoginPresenter>(), LoginView {

    @Inject
    lateinit var loginPresenter: LoginPresenter

    lateinit var handler: Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        vapulla().graph.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        handler = Handler()

        loadingText.setFactory(TextSwitcher.factory(this))
        loadingText.inAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        loadingText.outAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_out)

        login.click { login() }

        steamGuardButton.click {
            val code = steamGuardInput.text.toString()

            if (code.length < 5) {
                steamGuardLayout.error = getString(R.string.editTextErrorSteamGuard)
                return@click
            }

            hideKeyboardFrom(it)

            val transition = AutoParallelTransition()
            transition.addListener(object : TransitionListener() {
                override fun onTransitionEnd(transition: Transition) {
                    startLoadingAnimation()
                    presenter.login(code)
                }
            })

            val constraintSet = ConstraintSet()
            constraintSet.clone(this, R.layout.activity_login_frame_loading)
            TransitionManager.beginDelayedTransition(rootLayout, transition)
            constraintSet.applyTo(rootLayout)
        }

        retryButton.click {
            loadingText.setText(null)

            val constraintSet = ConstraintSet()
            constraintSet.clone(this, R.layout.activity_login_frame_loading)
            TransitionManager.beginDelayedTransition(rootLayout, AutoParallelTransition())
            constraintSet.applyTo(rootLayout)

            val faceAnim = getDrawable(R.drawable.animated_vapulla_from_face)
            vapullaLogoBottom.setImageDrawable(faceAnim)

            VectorAnimCompat.registerAnimationCallback(faceAnim as Animatable, object : Animatable2Compat.AnimationCallback() {
                override fun onAnimationEnd(drawable: Drawable) {
                    vapullaLogoTop.setImageDrawable(getDrawable(R.drawable.vapulla_top))
                    vapullaLogoBottom.show()
                    vapullaLogoMiddle.show()
                    startLoadingAnimation()
                    presenter.retry()
                }
            })

            faceAnim.start()
        }

        steamGuardButtonCancel.click {
            presenter.cancelSteamGuard()
        }

        username.bindLayout()
        password.bindLayout()
        steamGuardInput.bindLayout()
    }

    override fun createPresenter(): LoginPresenter = loginPresenter

    override fun showLoading(text: String) {
        runOnUiThread {
            loadingText.setText(text)
        }
    }

    override fun startLoading(finishedAction: (() -> Unit)?) {
        val transition = AutoParallelTransition()
        /*transition.addListener(object : TransitionListener() {
            override fun onTransitionEnd(transition: Transition) {
                startLoadingAnimation()
                finishedAction?.invoke()
            }
        })*/

        val constraintSet = ConstraintSet()
        constraintSet.clone(this, R.layout.activity_login_frame_loading)
        TransitionManager.beginDelayedTransition(rootLayout, transition)
        constraintSet.applyTo(rootLayout)

        startLoadingAnimation()
        finishedAction?.invoke()
    }

    override fun loginSuccess() {
        startActivity(intentFor<HomeActivity>().newTask().clearTask())
        finish()
    }

    override fun showSteamGuard(is2Fa: Boolean, errorMessage: String?) {
        runOnUiThread {
            stopLoadingAnimation()
            loadingText.setText("")

            errorMessage?.let {
                errorText.text = errorMessage
            }

            loadingText.setText(if (is2Fa) getString(R.string.loadingTextSteamGuardMobile) else
                getString(R.string.loadingTextSteamGuardEmail))

            val layout = errorMessage?.let { R.layout.activity_login_frame_steamguard_error }
                    ?: run { R.layout.activity_login_frame_steamguard }

            val constraintSet = ConstraintSet()
            constraintSet.clone(this, layout)
            TransitionManager.beginDelayedTransition(rootLayout, AutoParallelTransition())
            constraintSet.applyTo(rootLayout)
        }
    }

    override fun showLoginForm(errorMessage: String?) {
        runOnUiThread {
            stopLoadingAnimation()
            loadingText.setText("")

            errorMessage?.let {
                errorText.text = errorMessage
            }

            val layout = errorMessage?.let { R.layout.activity_login_frame_form_error }
                    ?: run { R.layout.activity_login_frame_form }

            val constraintSet = ConstraintSet()
            constraintSet.clone(this, layout)
            TransitionManager.beginDelayedTransition(rootLayout, AutoParallelTransition())
            constraintSet.applyTo(rootLayout)
        }
    }

    override fun showFailedScreen() {
        runOnUiThread {
            stopLoadingAnimation()

            vapullaLogoBottom.invisible()
            vapullaLogoMiddle.invisible()

            val faceAnim = getDrawable(R.drawable.animated_vapulla_to_face)
            vapullaLogoTop.setImageDrawable(faceAnim)
            (faceAnim as Animatable).start()

            loadingText.setText(getString(R.string.loadingTextFailed))

            val constraintSet = ConstraintSet()
            constraintSet.clone(this, R.layout.activity_login_frame_failed)
            TransitionManager.beginDelayedTransition(rootLayout, AutoParallelTransition())
            constraintSet.applyTo(rootLayout)
        }
    }

    fun login() {
        val username = username.text.toString()

        if (Strings.isNullOrEmpty(username)) {
            usernameLayout.error = getString(R.string.editTextErrorUsername)
            return
        }

        val password = password.text.toString()

        if (Strings.isNullOrEmpty(password)) {
            passwordLayout.error = getString(R.string.editTextErrorPassword)
            return
        }

        hideKeyboardFrom(login)

        startLoading({ presenter.login(username, password) })
    }

    private fun startLoadingAnimation() {
        val d = vapullaLogoMiddle.drawable as Animatable
        val d2 = vapullaLogoBottom.drawable as Animatable

        VectorAnimCompat.registerAnimationCallback(d, object : Animatable2Compat.AnimationCallback() {
            override fun onAnimationEnd(drawable: Drawable) {
                d.start()
                handler.postDelayed({
                    d2.stop()
                    d2.start()
                }, 300)
            }
        })
        d.start()
        handler.postDelayed({ d2.start() }, 300)
    }

    private fun stopLoadingAnimation() {
        handler.removeCallbacksAndMessages(null)

        val d = vapullaLogoMiddle.drawable as Animatable
        val d2 = vapullaLogoBottom.drawable as Animatable

        VectorAnimCompat.clearAnimationCallbacks(d)
        VectorAnimCompat.clearAnimationCallbacks(d2)

        d.stop()
        d2.stop()
    }
}

package `in`.dragonbra.vapulla.activity

import `in`.dragonbra.javasteam.util.Strings
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.anim.AutoParallelTransition
import `in`.dragonbra.vapulla.anim.TransitionListener
import `in`.dragonbra.vapulla.anim.VectorAnimCompat
import `in`.dragonbra.vapulla.extension.TextSwitcher
import `in`.dragonbra.vapulla.extension.bindLayout
import `in`.dragonbra.vapulla.extension.click
import `in`.dragonbra.vapulla.extension.hideKeyboardFrom
import `in`.dragonbra.vapulla.presenter.LoginPresenter
import `in`.dragonbra.vapulla.view.LoginView
import android.graphics.drawable.Animatable
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.constraint.ConstraintSet
import android.support.graphics.drawable.Animatable2Compat
import android.support.transition.Transition
import android.support.transition.TransitionManager
import android.text.InputType
import android.view.animation.AnimationUtils
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.login_button.*
import kotlinx.android.synthetic.main.login_error_text.*
import kotlinx.android.synthetic.main.login_loading_text.*
import kotlinx.android.synthetic.main.login_logo_top.*
import kotlinx.android.synthetic.main.login_password.*
import kotlinx.android.synthetic.main.login_steam_guard_cancel.*
import kotlinx.android.synthetic.main.login_username.*
import org.jetbrains.anko.*
import javax.inject.Inject


class LoginActivity : VapullaBaseActivity<LoginView, LoginPresenter>(), LoginView {

    companion object {
        private const val FRAME_SPLASH = 0
        private const val FRAME_FAILED = 1
        private const val FRAME_FORM = 2
        private const val FRAME_LOADING = 3
        private const val FRAME_STEAMGUARD = 4
    }

    @Inject
    lateinit var loginPresenter: LoginPresenter

    private var frame = FRAME_SPLASH

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        vapulla().graph.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        loadingText.setFactory(TextSwitcher.factory(this))
        loadingText.inAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        loadingText.outAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_out)

        login.click {
            when(frame) {
                FRAME_FORM -> login()
                FRAME_STEAMGUARD -> {
                    frame = FRAME_LOADING
                    val code = username.text.toString()

                    if (code.length < 5) {
                        usernameLayout.error = getString(R.string.editTextErrorSteamGuard)
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
                FRAME_FAILED -> {
                    frame = FRAME_LOADING
                    loadingText.setText(null)

                    val constraintSet = ConstraintSet()
                    constraintSet.clone(this, R.layout.activity_login_frame_loading)
                    TransitionManager.beginDelayedTransition(rootLayout, AutoParallelTransition())
                    constraintSet.applyTo(rootLayout)

                    val faceAnim = getDrawable(R.drawable.animated_vapulla_from_face)
                    vapullaLogo.setImageDrawable(faceAnim)

                    VectorAnimCompat.registerAnimationCallback(faceAnim as Animatable, object : Animatable2Compat.AnimationCallback() {
                        override fun onAnimationEnd(drawable: Drawable) {
                            startLoadingAnimation()
                            presenter.retry()
                        }
                    })

                    faceAnim.start()
                }
            }
        }

        steamGuardButtonCancel.click {
            presenter.cancelSteamGuard()
        }

        username.bindLayout()
        password.bindLayout()
    }

    override fun createPresenter(): LoginPresenter = loginPresenter

    override fun showLoading(text: String) {
        runOnUiThread {
            loadingText.setText(text)
        }
    }

    override fun startLoading(finishedAction: (() -> Unit)?) {
        frame = FRAME_LOADING
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
        frame = FRAME_STEAMGUARD
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

            login.text = getString(R.string.buttonSteamGuard)
            username.setText("")
            username.inputType = InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            username.allCaps = true
            usernameLayout.hint = getString(R.string.editTextHintSteamGuard)
        }
    }

    override fun showLoginForm(errorMessage: String?) {
        frame = FRAME_FORM
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

            login.text = getString(R.string.buttonLogin)
            password.setText("")
            username.setText("")
            username.inputType = InputType.TYPE_TEXT_VARIATION_PERSON_NAME
            username.allCaps = false
            usernameLayout.hint = getString(R.string.editTextHintUsername)
        }
    }

    override fun showFailedScreen() {
        frame = FRAME_FAILED
        runOnUiThread {
            stopLoadingAnimation()

            val faceAnim = getDrawable(R.drawable.animated_vapulla_to_face)
            vapullaLogo.setImageDrawable(faceAnim)
            (faceAnim as Animatable).start()

            loadingText.setText(getString(R.string.loadingTextFailed))

            val constraintSet = ConstraintSet()
            constraintSet.clone(this, R.layout.activity_login_frame_failed)
            TransitionManager.beginDelayedTransition(rootLayout, AutoParallelTransition())
            constraintSet.applyTo(rootLayout)

            login.text = getString(R.string.buttonRetry)
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
        val d = getDrawable(R.drawable.vapulla_loading) as AnimationDrawable
        vapullaLogo.setImageDrawable(d)
        d.start()
    }

    private fun stopLoadingAnimation() {
        (vapullaLogo.image as? AnimationDrawable)?.stop()
        vapullaLogo.setImageDrawable(getDrawable(R.drawable.vapulla))
    }
}

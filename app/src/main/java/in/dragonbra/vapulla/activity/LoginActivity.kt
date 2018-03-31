package `in`.dragonbra.vapulla.activity

import `in`.dragonbra.javasteam.util.Strings
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.anim.AutoParallelTransition
import `in`.dragonbra.vapulla.anim.TransitionListener
import `in`.dragonbra.vapulla.anim.VectorAnimCompat
import `in`.dragonbra.vapulla.extension.*
import `in`.dragonbra.vapulla.presenter.LoginPresenter
import `in`.dragonbra.vapulla.util.Utils
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
import org.jetbrains.anko.clearTask
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.newTask
import javax.inject.Inject


class LoginActivity : VapullaBaseActivity<LoginView, LoginPresenter>(), LoginView {

    @Inject
    lateinit var loginPresenter: LoginPresenter

    lateinit var handler: Handler

    override fun onCreate(savedInstanceState: Bundle?) {
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
                steamGuardLayout.error = "Code must be 5 characters long"
                return@click
            }

            Utils.hideKeyboardFrom(this@LoginActivity, it)
            presenter.login(code)

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
            vapullaLogoTop.setImageDrawable(faceAnim)

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

    override fun showSteamGuard() {
        runOnUiThread {
            stopLoadingAnimation()
            loadingText.setText("")

            val constraintSet = ConstraintSet()
            constraintSet.clone(this, R.layout.activity_login_frame_steamguard)
            TransitionManager.beginDelayedTransition(rootLayout, AutoParallelTransition())
            constraintSet.applyTo(rootLayout)
        }
    }

    override fun showLoginForm() {
        runOnUiThread {
            stopLoadingAnimation()
            loadingText.setText("")

            val constraintSet = ConstraintSet()
            constraintSet.clone(this, R.layout.activity_login_frame_form)
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

            loadingText.setText("Failed to connect to steam")

            val constraintSet = ConstraintSet()
            constraintSet.clone(this, R.layout.activity_login_frame_failed)
            TransitionManager.beginDelayedTransition(rootLayout, AutoParallelTransition())
            constraintSet.applyTo(rootLayout)
        }
    }

    fun login() {
        val username = username.text.toString()

        if (Strings.isNullOrEmpty(username)) {
            usernameLayout.error = "You did not enter your username"
            return
        }

        val password = password.text.toString()

        if (Strings.isNullOrEmpty(password)) {
            passwordLayout.error = "You did not enter your password"
            return
        }

        Utils.hideKeyboardFrom(this@LoginActivity, login)

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
        handler.removeCallbacksAndMessages(null);

        val d = vapullaLogoMiddle.drawable as Animatable
        val d2 = vapullaLogoBottom.drawable as Animatable

        VectorAnimCompat.clearAnimationCallbacks(d)
        VectorAnimCompat.clearAnimationCallbacks(d2)

        d.stop()
        d2.stop()
    }
}

package `in`.dragonbra.vapulla.ui.login

import `in`.dragonbra.javasteam.util.Strings
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.anim.AutoParallelTransition
import `in`.dragonbra.vapulla.anim.TransitionListener
import `in`.dragonbra.vapulla.anim.VectorAnimCompat
import `in`.dragonbra.vapulla.extension.TextSwitcher
import `in`.dragonbra.vapulla.extension.bindLayout
import `in`.dragonbra.vapulla.extension.click
import `in`.dragonbra.vapulla.extension.hideKeyboardFrom
import `in`.dragonbra.vapulla.ui.VapullaFragment
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.graphics.drawable.Animatable
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.constraint.ConstraintSet
import android.support.graphics.drawable.Animatable2Compat
import android.support.transition.Transition
import android.support.transition.TransitionManager
import android.support.v4.content.ContextCompat.getDrawable
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.login_button.*
import kotlinx.android.synthetic.main.login_error_text.*
import kotlinx.android.synthetic.main.login_loading_text.*
import kotlinx.android.synthetic.main.login_logo_top.*
import kotlinx.android.synthetic.main.login_password.*
import kotlinx.android.synthetic.main.login_steam_guard_cancel.*
import kotlinx.android.synthetic.main.login_username.*
import org.jetbrains.anko.allCaps
import org.jetbrains.anko.image

/**
 * Fragment used for logging in the users.
 */
class LoginFragment : VapullaFragment() {

    private lateinit var viewModel: LoginViewModel

    private var frame = LoginViewModel.FRAME_SPLASH

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.login_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        // get the viewmodel using the injected viewmodel factory
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(LoginViewModel::class.java)

        // fancy text switcher with animations
        loadingText.setFactory(TextSwitcher.factory(context))
        loadingText.inAnimation = AnimationUtils.loadAnimation(context, android.R.anim.fade_in)
        loadingText.outAnimation = AnimationUtils.loadAnimation(context, android.R.anim.fade_out)

        username.bindLayout()
        password.bindLayout()

        // The login button is actually a multi purpose button used in multiple frames to reduce the
        // number of views in this fragment
        login.click {
            when (frame) {
            // try logging in ont he login frame
                LoginViewModel.FRAME_FORM -> login()

            // steam guard frame, submit the steam guard code
                LoginViewModel.FRAME_STEAMGUARD -> {
                    frame = LoginViewModel.FRAME_LOADING
                    val code = username.text.toString()

                    // display an error message if the steam guard code is not 5 char long
                    if (code.length < 5) {
                        usernameLayout.error = getString(R.string.editTextErrorSteamGuard)
                        return@click
                    }

                    context?.hideKeyboardFrom(it)

                    //change to the loading frame
                    val transition = AutoParallelTransition()
                    transition.addListener(object : TransitionListener() {
                        override fun onTransitionEnd(transition: Transition) {
                            // wait or the animation to finish to actually perform logging in
                            startLoadingAnimation()
                            viewModel.login(code)
                        }
                    })

                    val constraintSet = ConstraintSet()
                    constraintSet.clone(context, R.layout.activity_login_frame_loading)
                    TransitionManager.beginDelayedTransition(rootLayout, transition)
                    constraintSet.applyTo(rootLayout)
                }

                // failed screen, the button retries logging in
                LoginViewModel.FRAME_FAILED -> {
                    frame = LoginViewModel.FRAME_LOADING
                    loadingText.setText(null)

                    //change to the loading frame
                    val constraintSet = ConstraintSet()
                    constraintSet.clone(context, R.layout.activity_login_frame_loading)
                    TransitionManager.beginDelayedTransition(rootLayout, AutoParallelTransition())
                    constraintSet.applyTo(rootLayout)

                    val faceAnim = getDrawable(context!!, R.drawable.animated_vapulla_from_face)
                    vapullaLogo.setImageDrawable(faceAnim)

                    VectorAnimCompat.registerAnimationCallback(faceAnim as Animatable, object : Animatable2Compat.AnimationCallback() {
                        override fun onAnimationEnd(drawable: Drawable) {
                            // wait or the animation to finish to actually perform logging in
                            startLoadingAnimation()
                            viewModel.retry()
                        }
                    })

                    // change the face to the V logo
                    faceAnim.start()
                }
            }
        }

        steamGuardButtonCancel.click {
            viewModel.cancelSteamGuard()
        }

        // frame changes fired by steam service events
        viewModel.frame.observe(this, Observer {
            it?.let {
                frame = it
                when (it) {
                    LoginViewModel.FRAME_FORM -> showLoginForm(viewModel.errorMessage)
                    LoginViewModel.FRAME_STEAMGUARD -> showSteamGuard(viewModel.is2Fa, viewModel.errorMessage)
                    LoginViewModel.FRAME_FAILED -> showFailedScreen()
                    LoginViewModel.FRAME_LOADING -> startLoading(viewModel.loadingFinishedAction)
                }
            }
        })

        viewModel.loadingText.observe(this, Observer {
            showLoading(it ?: "")
        })

        viewModel.loginSuccess.observe(this, Observer {
            it?.let {
                if (it) {
                    loginSuccess()
                }
            }
        })
    }

    /**
     * Change the text of the loading text switcher
     *
     * @param text the text to switch to
     */
    private fun showLoading(text: String) {
        loadingText.setText(text)
    }

    /**
     * Change to the loading frame and start loading.
     *
     * @param finishedAction the action is invoked when the switching the frame is done
     */
    private fun startLoading(finishedAction: (() -> Unit)?) {
        val transition = AutoParallelTransition()

        // TODO this doesn't work, I don't remember why
        /*transition.addListener(object : TransitionListener() {
            override fun onTransitionEnd(transition: Transition) {
                startLoadingAnimation()
                finishedAction?.invoke()
            }
        })*/

        val constraintSet = ConstraintSet()
        constraintSet.clone(context, R.layout.activity_login_frame_loading)
        TransitionManager.beginDelayedTransition(rootLayout, transition)
        constraintSet.applyTo(rootLayout)

        startLoadingAnimation()
        finishedAction?.invoke()
    }

    /**
     * navigates to the home fragment
     */
    private fun loginSuccess() {
        findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
    }

    /**
     * Switch to the steam guard frame
     *
     * @param is2Fa        2 factor or not
     * @param errorMessage this message is displayed if not null
     */
    private fun showSteamGuard(is2Fa: Boolean, errorMessage: String?) {
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
        constraintSet.clone(context, layout)
        TransitionManager.beginDelayedTransition(rootLayout, AutoParallelTransition())
        constraintSet.applyTo(rootLayout)

        login.text = getString(R.string.buttonSteamGuard)
        username.setText("")
        username.inputType = InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        username.allCaps = true
        usernameLayout.hint = getString(R.string.editTextHintSteamGuard)
    }

    /**
     * Switch to the login form frame
     *
     * @param errorMessage this message is displayed if not null
     */
    private fun showLoginForm(errorMessage: String?) {
        stopLoadingAnimation()
        loadingText.setText("")

        errorMessage?.let {
            errorText.text = errorMessage
        }

        val layout = errorMessage?.let { R.layout.activity_login_frame_form_error }
                ?: run { R.layout.activity_login_frame_form }

        val constraintSet = ConstraintSet()
        constraintSet.clone(context, layout)
        TransitionManager.beginDelayedTransition(rootLayout, AutoParallelTransition())
        constraintSet.applyTo(rootLayout)

        login.text = getString(R.string.buttonLogin)
        password.setText("")
        username.setText("")
        username.inputType = InputType.TYPE_TEXT_VARIATION_PERSON_NAME
        username.allCaps = false
        usernameLayout.hint = getString(R.string.editTextHintUsername)
    }

    /**
     * Switch to the failed frame
     */
    private fun showFailedScreen() {
        stopLoadingAnimation()

        val faceAnim = getDrawable(context!!, R.drawable.animated_vapulla_to_face)
        vapullaLogo.setImageDrawable(faceAnim)
        (faceAnim as Animatable).start()

        loadingText.setText(getString(R.string.loadingTextFailed))

        val constraintSet = ConstraintSet()
        constraintSet.clone(context, R.layout.activity_login_frame_failed)
        TransitionManager.beginDelayedTransition(rootLayout, AutoParallelTransition())
        constraintSet.applyTo(rootLayout)

        login.text = getString(R.string.buttonRetry)
    }

    /**
     * Try logging in the user if none of the inputs are empty.
     */
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

        context?.hideKeyboardFrom(login)

        startLoading { viewModel.login(username, password) }
    }

    /**
     * Start the loading animation of the V logo.
     */
    private fun startLoadingAnimation() {
        val d = getDrawable(context!!, R.drawable.vapulla_loading) as AnimationDrawable
        vapullaLogo.setImageDrawable(d)
        d.start()
    }

    /**
     * Stop the loading animation of the V logo.
     */
    private fun stopLoadingAnimation() {
        (vapullaLogo.image as? AnimationDrawable)?.stop()
        vapullaLogo.setImageDrawable(getDrawable(context!!, R.drawable.vapulla))
    }
}

package `in`.dragonbra.vapulla.ui.login

import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.steam.handlers.steamuser.LogOnDetails
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOnCallback
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.extension.getErrorMessage
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.service.SteamService
import `in`.dragonbra.vapulla.threading.runOnBackgroundThread
import `in`.dragonbra.vapulla.viewmodel.VapullaViewModel
import android.app.Application
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import org.jetbrains.anko.info
import org.jetbrains.anko.startService
import org.jetbrains.anko.warn

/**
 * View model of the LoginFragment. Handle connection to steam servers, starting the steam service
 * logging in and steam guard.
 *
 * @param context must be application context
 * @param account the account manager
 */
class LoginViewModel(context: Application,
                     private val account: AccountManager) :
        VapullaViewModel(context) {

    companion object {
        // Possible frames of the fragment
        const val FRAME_SPLASH = 0
        const val FRAME_FAILED = 1
        const val FRAME_FORM = 2
        const val FRAME_LOADING = 3
        const val FRAME_STEAMGUARD = 4
    }

    private val logOnDetails = LogOnDetails()

    var is2Fa = false

    private var expectSteamGuard = false

    private val _frame: MutableLiveData<Int> = MutableLiveData()

    val frame: LiveData<Int>
        get() {
            if (_frame.value == null) {
                _frame.value = FRAME_SPLASH
            }

            return _frame
        }

    private val _loadingText: MutableLiveData<String> = MutableLiveData()

    val loadingText: LiveData<String>
        get() = _loadingText

    private val _loginSuccess: MutableLiveData<Boolean> = MutableLiveData()

    val loginSuccess: LiveData<Boolean>
        get() = _loginSuccess

    var errorMessage: String? = null

    var loadingFinishedAction: (() -> Unit)? = null

    override fun init() {
        super.init()

        subscribe(steamService?.subscribe<LoggedOnCallback> { onLoggedOn(it) })

        if (account.hasLoginKey()) {
            // the user has a login  key saved so we start connecting without showing the login form
            loadingFinishedAction = {
                logOnDetails.username = account.username
                logOnDetails.password = null
                logOnDetails.loginKey = account.loginKey
                startSteamService()
            }
            _frame.postValue(FRAME_LOADING)

        } else {
            if (!expectSteamGuard) {
                errorMessage = null
                _frame.postValue(FRAME_FORM)
            }
        }
    }

    override fun onConnected() {
        if (steamService?.isLoggedIn == true) {
            _loginSuccess.postValue(true)
            return
        }

        runOnBackgroundThread {
            steamService?.logOn(logOnDetails)
        }

        _loadingText.postValue(context.getString(R.string.loadingTextLoggingIn))
    }

    override fun onDisconnected() {
        if (!expectSteamGuard) {
            if (account.hasLoginKey()) {
                _frame.postValue(FRAME_FAILED)
            }
        }
    }

    private fun onLoggedOn(callback: LoggedOnCallback) {
        if (callback.result != EResult.OK) {
            if (callback.result == EResult.AccountLogonDenied || callback.result == EResult.AccountLoginDeniedNeedTwoFactor) {
                // The user has mobile or email 2factor enabled
                is2Fa = callback.result == EResult.AccountLoginDeniedNeedTwoFactor
                expectSteamGuard = true
                errorMessage = null
                _frame.postValue(FRAME_STEAMGUARD)
            } else {
                // Couldn't log in for whatever reason
                warn { "Failed to log in ${callback.result} / ${callback.extendedResult}" }
                expectSteamGuard = false

                errorMessage = context.getErrorMessage(callback.result, callback.extendedResult)
                if (callback.result == EResult.TwoFactorCodeMismatch || callback.result == EResult.InvalidLoginAuthCode) {
                    _frame.postValue(FRAME_STEAMGUARD)
                } else {
                    _frame.postValue(FRAME_FORM)
                }
            }
            // Disconnect from steam and restart with a clean connection later
            steamService?.disconnect()
            return
        }

        //Successful login, save the user name, the login key will be sent in a packet
        expectSteamGuard = false

        account.username = logOnDetails.username
        _loginSuccess.postValue(true)
    }

    /**
     * Log in the user
     *
     * @param username the username of the user
     * @param password the password of the user
     */
    fun login(username: String, password: String) {
        logOnDetails.username = username
        logOnDetails.password = password
        logOnDetails.loginKey = null

        startSteamService()
    }

    /**
     * Log in the user using the steam guard code. At this point the user name should be saved in
     * memory.
     *
     * @param steamGuardCode the entered steam guard code
     */
    fun login(steamGuardCode: String) {
        if (is2Fa) {
            logOnDetails.twoFactorCode = steamGuardCode
        } else {
            logOnDetails.authCode = steamGuardCode
        }

        startSteamService()
    }

    /**
     * Start the steam service that keeps the connection alive. Also initiate connection to servers
     * if not already connected.
     */
    private fun startSteamService() {
        info("Starting steam service...")
        context.startService<SteamService>()

        if (steamService?.isRunning != true) {
            steamService?.connect()

            _loadingText.postValue(context.getString(R.string.loadingTextConnecting))
        } else {
            onConnected()
        }
    }

    /**
     * Retry logging in after a failure.
     */
    fun retry() {
        if (account.hasLoginKey()) {
            logOnDetails.username = account.username
            logOnDetails.password = null
            logOnDetails.loginKey = account.loginKey
            startSteamService()
        }
    }

    /**
     * Go back to the login form from the steam guard form.
     */
    fun cancelSteamGuard() {
        expectSteamGuard = false

        errorMessage = null
        _frame.postValue(FRAME_FORM)
    }
}

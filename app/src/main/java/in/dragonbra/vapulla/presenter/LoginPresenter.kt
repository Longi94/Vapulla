package `in`.dragonbra.vapulla.presenter

import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.SteamFriends
import `in`.dragonbra.javasteam.steam.handlers.steamuser.LogOnDetails
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOnCallback
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.extension.getErrorMessage
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.service.SteamService
import `in`.dragonbra.vapulla.threading.runOnBackgroundThread
import `in`.dragonbra.vapulla.view.LoginView
import android.content.ComponentName
import android.content.Context
import android.os.IBinder
import org.jetbrains.anko.info
import org.jetbrains.anko.startService
import org.jetbrains.anko.warn

class LoginPresenter(context: Context) : VapullaPresenter<LoginView>(context) {

    private val logOnDetails = LogOnDetails()

    private val account = AccountManager(context)

    private var is2Fa = false

    private var expectSteamGuard = false

    override fun onServiceDisconnected(name: ComponentName) {
        info("Unbound from Steam service")
    }

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        info("Bound to Steam service")

        subscribe(steamService?.subscribe<LoggedOnCallback>({ onLoggedOn(it) }))

        if (account.hasLoginKey()) {
            ifViewAttached {
                it.startLoading({
                    logOnDetails.username = account.username
                    logOnDetails.password = null
                    logOnDetails.loginKey = account.loginKey
                    startSteamService()
                })
            }
        } else {
            if (!expectSteamGuard) {
                ifViewAttached { it.showLoginForm() }
            }
        }
    }

    override fun onConnected() {
        if (steamService?.isLoggedIn == true) {
            ifViewAttached {
                it.loginSuccess()
            }
            return
        }

        runOnBackgroundThread {
            steamService?.logOn(logOnDetails)
        }

        ifViewAttached {
            it.showLoading(context.getString(R.string.loadingTextLoggingIn))
        }
    }

    override fun onDisconnected() {
        if (!expectSteamGuard) {
            if (account.hasLoginKey()) {
                ifViewAttached { it.showFailedScreen() }
            }
        }
    }

    private fun onLoggedOn(callback: LoggedOnCallback) {
        if (callback.result != EResult.OK) {
            if (callback.result == EResult.AccountLogonDenied || callback.result == EResult.AccountLoginDeniedNeedTwoFactor) {
                is2Fa = callback.result == EResult.AccountLoginDeniedNeedTwoFactor
                expectSteamGuard = true
                ifViewAttached {
                    it.showSteamGuard()
                }
            } else {
                warn { "Failed to log in ${callback.result} / ${callback.extendedResult}" }
                expectSteamGuard = false

                val errorMessage = context.getErrorMessage(callback.result, callback.extendedResult)
                if (callback.result == EResult.TwoFactorCodeMismatch || callback.result == EResult.InvalidLoginAuthCode) {
                    ifViewAttached { it.showSteamGuard(errorMessage) }
                } else {
                    ifViewAttached { it.showLoginForm(errorMessage) }
                }
            }
            steamService?.disconnect()
            return
        }

        expectSteamGuard = false
        runOnBackgroundThread {
            steamService?.getHandler<SteamFriends>()?.setPersonaState(EPersonaState.Online)
        }

        ifViewAttached {
            account.username = logOnDetails.username
            it.loginSuccess()
        }
    }

    fun login(username: String, password: String) {
        logOnDetails.username = username
        logOnDetails.password = password
        logOnDetails.loginKey = null

        startSteamService()
    }

    fun login(steamGuardCode: String) {
        if (is2Fa) {
            logOnDetails.twoFactorCode = steamGuardCode
        } else {
            logOnDetails.authCode = steamGuardCode
        }

        startSteamService()
    }

    private fun startSteamService() {
        info("Starting steam service...")
        context.startService<SteamService>()

        if (steamService?.isRunning != true) {
            steamService?.connect()

            ifViewAttached {
                it.showLoading(context.getString(R.string.loadingTextConnecting))
            }
        } else {
            onConnected()
        }

    }

    fun retry() {
        if (account.hasLoginKey()) {
            logOnDetails.username = account.username
            logOnDetails.password = null
            logOnDetails.loginKey = account.loginKey
            startSteamService()
        }
    }
}
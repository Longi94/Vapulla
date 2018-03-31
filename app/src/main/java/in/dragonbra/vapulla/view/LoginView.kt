package `in`.dragonbra.vapulla.view

import com.hannesdorfmann.mosby3.mvp.MvpView

interface LoginView : MvpView {
    fun showLoading(text: String)
    fun loginSuccess()
    fun showSteamGuard()
    fun showLoginForm()
    fun showFailedScreen()
    fun startLoading(finishedAction: (() -> Unit)? = null)
}
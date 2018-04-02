package `in`.dragonbra.vapulla.view

import com.hannesdorfmann.mosby3.mvp.MvpView

interface LoginView : MvpView {
    fun showLoading(text: String)
    fun loginSuccess()
    fun showSteamGuard(errorMessage: String? = null)
    fun showLoginForm(errorMessage: String? = null)
    fun showFailedScreen()
    fun startLoading(finishedAction: (() -> Unit)? = null)
}
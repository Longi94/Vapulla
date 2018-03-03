package `in`.dragonbra.vapulla.view

import com.hannesdorfmann.mosby3.mvp.MvpView

interface LoginView : MvpView {
    fun showLoginScreen()

    fun onDisconnected()
}
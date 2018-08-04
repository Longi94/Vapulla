package `in`.dragonbra.vapulla.viewmodel

import `in`.dragonbra.vapulla.VapullaApplication
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.ui.home.HomeViewModel
import `in`.dragonbra.vapulla.ui.login.LoginViewModel
import android.app.Application
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider

class ViewModelFactory(private val application: Application,
                       private val account: AccountManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        val viewModel = with(modelClass) {
            when {
                isAssignableFrom(LoginViewModel::class.java) -> LoginViewModel(application, account)
                isAssignableFrom(HomeViewModel::class.java) -> HomeViewModel()
                else ->
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        } as T

        if ((application as VapullaApplication).steamService != null) {
            (viewModel as VapullaViewModel).init()
        }

        return viewModel
    }
}
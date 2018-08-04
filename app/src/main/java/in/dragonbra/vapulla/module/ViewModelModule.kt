package `in`.dragonbra.vapulla.module

import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.viewmodel.ViewModelFactory
import android.app.Application
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Dagger module for view model stuff
 */
@Module
class ViewModelModule {

    /**
     * Provide a single view model factory for all the fragments
     */
    @Provides
    @Singleton
    fun provideViewModelFactory(application: Application, account: AccountManager) =
            ViewModelFactory(application, account)
}
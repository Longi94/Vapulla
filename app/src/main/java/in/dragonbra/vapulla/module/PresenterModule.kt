package `in`.dragonbra.vapulla.module

import `in`.dragonbra.vapulla.data.dao.SteamFriendDao
import `in`.dragonbra.vapulla.presenter.HomePresenter
import `in`.dragonbra.vapulla.presenter.LoginPresenter
import android.content.Context
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class PresenterModule {

    @Provides
    @Singleton
    fun provideHomePresenter(context: Context, steamFriendDao: SteamFriendDao) =
            HomePresenter(context, steamFriendDao)

    @Provides
    @Singleton
    fun provideLoginPresenter(context: Context) =
            LoginPresenter(context)
}
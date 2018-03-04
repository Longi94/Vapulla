package `in`.dragonbra.vapulla.component

import `in`.dragonbra.vapulla.activity.HomeActivity
import `in`.dragonbra.vapulla.activity.LoginActivity
import `in`.dragonbra.vapulla.module.AppModule
import `in`.dragonbra.vapulla.module.PresenterModule
import `in`.dragonbra.vapulla.module.StorageModule
import `in`.dragonbra.vapulla.service.SteamService
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class, PresenterModule::class, StorageModule::class])
interface VapullaComponent {
    fun inject(steamService: SteamService)
    fun inject(homeActivity: HomeActivity)
    fun inject(loginActivity: LoginActivity)
}
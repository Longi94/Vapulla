package `in`.dragonbra.vapulla.component

import `in`.dragonbra.vapulla.activity.*
import `in`.dragonbra.vapulla.module.*
import `in`.dragonbra.vapulla.service.SteamService
import `in`.dragonbra.vapulla.ui.VapullaFragment
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class, PresenterModule::class, StorageModule::class,
    RetrofitModule::class, SteamModule::class, ViewModelModule::class])
interface VapullaComponent {
    fun inject(steamService: SteamService)
    fun inject(homeActivity: HomeActivity)
    fun inject(loginActivity: LoginActivity)
    fun inject(chatActivity: ChatActivity)
    fun inject(settingsActivity: SettingsActivity)
    fun inject(webActivity: WebActivity)
    fun inject(vapullaFragment: VapullaFragment)
}
package `in`.dragonbra.vapulla.module

import `in`.dragonbra.javasteam.steam.discovery.FileServerListProvider
import `in`.dragonbra.javasteam.steam.handlers.steamapps.SteamApps
import `in`.dragonbra.javasteam.steam.handlers.steamcloud.SteamCloud
import `in`.dragonbra.javasteam.steam.handlers.steamgamecoordinator.SteamGameCoordinator
import `in`.dragonbra.javasteam.steam.handlers.steamgameserver.SteamGameServer
import `in`.dragonbra.javasteam.steam.handlers.steammasterserver.SteamMasterServer
import `in`.dragonbra.javasteam.steam.handlers.steamscreenshots.SteamScreenshots
import `in`.dragonbra.javasteam.steam.handlers.steamuserstats.SteamUserStats
import `in`.dragonbra.javasteam.steam.handlers.steamworkshop.SteamWorkshop
import `in`.dragonbra.javasteam.steam.steamclient.SteamClient
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackManager
import `in`.dragonbra.javasteam.steam.steamclient.configuration.SteamConfiguration
import `in`.dragonbra.javasteam.steam.webapi.WebAPI
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.steam.SteamWebAuth
import `in`.dragonbra.vapulla.steam.VapullaHandler
import android.content.Context
import dagger.Module
import dagger.Provides
import java.io.File
import javax.inject.Named
import javax.inject.Singleton

@Module
class SteamModule {

    companion object {
        /**
         * Store CM servers in this file.
         */
        const val SERVERS_FILE = "servers.bin"
    }

    @Provides
    @Singleton
    fun provideSteamConfig(context: Context): SteamConfiguration = SteamConfiguration.create {
        it.withServerListProvider(FileServerListProvider(File(context.filesDir, SERVERS_FILE)))
    }

    @Provides
    @Singleton
    fun provideSteamClient(config: SteamConfiguration): SteamClient {
        val steamClient = SteamClient(config)
        steamClient.addHandler(VapullaHandler())

        steamClient.removeHandler(SteamApps::class.java)
        steamClient.removeHandler(SteamCloud::class.java)
        steamClient.removeHandler(SteamGameCoordinator::class.java)
        steamClient.removeHandler(SteamGameServer::class.java)
        steamClient.removeHandler(SteamMasterServer::class.java)
        steamClient.removeHandler(SteamScreenshots::class.java)
        steamClient.removeHandler(SteamUserStats::class.java)
        steamClient.removeHandler(SteamWorkshop::class.java)

        return steamClient
    }

    @Provides
    @Singleton
    fun provideCallbackMgr(client: SteamClient) = CallbackManager(client)

    @Provides
    @Singleton
    @Named("ISteamUserAuth")
    fun provideProvideSteamUserAuth(config: SteamConfiguration): WebAPI = config.getWebAPI("ISteamUserAuth")

    @Provides
    @Singleton
    fun provideSteamAuth(@Named("ISteamUserAuth") api: WebAPI,
                         account: AccountManager) = SteamWebAuth(api, account)
}
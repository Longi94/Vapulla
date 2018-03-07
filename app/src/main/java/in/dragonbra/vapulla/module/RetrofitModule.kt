package `in`.dragonbra.vapulla.module

import `in`.dragonbra.vapulla.BuildConfig
import `in`.dragonbra.vapulla.retrofit.ISteamUserStats
import `in`.dragonbra.vapulla.retrofit.StoreFront
import `in`.dragonbra.vapulla.retrofit.interceptor.QueryParamInterceptor
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
class RetrofitModule {

    companion object {
        const val BASE_STEAM_URL = "http://api.steampowered.com/"
        const val BASE_STEAM_STORE_URL = "http://store.steampowered.com/api/"
    }

    @Provides
    @Singleton
    fun provideClient(): OkHttpClient = OkHttpClient.Builder()
            .addInterceptor(QueryParamInterceptor("key", BuildConfig.STEAM_WEB_API_KEY))
            .build()

    @Provides
    @Singleton
    fun provideSteamUserStats(client: OkHttpClient): ISteamUserStats {
        val retrofit = Retrofit.Builder()
                .baseUrl("${BASE_STEAM_URL}ISteamUserStats/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

        return retrofit.create(ISteamUserStats::class.java)
    }

    @Provides
    @Singleton
    fun provideStoreFront(): StoreFront {
        val retrofit = Retrofit.Builder()
                .baseUrl(BASE_STEAM_STORE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

        return retrofit.create(StoreFront::class.java)
    }
}
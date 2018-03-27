package `in`.dragonbra.vapulla.module

import `in`.dragonbra.vapulla.retrofit.StoreFront
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
class RetrofitModule {

    companion object {
        const val BASE_STEAM_STORE_URL = "http://store.steampowered.com/api/"
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
package `in`.dragonbra.vapulla.retrofit

import `in`.dragonbra.vapulla.retrofit.response.GameSchemaResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ISteamUserStats {

    @GET("GetSchemaForGame/v2")
    fun getSchemaForGame(@Query("appId") appId: Int): Call<GameSchemaResponse>
}
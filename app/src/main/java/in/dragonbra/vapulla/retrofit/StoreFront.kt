package `in`.dragonbra.vapulla.retrofit

import `in`.dragonbra.vapulla.retrofit.response.AppDetailsResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface StoreFront {

    @GET("appdetails")
    fun getAppDetails(@Query("appids") appId: Int): Call<Map<Int, AppDetailsResponse>>
}
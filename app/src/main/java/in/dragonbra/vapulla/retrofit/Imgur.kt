package `in`.dragonbra.vapulla.retrofit

import `in`.dragonbra.vapulla.retrofit.response.ImgurToken
import retrofit2.Call
import retrofit2.http.POST
import retrofit2.http.Query

interface Imgur {

    @POST("oauth2/token")
    fun refreshToken(@Query("refresh_token") refreshToken: String,
                     @Query("client_id") clientId: String,
                     @Query("client_secret") clientSecret: String,
                     @Query("grant_type") grantType: String): Call<ImgurToken>
}
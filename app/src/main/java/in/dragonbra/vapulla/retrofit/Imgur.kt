package `in`.dragonbra.vapulla.retrofit

import `in`.dragonbra.vapulla.retrofit.response.ImgurToken
import `in`.dragonbra.vapulla.retrofit.response.ImgurUploadResult
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface Imgur {

    @POST("oauth2/token")
    fun refreshToken(@Query("refresh_token") refreshToken: String,
                     @Query("client_id") clientId: String,
                     @Query("client_secret") clientSecret: String,
                     @Query("grant_type") grantType: String): Call<ImgurToken>

    @Multipart
    @POST("3/image")
    fun postImage(@Part("image") body: RequestBody,
                  @Header("Authorization") auth: String): Call<ImgurUploadResult>
}
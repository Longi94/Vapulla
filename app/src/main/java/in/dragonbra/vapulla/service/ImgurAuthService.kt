package `in`.dragonbra.vapulla.service

import `in`.dragonbra.javasteam.util.Strings
import `in`.dragonbra.vapulla.BuildConfig
import `in`.dragonbra.vapulla.retrofit.Imgur
import `in`.dragonbra.vapulla.retrofit.response.ImgurToken
import android.content.Context
import android.net.Uri
import android.preference.PreferenceManager
import android.util.Log
import okhttp3.HttpUrl
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.math.BigInteger
import java.security.SecureRandom

class ImgurAuthService(context: Context, val imgur: Imgur) {

    companion object {
        val TAG = ImgurAuthService::class.simpleName

        const val KEY_IMGUR_ACCESS_TOKEN = "imgur_access_token"
        const val KEY_IMGUR_REFRESH_TOKEN = "imgur_refresh_token"
        const val KEY_IMGUR_USERNAME = "imgur_username"
        const val KEY_IMGUR_ACCOUNT_ID = "imgur_account_id"
        const val KEY_IMGUR_LAST_UPDATE = "imgur_last_update"
    }

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    private val random = SecureRandom()

    private var state: String = ""

    fun getAuthUrl(): String {
        state = BigInteger(130, random).toString()

        val url = HttpUrl.Builder()
                .scheme("https")
                .host("api.imgur.com")
                .addPathSegments("oauth2/authorize")
                .addQueryParameter("client_id", BuildConfig.IMGUR_CLIENT_ID)
                .addQueryParameter("state", state)
                .addQueryParameter("response_type", "token")
                .build()

        return url.toString()
    }

    fun authorize(uri: Uri): Boolean {
        val state = uri.getQueryParameter("state")

        if (state != this.state) {
            Log.d(TAG, "got state $state expected ${this.state}")
            return false
        }

        this.state = ""

        val paramsUri = Uri.parse("vapulla://authorize?${uri.fragment}")

        val accessToken = paramsUri.getQueryParameter("access_token")

        if (Strings.isNullOrEmpty(accessToken)) {
            Log.d(TAG, "missing access_token")
            return false
        }

        val refreshToken = paramsUri.getQueryParameter("refresh_token")

        if (Strings.isNullOrEmpty(refreshToken)) {
            Log.d(TAG, "missing refresh_token")
            return false
        }

        val expiresIn = paramsUri.getQueryParameter("expires_in")

        if (Strings.isNullOrEmpty(refreshToken)) {
            Log.d(TAG, "missing expires_in")
            return false
        }

        val accountUsername = paramsUri.getQueryParameter("account_username")

        if (Strings.isNullOrEmpty(refreshToken)) {
            Log.d(TAG, "missing refresh_token")
            return false
        }

        val accountId = paramsUri.getQueryParameter("account_id")

        if (Strings.isNullOrEmpty(refreshToken)) {
            Log.d(TAG, "missing refresh_token")
            return false
        }

        prefs.edit()
                .putString(KEY_IMGUR_ACCESS_TOKEN, accessToken)
                .putString(KEY_IMGUR_REFRESH_TOKEN, refreshToken)
                .putString(KEY_IMGUR_USERNAME, accountUsername)
                .putString(KEY_IMGUR_ACCOUNT_ID, accountId)
                .putLong(KEY_IMGUR_LAST_UPDATE, System.currentTimeMillis())
                .apply()

        return true
    }

    fun getUsername(): String = prefs.getString(KEY_IMGUR_USERNAME, "")

    fun clear() {
        prefs.edit()
                .remove(KEY_IMGUR_ACCESS_TOKEN)
                .remove(KEY_IMGUR_REFRESH_TOKEN)
                .remove(KEY_IMGUR_USERNAME)
                .remove(KEY_IMGUR_ACCOUNT_ID)
                .remove(KEY_IMGUR_LAST_UPDATE)
                .apply()
    }

    fun refreshTokenIfNeeded() {
        if (prefs.getLong(KEY_IMGUR_LAST_UPDATE, 0L) < System.currentTimeMillis() - 1728000000L) {
            val call = imgur.refreshToken(
                    prefs.getString(KEY_IMGUR_REFRESH_TOKEN, null),
                    BuildConfig.IMGUR_CLIENT_ID,
                    BuildConfig.IMGUR_CLIENT_SECRET,
                    "refresh_token"
            )

            call.enqueue(object : Callback<ImgurToken> {
                override fun onFailure(call: Call<ImgurToken>, t: Throwable?) {
                }

                override fun onResponse(call: Call<ImgurToken>, response: Response<ImgurToken>) {
                    val token = response.body()

                    token?.let {
                        prefs.edit()
                                .putString(KEY_IMGUR_ACCESS_TOKEN, it.access_token)
                                .putLong(KEY_IMGUR_LAST_UPDATE, System.currentTimeMillis())
                                .apply()
                    }
                }
            })
        }
    }

    fun authorized(): Boolean = prefs.contains(KEY_IMGUR_ACCESS_TOKEN)

    fun postImage(body: RequestBody) =
            imgur.postImage(body, "Bearer ${prefs.getString(KEY_IMGUR_ACCESS_TOKEN, "")}")
}
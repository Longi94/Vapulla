package `in`.dragonbra.vapulla.steam

import `in`.dragonbra.javasteam.steam.CMClient
import `in`.dragonbra.javasteam.steam.webapi.WebAPI
import `in`.dragonbra.javasteam.util.KeyDictionary
import `in`.dragonbra.javasteam.util.WebHelpers
import `in`.dragonbra.javasteam.util.crypto.CryptoHelper
import `in`.dragonbra.javasteam.util.crypto.RSACrypto
import `in`.dragonbra.vapulla.manager.AccountManager
import android.webkit.CookieManager
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.spongycastle.util.encoders.Hex

class SteamWebAuth(private val steamUserAuth: WebAPI, private val account: AccountManager): AnkoLogger {

    companion object {
        const val MOBILE_VERSION_ID = 3922515
        const val MOBILE_VERSION_NAME = "2.3.1"
    }

    private var sessionId: String? = null

    private var token: String? = null

    private var tokenSecure: String? = null

    private var steamId: Long = 0L

    private var authenticated: Boolean = false

    fun authenticate(client: CMClient, nonce: String) {
        info { "attempting web authentication" }
        authenticated = false

        steamId = client.steamID?.convertToUInt64() ?: return

        // get the public key
        val publicKey = KeyDictionary.getPublicKey(client.universe) ?: return

        // generate a session id
        sessionId = Hex.toHexString(CryptoHelper.generateRandomBlock(4))

        // generate a sessionkey
        val sessionKey = CryptoHelper.generateRandomBlock(32)

        // encrypt the session key
        val rsa = RSACrypto(publicKey)
        val encryptedSessionKey = rsa.encrypt(sessionKey)

        // encrypt the nonce
        val loginKey = ByteArray(20)
        System.arraycopy(nonce.toByteArray(), 0, loginKey, 0, nonce.length)
        val encryptedLoginKey = CryptoHelper.symmetricEncrypt(loginKey, sessionKey)

        val params = mutableMapOf(
                "steamid" to steamId.toString(),
                "sessionkey" to WebHelpers.urlEncode(encryptedSessionKey),
                "encrypted_loginkey" to WebHelpers.urlEncode(encryptedLoginKey)
        )

        val result = steamUserAuth.call("POST", "AuthenticateUser", params)

        token = result["token"].asString()
        tokenSecure = result["tokenSecure"].asString()

        authenticated = true

        info { "web authentication successful" }
    }

    fun updateCookies() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)

        if (authenticated) {
            cookieManager.setSteamCookie("sessionid", sessionId)
            cookieManager.setSteamCookie("steamLogin", token)
            cookieManager.setSteamCookie("steamLoginSecure", tokenSecure)
            cookieManager.setSteamCookie("steamMachineAuth$steamId", Hex.toHexString(account.readSentryFile()))
            cookieManager.setSteamCookie("mobileClient", "android")
            cookieManager.setSteamCookie("mobileClientVersion", "$MOBILE_VERSION_ID+%28$MOBILE_VERSION_NAME%29")

            cookieManager.flush()
        }
    }

    private fun CookieManager.setSteamCookie(key: String, value: String?) {
        val cookie = "$key=$value"
        this.setCookie("https://steamcommunity.com", cookie)
        this.setCookie("https://store.steampowered.com", cookie)
    }
}
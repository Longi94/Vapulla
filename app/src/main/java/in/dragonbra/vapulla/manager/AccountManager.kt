package `in`.dragonbra.vapulla.manager

import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.PersonaState
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.UpdateMachineAuthCallback
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import org.spongycastle.util.encoders.Hex
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.security.MessageDigest

class AccountManager(private val context: Context) {

    companion object {
        private const val KEY_LOGIN_KEY = "account_login_key"
        private const val KEY_UNIQUE_ID = "account_unique_id"
        private const val KEY_USERNAME = "account_username"
        private const val KEY_NICKNAME = "account_nickname"
        private const val KEY_AVATAR_HASH = "account_avatar_hash"
        private const val KEY_STEAM_ID = "account_steam_id"
        private const val KEY_STATE = "account_state"

        private const val SENTRY_FILE_NAME = "sentry.bin"
    }

    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    private val editor: SharedPreferences.Editor = prefs.edit()

    private val listeners = mutableSetOf<AccountManagerListener>()

    var loginKey: String?
        get() = prefs.getString(KEY_LOGIN_KEY, null)
        set(value) = editor.putString(KEY_LOGIN_KEY, value).apply()

    var uniqueId: Int
        get() = prefs.getInt(KEY_UNIQUE_ID, 0)
        set(value) = editor.putInt(KEY_UNIQUE_ID, value).apply()

    var username: String?
        get() = prefs.getString(KEY_USERNAME, null)
        set(value) = editor.putString(KEY_USERNAME, value).apply()

    var nickname: String?
        get() = prefs.getString(KEY_NICKNAME, null)
        set(value) = editor.putString(KEY_NICKNAME, value).apply()

    var steamId: Long
        get() = prefs.getLong(KEY_STEAM_ID, 0L)
        set(value) = editor.putLong(KEY_STEAM_ID, value).apply()

    var avatarHash: String?
        get() = prefs.getString(KEY_AVATAR_HASH, null)
        set(value) = editor.putString(KEY_AVATAR_HASH, value).apply()

    var state: EPersonaState
        get() = EPersonaState.from(prefs.getInt(KEY_STATE, 0))
        set(value) = editor.putInt(KEY_STATE, value.code()).apply()

    var sentrySize: Long
        get() = File(context.filesDir, SENTRY_FILE_NAME).length()
        private set(value) {}

    fun updateSentryFile(callback: UpdateMachineAuthCallback) {
        val sentryFile = File(context.filesDir, SENTRY_FILE_NAME)
        FileOutputStream(sentryFile).use {
            val channel = it.channel;
            channel.position(callback.offset.toLong())
            channel.write(ByteBuffer.wrap(callback.data, 0, callback.bytesToWrite))
        }
    }

    fun clear() {
        editor.remove(KEY_LOGIN_KEY)
                .remove(KEY_UNIQUE_ID)
                .remove(KEY_USERNAME)
                .remove(KEY_STEAM_ID)
                .remove(KEY_AVATAR_HASH)
                .remove(KEY_NICKNAME)
                .remove(KEY_STATE)
                .apply()
    }

    fun hasLoginKey() = prefs.contains(KEY_LOGIN_KEY)

    fun hasSentryFile() = File(context.filesDir, SENTRY_FILE_NAME).exists()

    fun readSentryFile(): ByteArray {
        val file = File(context.filesDir, SENTRY_FILE_NAME)

        val digest = MessageDigest.getInstance("SHA-1")

        val buffer = ByteArray(8192)
        var n = 0;

        FileInputStream(file).use {
            while (n != -1) {
                n = it.read(buffer)

                if (n > 0) {
                    digest.update(buffer, 0, n)
                }
            }
        }

        return digest.digest()
    }

    fun saveLocalUser(state: PersonaState) {
        avatarHash = Hex.toHexString(state.avatarHash)
        nickname = state.name
        steamId = state.friendID.convertToUInt64()
        this.state = state.state

        listeners.forEach {
            it.unAccountUpdate(this@AccountManager)
        }
    }

    fun addListener(l: AccountManagerListener) = listeners.add(l)

    fun removeListener(l: AccountManagerListener) = listeners.remove(l)

    interface AccountManagerListener {
        fun unAccountUpdate(account: AccountManager)
    }
}
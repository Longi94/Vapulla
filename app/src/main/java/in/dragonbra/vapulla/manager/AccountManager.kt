package `in`.dragonbra.vapulla.manager

import `in`.dragonbra.javasteam.steam.handlers.steamfriends.PersonaState
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import org.spongycastle.util.encoders.Hex
import java.io.*

class AccountManager(private val context: Context) {

    companion object {
        private const val KEY_LOGIN_KEY = "account_login_key"
        private const val KEY_UNIQUE_ID = "account_unique_id"
        private const val KEY_USERNAME = "account_username"
        private const val KEY_AVATAR_HASH = "account_avatar_hash"
        private const val KEY_STEAM_ID = "account_avatar_hash"

        private const val SENTRY_FILE_NAME = "sentry.bin"
    }

    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    private val editor: SharedPreferences.Editor = prefs.edit()

    var loginKey: String?
        get() = prefs.getString(KEY_LOGIN_KEY, null)
        set(value) = editor.putString(KEY_LOGIN_KEY, value).apply()

    var uniqueId: Int
        get() = prefs.getInt(KEY_UNIQUE_ID, 0)
        set(value) = editor.putInt(KEY_UNIQUE_ID, value).apply()

    var username: String?
        get() = prefs.getString(KEY_USERNAME, null)
        set(value) = editor.putString(KEY_USERNAME, value).apply()

    var steamId: Long
        get() = prefs.getLong(KEY_STEAM_ID, 0L)
        set(value) = editor.putLong(KEY_STEAM_ID, value).apply()

    var avatarHash: String?
        get() = prefs.getString(KEY_AVATAR_HASH, null)
        set(value) = editor.putString(KEY_AVATAR_HASH, value).apply()

    var sentry: ByteArray
        get() = readSentryFile()
        set(value) {
            val sentryFile = File(context.filesDir, SENTRY_FILE_NAME)

            val bos = BufferedOutputStream(FileOutputStream(sentryFile, false))
            bos.write(value)
            bos.flush()
            bos.close()
        }

    fun clear() {
        editor.remove(KEY_LOGIN_KEY)
                .remove(KEY_UNIQUE_ID)
                .remove(KEY_USERNAME)
                .remove(KEY_STEAM_ID)
                .remove(KEY_AVATAR_HASH)
                .apply()
    }

    fun hasLoginKey() = prefs.contains(KEY_LOGIN_KEY)

    fun hasSentryFile() = File(context.filesDir, SENTRY_FILE_NAME).exists()

    private fun readSentryFile(): ByteArray {
        val file = File(context.filesDir, SENTRY_FILE_NAME)
        val size = file.length()
        val bytes = ByteArray(size.toInt())

        val bis = BufferedInputStream(FileInputStream(file))
        bis.read(bytes)

        return bytes
    }

    fun saveLocalUser(state: PersonaState) {
        avatarHash = Hex.toHexString(state.avatarHash)
        username = state.name
        steamId = state.friendID.convertToUInt64()
    }
}
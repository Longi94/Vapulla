package `in`.dragonbra.vapulla.manager

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import java.io.*

class AccountManager(private val context: Context) {

    companion object {
        private const val KEY_LOGIN_KEY = "account_login_key"
        private const val KEY_UNIQUE_ID = "account_unique_id"
        private const val KEY_USERNAME = "account_username"

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
}
package `in`.dragonbra.vapulla.activity

import `in`.dragonbra.javasteam.steam.steamclient.callbacks.DisconnectedCallback
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.VapullaApplication
import `in`.dragonbra.vapulla.data.VapullaDatabase
import `in`.dragonbra.vapulla.extension.click
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.service.ImgurAuthService
import `in`.dragonbra.vapulla.service.SteamService
import `in`.dragonbra.vapulla.threading.runOnBackgroundThread
import android.content.*
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.preference.*
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v4.app.NavUtils
import android.support.v7.app.AlertDialog
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import android.view.ViewGroup
import org.jetbrains.anko.*
import java.io.Closeable
import java.util.*
import javax.inject.Inject

/**
 * A [PreferenceActivity] that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 *
 * See [Android Design: Settings](http://developer.android.com/design/patterns/settings.html)
 * for design guidelines and the [Settings API Guide](http://developer.android.com/guide/topics/ui/settings.html)
 * for more information on developing a Settings UI.
 */
class SettingsActivity : AppCompatPreferenceActivity() {

    @Inject
    lateinit var imgurAuthService: ImgurAuthService

    @Inject
    lateinit var accountManager: AccountManager

    @Inject
    lateinit var db: VapullaDatabase

    private lateinit var prefs: SharedPreferences

    private lateinit var steamService: SteamService

    private val subs: MutableList<Closeable?> = LinkedList()

    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName) {
        }

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as SteamService.SteamBinder
            steamService = binder.getService()
            subs.add(steamService.subscribe<DisconnectedCallback>({ onDisconnected() }))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as VapullaApplication).graph.inject(this)

        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        val v = find<ViewGroup>(android.R.id.list).parent.parent.parent as ViewGroup
        val toolbar = layoutInflater.inflate(R.layout.toolbar, v, false) as Toolbar?

        v.addView(toolbar, 0)

        setSupportActionBar(find(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupPreferences()

        val uri = intent.data

        if (uri != null && uri.host == "authorize") {
            val success = imgurAuthService.authorize(uri)

            if (success) {
                updateImgurPref()
                Snackbar.make(v, getString(R.string.snackbarImgurLinked), Snackbar.LENGTH_SHORT).show()
            } else {
                Snackbar.make(v, getString(R.string.snackbarImgurLinkFailed), Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        bindService(intentFor<SteamService>(), connection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        unbindService(connection)
    }

    override fun onMenuItemSelected(featureId: Int, item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            if (!super.onMenuItemSelected(featureId, item)) {
                NavUtils.navigateUpFromSameTask(this)
            }
            return true
        }
        return super.onMenuItemSelected(featureId, item)
    }

    @Suppress("DEPRECATION")
    private fun setupPreferences() {
        addPreferencesFromResource(R.xml.pref_general)

        val chatBubble = findPreference("pref_chat_bubble") as SwitchPreference

        chatBubble.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newVal ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (newVal as Boolean && !Settings.canDrawOverlays(this@SettingsActivity)) {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
                    return@OnPreferenceChangeListener false
                }
            }
            true
        }

        updateImgurPref()

        val changeUserPreference = findPreference("pref_change_user")
        changeUserPreference.summary = getString(R.string.prefSummaryChangeUser, accountManager.username)
        changeUserPreference.click {
            val builder = AlertDialog.Builder(this)

            builder.setMessage(getString(R.string.dialogMessageChangeUser))
                    .setTitle(getString(R.string.dialogTitleChangeUser))
                    .setPositiveButton(R.string.dialogYes, { _, _ ->
                        runOnBackgroundThread {
                            runOnBackgroundThread { steamService.disconnect() }
                            clearData()
                        }
                    })
                    .setNegativeButton(R.string.dialogNo, null)

            builder.create().show()
            true
        }
    }

    fun clearData() {
        accountManager.clear()
        imgurAuthService.clear()

        db.steamFriendDao().delete()
        db.chatMessageDao().delete()
        db.emoticonDao().delete()
    }

    fun onDisconnected() {
        startActivity(intentFor<LoginActivity>().newTask().clearTask())
    }

    private fun updateImgurPref() {
        val pref = findPreference("pref_imgur")
        if (prefs.contains(ImgurAuthService.KEY_IMGUR_USERNAME)) {
            pref.title = getString(R.string.prefTitleImgurLinked)
            pref.summary = getString(R.string.prefSummaryImgurLinked, imgurAuthService.getUsername())

            findPreference("pref_imgur").setOnPreferenceClickListener {
                imgurAuthService.clear()
                updateImgurPref()
                true
            }
        } else {
            pref.title = getString(R.string.prefTitleImgur)
            pref.summary = null

            findPreference("pref_imgur").setOnPreferenceClickListener {
                browse(imgurAuthService.getAuthUrl())
            }
        }
    }

    companion object {

        /**
         * A preference value change listener that updates the preference's summary
         * to reflect its new value.
         */
        private val sBindPreferenceSummaryToValueListener = Preference.OnPreferenceChangeListener { preference, value ->
            val stringValue = value.toString()

            if (preference is ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                val listPreference = preference
                val index = listPreference.findIndexOfValue(stringValue)

                // Set the summary to reflect the new value.
                preference.setSummary(
                        if (index >= 0)
                            listPreference.entries[index]
                        else
                            null)

            }/* else if (preference is RingtonePreference) {
                // For ringtone preferences, look up the correct display value
                // using RingtoneManager.
                if (TextUtils.isEmpty(stringValue)) {
                    // Empty values correspond to 'silent' (no ringtone).
                    preference.setSummary(R.string.pref_ringtone_silent)

                } else {
                    val ringtone = RingtoneManager.getRingtone(
                            preference.getContext(), Uri.parse(stringValue))

                    if (ringtone == null) {
                        // Clear the summary if there was a lookup error.
                        preference.setSummary(null)
                    } else {
                        // Set the summary to reflect the new ringtone display
                        // name.
                        val name = ringtone.getTitle(preference.getContext())
                        preference.setSummary(name)
                    }
                }

            } */ else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.summary = stringValue
            }
            true
        }

        /**
         * Binds a preference's summary to its value. More specifically, when the
         * preference's value is changed, its summary (line of text below the
         * preference title) is updated to reflect the value. The summary is also
         * immediately updated upon calling this method. The exact display format is
         * dependent on the type of preference.

         * @see .sBindPreferenceSummaryToValueListener
         */
        private fun bindPreferenceSummaryToValue(preference: Preference) {
            // Set the listener to watch for value changes.
            preference.onPreferenceChangeListener = sBindPreferenceSummaryToValueListener

            // Trigger the listener immediately with the preference's
            // current value.
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.context)
                            .getString(preference.key, ""))
        }
    }
}

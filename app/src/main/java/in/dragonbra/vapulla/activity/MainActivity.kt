package `in`.dragonbra.vapulla.activity

import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.steam.handlers.steamuser.LogOnDetails
import `in`.dragonbra.javasteam.steam.handlers.steamuser.SteamUser
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOffCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOnCallback
import `in`.dragonbra.javasteam.steam.steamclient.SteamClient
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackManager
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.ConnectedCallback
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.DisconnectedCallback
import `in`.dragonbra.vapulla.R
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    companion object {
        val TAG = MainActivity::class.qualifiedName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        login.setOnClickListener({
            login()
        })
    }

    private fun login() {
        ConnectTask(username.text.toString(), password.text.toString()).execute()
    }

    private class ConnectTask(val username: String, val password: String) : AsyncTask<Void, Void, Void>() {
        private var steamUser: SteamUser? = null

        private var isRunning: Boolean = false

        override fun doInBackground(vararg params: Void?): Void? {

            // create our steamclient instance
            val steamClient = SteamClient()

            // create the callback manager which will route callbacks to function calls
            val manager = CallbackManager(steamClient)

            // get the steamuser handler, which is used for logging on after successfully connecting
            steamUser = steamClient.getHandler(SteamUser::class.java)

            // register a few callbacks we're interested in
            // these are registered upon creation to a callback manager, which will then route the callbacks
            // to the functions specified
            manager.subscribe(ConnectedCallback::class.java, this::onConnected)
            manager.subscribe(DisconnectedCallback::class.java, this::onDisconnected)

            manager.subscribe(LoggedOnCallback::class.java, this::onLoggedOn)
            manager.subscribe(LoggedOffCallback::class.java, this::onLoggedOff)

            isRunning = true

            Log.d(TAG, "Connecting to steam...")

            // initiate the connection
            steamClient.connect()

            // create our callback handling loop
            while (isRunning) {
                // in order for the callbacks to get routed, they need to be handled by the manager
                manager.runWaitCallbacks(1000L)
            }

            return null
        }

        private fun onConnected(callback: ConnectedCallback) {
            Log.d(TAG, "Connected to Steam! Logging in ${username}...")

            val details = LogOnDetails()
            details.username = username
            details.password = password

            steamUser?.logOn(details)
        }

        private fun onDisconnected(callback: DisconnectedCallback) {
            Log.d(TAG, "Disconnected from Steam")
            isRunning = false
        }

        private fun onLoggedOn(callback: LoggedOnCallback) {
            if (callback.result != EResult.OK) {
                if (callback.result == EResult.AccountLogonDenied) {
                    // if we recieve AccountLogonDenied or one of it's flavors (AccountLogonDeniedNoMailSent, etc)
                    // then the account we're logging into is SteamGuard protected
                    // see sample 5 for how SteamGuard can be handled
                    println("Unable to logon to Steam: This account is SteamGuard protected.")
                    isRunning = false
                    return
                }

                println("Unable to logon to Steam: " + callback.result)
                isRunning = false
                return

            }
            println("Successfully logged on!")

            // at this point, we'd be able to perform actions on Steam

            // for this sample we'll just log off
            steamUser?.logOff()
        }

        private fun onLoggedOff(callback: LoggedOffCallback) {
            Log.d(TAG, "Logged off of Steam: " + callback.result)
            isRunning = false
        }
    }
}

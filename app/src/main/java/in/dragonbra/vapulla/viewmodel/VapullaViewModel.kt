package `in`.dragonbra.vapulla.viewmodel

import `in`.dragonbra.javasteam.steam.steamclient.callbacks.ConnectedCallback
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.DisconnectedCallback
import `in`.dragonbra.vapulla.VapullaApplication
import `in`.dragonbra.vapulla.service.SteamService
import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.support.annotation.CallSuper
import android.support.v4.content.LocalBroadcastManager
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import java.io.Closeable
import java.util.*

/**
 * Abstract view model that every fragment that uses anything steam related should inherit.
 *
 * @param context Application to make sure it's not a UI related context to avoid memory leaks
 */
abstract class VapullaViewModel(val context: Application) :
        AndroidViewModel(context), AnkoLogger {

    companion object {
        /**
         * The action for the broadcast when the activity binds to the [SteamService]
         */
        const val SERVICE_BOUND_ACTION = "in.dragonbra.vapulla.broadcast.SERVICE_BOUND"
    }

    val steamService: SteamService?
        get() = (context as VapullaApplication).steamService

    /**
     * Callback subscriptions that will be closed when the view model is destroyed
     */
    private val subs: MutableList<Closeable?> = LinkedList()

    private val serviceBoundReceiver = ServiceBoundReceiver()

    init {
        info { "registering broadcast receiver" }
        LocalBroadcastManager.getInstance(context).registerReceiver(serviceBoundReceiver, IntentFilter(SERVICE_BOUND_ACTION))
    }

    /**
     * Save a subscription of a callback. It will be closed when the view model is
     * destroyed.
     *
     * @param sub the [Closeable] object that you get when subscribing to a callback.
     */
    fun subscribe(sub: Closeable?) {
        subs.add(sub)
    }

    @CallSuper
    override fun onCleared() {
        info { "unregistering broadcast receiver" }
        LocalBroadcastManager.getInstance(context).unregisterReceiver(serviceBoundReceiver)
        subs.forEach { it?.close() }
        subs.clear()
    }

    /**
     * Called when we are sure that the steamService is not null (when VapullaActivity successfully
     * binds to the service or when this object is created (already bound)
     */
    @CallSuper
    open fun init() {
        subs.add(steamService?.subscribe<ConnectedCallback> { onConnected() })
        subs.add(steamService?.subscribe<DisconnectedCallback> { onDisconnected() })
    }

    /**
     * Called when the device is connected to the steam servers
     */
    open fun onConnected() {}

    /**
     * Called when the device is fully disconnected from the steam servers. It's not called when
     * the service reconnects.
     */
    open fun onDisconnected() {}

    private inner class ServiceBoundReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            info { "received service bound broadcast" }
            init()
        }
    }
}
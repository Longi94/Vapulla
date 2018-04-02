package `in`.dragonbra.vapulla.activity

import `in`.dragonbra.vapulla.VapullaApplication
import `in`.dragonbra.vapulla.presenter.VapullaPresenter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.annotation.CallSuper
import com.hannesdorfmann.mosby3.mvp.MvpActivity
import com.hannesdorfmann.mosby3.mvp.MvpPresenter
import com.hannesdorfmann.mosby3.mvp.MvpView
import org.jetbrains.anko.AnkoLogger

abstract class VapullaBaseActivity<V : MvpView, P : MvpPresenter<V>> : MvpActivity<V, P>(), AnkoLogger {

    companion object {
        const val STOP_INTENT = "in.dragonbra.vapulla.SERVICE_STOP"
    }

    private val stopReceiver = StopReceiver()

    fun vapulla() = application as VapullaApplication

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val filter = IntentFilter(STOP_INTENT)
        registerReceiver(stopReceiver, filter)
    }

    @CallSuper
    override fun onStart() {
        super.onStart()
        (presenter as? VapullaPresenter<*>)?.onStart()
    }

    @CallSuper
    override fun onResume() {
        super.onResume()
        (presenter as? VapullaPresenter<*>)?.onResume()
    }

    @CallSuper
    override fun onPause() {
        super.onPause()
        (presenter as? VapullaPresenter<*>)?.onPause()
    }

    @CallSuper
    override fun onStop() {
        super.onStop()
        (presenter as? VapullaPresenter<*>)?.onStop()
    }

    @CallSuper
    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(stopReceiver)
        } catch (ignored: Exception) {
        }
        (presenter as? VapullaPresenter<*>)?.onDestroy()
    }

    inner class StopReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            finishAffinity()
        }
    }
}
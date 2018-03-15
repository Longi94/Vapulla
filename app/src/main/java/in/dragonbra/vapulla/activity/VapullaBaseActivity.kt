package `in`.dragonbra.vapulla.activity

import `in`.dragonbra.vapulla.VapullaApplication
import `in`.dragonbra.vapulla.presenter.VapullaPresenter
import com.hannesdorfmann.mosby3.mvp.MvpActivity
import com.hannesdorfmann.mosby3.mvp.MvpPresenter
import com.hannesdorfmann.mosby3.mvp.MvpView
import org.jetbrains.anko.AnkoLogger

abstract class VapullaBaseActivity<V : MvpView, P : MvpPresenter<V>> : MvpActivity<V, P>(), AnkoLogger {
    fun vapulla() = application as VapullaApplication

    override fun onStart() {
        super.onStart()
        (presenter as? VapullaPresenter<*>)?.onStart()
    }

    override fun onResume() {
        super.onResume()
        (presenter as? VapullaPresenter<*>)?.onResume()
    }

    override fun onPause() {
        super.onPause()
        (presenter as? VapullaPresenter<*>)?.onPause()
    }

    override fun onStop() {
        super.onStop()
        (presenter as? VapullaPresenter<*>)?.onStop()
    }
}
package `in`.dragonbra.vapulla.activity

import `in`.dragonbra.vapulla.R.id.toolbar
import `in`.dragonbra.vapulla.VapullaApplication
import `in`.dragonbra.vapulla.presenter.VapullaPresenter
import android.os.Bundle
import android.os.PersistableBundle
import com.hannesdorfmann.mosby3.mvp.MvpActivity
import com.hannesdorfmann.mosby3.mvp.MvpPresenter
import com.hannesdorfmann.mosby3.mvp.MvpView
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.find

abstract class VapullaBaseActivity<V : MvpView, P : MvpPresenter<V>> : MvpActivity<V, P>(), AnkoLogger {
    fun vapulla() = application as VapullaApplication

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        setSupportActionBar(find(toolbar))
    }

    override fun onStart() {
        super.onStart()
        (presenter as VapullaPresenter<*>).onStart()
    }

    override fun onResume() {
        super.onResume()
        (presenter as VapullaPresenter<*>).onResume()
    }

    override fun onPause() {
        super.onPause()
        (presenter as VapullaPresenter<*>).onPause()
    }

    override fun onStop() {
        super.onStop()
        (presenter as VapullaPresenter<*>).onStop()
    }
}
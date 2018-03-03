package `in`.dragonbra.vapulla.activity

import android.view.View
import com.hannesdorfmann.mosby3.mvp.MvpActivity
import com.hannesdorfmann.mosby3.mvp.MvpPresenter
import com.hannesdorfmann.mosby3.mvp.MvpView
import org.jetbrains.anko.AnkoLogger

fun View.click(l: (v: View) -> Unit) {
    this.setOnClickListener { l(it) }
}

abstract class VapullaBaseActivity<V : MvpView, P : MvpPresenter<V>> : MvpActivity<V, P>(), AnkoLogger
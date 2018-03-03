package `in`.dragonbra.vapulla.activity

import android.view.View
import com.hannesdorfmann.mosby3.mvp.MvpActivity
import com.hannesdorfmann.mosby3.mvp.MvpPresenter
import com.hannesdorfmann.mosby3.mvp.MvpView
import org.jetbrains.anko.AnkoLogger

abstract class VapullaBaseActivity<V : MvpView, P : MvpPresenter<V>> : MvpActivity<V, P>(), AnkoLogger
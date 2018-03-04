package `in`.dragonbra.vapulla.presenter

import com.hannesdorfmann.mosby3.mvp.MvpBasePresenter
import com.hannesdorfmann.mosby3.mvp.MvpView

abstract class VapullaPresenter<V : MvpView> : MvpBasePresenter<V>() {
    open fun onStart() {}
    open fun onResume() {}
    open fun onPause() {}
    open fun onStop() {}
}
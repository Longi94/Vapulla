package `in`.dragonbra.vapulla.extension

import `in`.dragonbra.vapulla.threading.RunnableAsyncTask
import android.os.AsyncTask
import com.hannesdorfmann.mosby3.mvp.MvpBasePresenter
import com.hannesdorfmann.mosby3.mvp.MvpView

fun <V : MvpView> MvpBasePresenter<V>.runOnBackgroundThread(r: () -> Unit) {
    RunnableAsyncTask(r).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
}
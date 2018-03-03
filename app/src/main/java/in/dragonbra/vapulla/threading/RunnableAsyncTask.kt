package `in`.dragonbra.vapulla.threading

import android.os.AsyncTask

fun runOnBackgroundThread(r: () -> Unit) {
    RunnableAsyncTask(r).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
}

class RunnableAsyncTask(val r: () -> Unit) : AsyncTask<Unit, Unit, Unit>() {
    override fun doInBackground(vararg params: Unit?) = r()
}
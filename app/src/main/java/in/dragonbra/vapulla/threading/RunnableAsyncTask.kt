package `in`.dragonbra.vapulla.threading

import android.os.AsyncTask

class RunnableAsyncTask(val r: () -> Unit) : AsyncTask<Unit, Unit, Unit>() {
    override fun doInBackground(vararg params: Unit?) = r()
}
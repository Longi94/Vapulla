package `in`.dragonbra.vapulla.extension

import android.content.Context
import android.view.Gravity
import android.widget.TextView
import android.widget.ViewSwitcher

object TextSwitcher {
    fun factory(context: Context?): ViewSwitcher.ViewFactory {
        return ViewSwitcher.ViewFactory {
            val tv = TextView(context)
            tv.gravity = Gravity.CENTER_HORIZONTAL
            tv
        }
    }
}
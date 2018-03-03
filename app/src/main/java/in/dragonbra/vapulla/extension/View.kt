package `in`.dragonbra.vapulla.extension

import android.view.View

fun View.click(l: (v: View) -> Unit) {
    this.setOnClickListener { l(it) }
}
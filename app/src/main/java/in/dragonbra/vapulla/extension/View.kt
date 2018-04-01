package `in`.dragonbra.vapulla.extension

import android.view.View

fun View.click(l: (v: View) -> Unit) {
    this.setOnClickListener(l)
}

fun View.longClick(l: (v: View) -> Boolean) {
    this.setOnLongClickListener(l)
}

fun View.hide() {
    this.visibility = View.GONE
}

fun View.invisible() {
    this.visibility = View.INVISIBLE
}

fun View.show() {
    this.visibility = View.VISIBLE
}

fun View.toggleVisibility() {
    this.visibility = if (this.visibility == View.VISIBLE) View.GONE else View.VISIBLE
}

fun View.isVisible() = this.visibility == View.VISIBLE
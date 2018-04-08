package `in`.dragonbra.vapulla.extension

import android.graphics.Typeface
import android.widget.TextView

fun TextView.bold() {
    if (this.typeface.style != Typeface.BOLD) {
        this.setTypeface(this.typeface, Typeface.BOLD)
    }
}

fun TextView.normal() {
    if (this.typeface.style != Typeface.NORMAL) {
        this.setTypeface(Typeface.create(this.typeface, Typeface.NORMAL), Typeface.NORMAL)
    }
}

fun TextView.maxLineWidth(): Float {
    var res = 0.0f
    for (i in 0..(this.lineCount - 1)) {
        val width = this.layout.getLineWidth(i)
        if (res < width) {
            res = width
        }
    }
    return res
}
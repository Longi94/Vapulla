package `in`.dragonbra.vapulla.extension

import android.graphics.Typeface
import android.widget.TextView

fun TextView.bold() {
    this.setTypeface(this.typeface, Typeface.BOLD)
}

fun TextView.normal() {
    this.setTypeface(Typeface.create(this.typeface, Typeface.NORMAL), Typeface.NORMAL)
}
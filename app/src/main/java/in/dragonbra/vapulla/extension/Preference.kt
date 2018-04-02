package `in`.dragonbra.vapulla.extension

import android.preference.Preference

fun Preference.click(l: (preference: Preference) -> Boolean) {
    this.setOnPreferenceClickListener(l)
}
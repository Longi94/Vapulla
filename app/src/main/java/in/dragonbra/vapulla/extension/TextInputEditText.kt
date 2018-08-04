package `in`.dragonbra.vapulla.extension

import android.support.design.widget.TextInputEditText
import android.support.design.widget.TextInputLayout
import android.text.Editable
import android.text.TextWatcher

/**
 * Helper method to hide the error message of a TextInputLayout when the text is changed.
 */
fun TextInputEditText.bindLayout() {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            (this@bindLayout.parent.parent as? TextInputLayout)?.error = null
        }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }
    })
}
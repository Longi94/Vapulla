package `in`.dragonbra.vapulla.extension

import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.vapulla.R
import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager

fun Context.getErrorMessage(result: EResult, extendedResult: EResult? = null): String =
        when (result) {
            EResult.NoConnection -> getString(R.string.errorMessageLostConnection)
            EResult.InvalidPassword -> getString(R.string.errorMessageInvalidPassword)
            EResult.TwoFactorCodeMismatch -> getString(R.string.errorMessageTwoFactorCodeMismatch)
            EResult.InvalidLoginAuthCode -> getString(R.string.errorMessageInvalidLoginAuthCode)
            else -> result.toString()
        }

fun Context.hideKeyboardFrom(view: View) {
    val imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(view.windowToken, 0)
}

fun Context.showKeyboard(view: View) {
    val imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
}
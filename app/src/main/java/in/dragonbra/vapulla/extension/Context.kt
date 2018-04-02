package `in`.dragonbra.vapulla.extension

import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.vapulla.R
import android.content.Context

fun Context.getErrorMessage(result: EResult, extendedResult: EResult? = null): String =
        when (result) {
            EResult.NoConnection -> getString(R.string.errorMessageLostConnection)
            EResult.InvalidPassword -> getString(R.string.errorMessageInvalidPassword)
            EResult.TwoFactorCodeMismatch -> getString(R.string.errorMessageTwoFactorCodeMismatch)
            EResult.InvalidLoginAuthCode -> getString(R.string.errorMessageInvalidLoginAuthCode)
            else -> result.toString()
        }
package `in`.dragonbra.vapulla.util

import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.util.Strings
import `in`.dragonbra.vapulla.R
import android.content.Context
import android.support.v4.content.ContextCompat
import com.bumptech.glide.request.RequestOptions

object Utils {
    val avatarOptions = RequestOptions()
            .transform(CircleTransform())

    fun getAvatarUrl(avatar: String?) = if (avatar == null || Strings.isNullOrEmpty(avatar) || avatar == "0000000000000000000000000000000000000000") {
        "http://cdn.akamai.steamstatic.com/steamcommunity/public/images/avatars/fe/fef49e7fa7e1997310d705b2a6158ff8dc1cdfeb_full.jpg"
    } else {
        "http://cdn.akamai.steamstatic.com/steamcommunity/public/images/avatars/${avatar.substring(0, 2)}/${avatar}_full.jpg"
    }

    fun getStatusColor(context: Context, state: EPersonaState?, gameName: String?) = if (Strings.isNullOrEmpty(gameName)) {
        when (state) {
            EPersonaState.Online -> ContextCompat.getColor(context, R.color.statusOnline)
            EPersonaState.Busy -> ContextCompat.getColor(context, R.color.statusBusy)
            EPersonaState.Away, EPersonaState.Snooze -> ContextCompat.getColor(context, R.color.statusAway)
            EPersonaState.LookingToTrade, EPersonaState.LookingToPlay -> ContextCompat.getColor(context, R.color.statusLookingTo)
            else -> ContextCompat.getColor(context, R.color.statusOffline)
        }
    } else {
        ContextCompat.getColor(context, R.color.statusInGame)
    }

    fun getStatusText(context: Context, state: EPersonaState?, gameName: String?) = if (Strings.isNullOrEmpty(gameName)) {
        when (state) {
            EPersonaState.Online -> context.getString(R.string.statusOnline)
            EPersonaState.Busy -> context.getString(R.string.statusBusy)
            EPersonaState.Away -> context.getString(R.string.statusAway)
            EPersonaState.Snooze -> context.getString(R.string.statusSnooze)
            EPersonaState.LookingToTrade -> context.getString(R.string.statusLookingTrade)
            EPersonaState.LookingToPlay -> context.getString(R.string.statusLookingPlay)
            else ->context.getString(R.string.statusOffline)
        }
    } else {
        context.getString(R.string.statusPlaying, gameName)
    }
}
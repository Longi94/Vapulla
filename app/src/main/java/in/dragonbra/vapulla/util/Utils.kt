package `in`.dragonbra.vapulla.util

import `in`.dragonbra.javasteam.util.Strings
import com.bumptech.glide.request.RequestOptions

object Utils {
    val avatarOptions = RequestOptions()
            .transform(CircleTransform())

    fun getAvatarUrl(avatar: String?) = if (avatar == null || Strings.isNullOrEmpty(avatar) || avatar == "0000000000000000000000000000000000000000") {
        "http://cdn.akamai.steamstatic.com/steamcommunity/public/images/avatars/fe/fef49e7fa7e1997310d705b2a6158ff8dc1cdfeb_full.jpg"
    } else {
        "http://cdn.akamai.steamstatic.com/steamcommunity/public/images/avatars/${avatar.substring(0, 2)}/${avatar}_full.jpg"
    }
}

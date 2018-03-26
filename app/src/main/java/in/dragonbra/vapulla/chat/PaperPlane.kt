package `in`.dragonbra.vapulla.chat

import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.util.Patterns
import android.widget.TextView
import com.bumptech.glide.Glide
import java.util.*
import java.util.regex.Pattern

class PaperPlane(val context: Context, val emoteSizeDp: Float) {

    companion object {
        val EMOTE_PATTERN = Pattern.compile("\\u02D0([a-zA-Z0-9]+)\\u02D0")
    }

    private val targets: MutableMap<TextView, MutableList<EmoteTarget>> = HashMap()

    fun load(view: TextView, message: String, showUrl: Boolean) {
        clear(view)

        val spannable = SpannableString(message)

        if (showUrl) {
            val urlMatcher = Patterns.WEB_URL.matcher(message)
            while (urlMatcher.find()) {
                val result = urlMatcher.toMatchResult()

                val nextSpace = message.indexOf(' ', result.start())
                val end = if (nextSpace == -1) message.length else nextSpace
                spannable.setSpan(URLSpan(result.group()), result.start(), end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            view.movementMethod = LinkMovementMethod.getInstance()
        }

        view.text = spannable
        view.requestLayout()

        val emoteMatcher = EMOTE_PATTERN.matcher(message)

        while (emoteMatcher.find()) {
            if (!targets.containsKey(view)) {
                targets[view] = LinkedList()
            }

            val result = emoteMatcher.toMatchResult()

            val emote = result.group(1)

            val target = EmoteTarget(context, view, spannable, result.start(), result.end(), emoteSizeDp, targets[view])

            Glide.with(context)
                    .asBitmap()
                    .load("https://steamcommunity-a.akamaihd.net/economy/emoticon/:$emote:")
                    .into(target)
        }
    }

    fun clear(view: TextView) {
        view.text = ""

        targets[view]?.forEach { it.cancel() }
        targets[view]?.clear()
    }

    fun clearAll() {
        targets.entries.forEach {
            it.value.forEach {
                it.cancel()
            }
        }
        targets.clear()
    }
}
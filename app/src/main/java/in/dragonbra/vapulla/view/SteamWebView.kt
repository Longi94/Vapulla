package `in`.dragonbra.vapulla.view

import android.content.Context
import android.util.AttributeSet
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.browse
import org.jetbrains.anko.info

class SteamWebView : WebView, AnkoLogger {

    var webListener: WebClientListener? = null

    private val chromeClient = object : WebChromeClient() {
        /*override fun onReceivedTitle(view: WebView?, title: String?) {
            webListener?.onTitle(title)
        }*/

        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            webListener?.onProgressChanged(newProgress)
        }
    }

    private val webClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val uri = request.url
            when (uri.scheme) {
                "steammobile" -> {
                    val steamId = uri.getQueryParameter("steamid")?.toLong()
                    when (uri.host) {
                        "chat" -> webListener?.openChat(steamId)
                        "settitle" -> webListener?.onTitle(uri.getQueryParameter("title"))
                        "reloadpage" -> reload()
                        "openurl", "opencategoryurl" -> loadUrl(uri.getQueryParameter("url"))
                        "openexternalurl" -> context.browse(uri.getQueryParameter("url"))
                        "closethis" -> webListener?.finish()
                        else -> info { "Unhandled steam mobile action: ${uri.host}" }
                    }

                }
                "http", "https" -> {
                    if (uri.host == "steamcommunity.com" || uri.host == "steampowered.com") {
                        loadUrl(uri.toString())
                    } else {
                        context.browse(uri.toString())
                    }
                }
                else -> context.browse(uri.toString())
            }

            return true
        }
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr)

    init {
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.javaScriptEnabled = true

        webChromeClient = chromeClient
        webViewClient = webClient
    }

    interface WebClientListener {
        fun onProgressChanged(newProgress: Int)
        fun onTitle(title: String?)
        fun openChat(steamId: Long?)
        fun finish()
    }
}
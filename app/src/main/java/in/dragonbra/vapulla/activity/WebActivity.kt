package `in`.dragonbra.vapulla.activity

import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.VapullaApplication
import `in`.dragonbra.vapulla.extension.click
import `in`.dragonbra.vapulla.extension.hide
import `in`.dragonbra.vapulla.extension.show
import `in`.dragonbra.vapulla.steam.SteamWebAuth
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.*
import android.webkit.*
import android.widget.ImageView
import android.widget.PopupWindow
import kotlinx.android.synthetic.main.activity_web.*
import org.jetbrains.anko.find
import javax.inject.Inject

class WebActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_URL = "url"
    }

    @Inject
    lateinit var steamWebAuth: SteamWebAuth

    private val chromeClient = object : WebChromeClient() {
        override fun onReceivedTitle(view: WebView?, title: String?) {
            this@WebActivity.title = title
        }

        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            if (newProgress < 100) {
                progressBar.show()
            } else {
                progressBar.hide()
            }
            progressBar.progress = newProgress
        }
    }

    private val webClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            view.loadUrl(request.url.toString())
            supportActionBar?.subtitle = request.url.toString()
            return true
        }
    }

    private lateinit var webControls: View

    private lateinit var forwardButton: ImageView

    private lateinit var popup: PopupWindow

    override fun onCreate(savedInstanceState: Bundle?) {
        (application as VapullaApplication).graph.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web)

        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        webControls = inflater.inflate(R.layout.web_controls, null)
        webControls.find<ImageView>(R.id.refreshButton).click { refresh() }
        forwardButton = webControls.find(R.id.forwardButton)
        forwardButton.click { forward() }

        popup = PopupWindow(this)

        popup.isFocusable = true
        popup.width = WindowManager.LayoutParams.WRAP_CONTENT
        popup.height = WindowManager.LayoutParams.WRAP_CONTENT
        popup.contentView = webControls

        val url = intent.getStringExtra(EXTRA_URL)
                ?: throw IllegalStateException("missing url extra")

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.subtitle = url

        progressBar.max = 100

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)

        if (steamWebAuth.authenticated) {
            steamWebAuth.buildCookies().forEach { key, value ->
                val cookie = "$key=$value"
                cookieManager.setCookie("https://steamcommunity.com", cookie)
                cookieManager.setCookie("https://store.steampowered.com", cookie)
            }

            cookieManager.flush()
        }

        webView.settings.builtInZoomControls = true
        webView.settings.displayZoomControls = false
        webView.settings.javaScriptEnabled = true

        webView.webChromeClient = chromeClient
        webView.webViewClient = webClient
        webView.loadUrl(url)
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_web, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            finish()
            true
        }
        R.id.more -> {
            popupControls(find(R.id.more))
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            finish()
        }
    }

    private fun popupControls(v: View) {
        if (!popup.isShowing) {
            if (webView.canGoForward()) {
                forwardButton.imageTintList = ColorStateList.valueOf(-1)
                forwardButton.isClickable = true
            } else {
                forwardButton.imageTintList = ColorStateList.valueOf(0x64ffffff)
                forwardButton.isClickable = false
            }
            popup.showAsDropDown(v)
        }
    }

    private fun refresh() {
        popup.dismiss()
        webView.reload()
    }

    private fun forward() {
        if (webView.canGoForward()) {
            webView.goForward()
            popup.dismiss()
        }
    }
}

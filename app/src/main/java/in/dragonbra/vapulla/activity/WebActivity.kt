package `in`.dragonbra.vapulla.activity

import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.VapullaApplication
import `in`.dragonbra.vapulla.extension.click
import `in`.dragonbra.vapulla.extension.hide
import `in`.dragonbra.vapulla.extension.show
import `in`.dragonbra.vapulla.steam.SteamWebAuth
import `in`.dragonbra.vapulla.view.SteamWebView
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.*
import android.widget.ImageView
import android.widget.PopupWindow
import kotlinx.android.synthetic.main.activity_web.*
import org.jetbrains.anko.find
import org.jetbrains.anko.startActivity
import javax.inject.Inject

class WebActivity : AppCompatActivity(), SteamWebView.WebClientListener {

    companion object {
        const val EXTRA_URL = "url"
    }

    @Inject
    lateinit var steamWebAuth: SteamWebAuth

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
        title = ""

        progressBar.max = 100

        steamWebAuth.updateCookies()

        webView.webListener = this
        webView.loadUrl(url)
    }

    override fun onDestroy() {
        super.onDestroy()
        webView.webListener = null
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

    override fun onProgressChanged(newProgress: Int) {
        if (newProgress < 100) {
            progressBar.show()
        } else {
            progressBar.hide()
        }
        progressBar.progress = newProgress
    }

    override fun onTitle(title: String?) {
        this.title = title
    }

    override fun openChat(steamId: Long?) {
        steamId?.let {
            startActivity<ChatActivity>(ChatActivity.INTENT_STEAM_ID to it)
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

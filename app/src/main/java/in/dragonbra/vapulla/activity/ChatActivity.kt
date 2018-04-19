package `in`.dragonbra.vapulla.activity

import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.javasteam.util.Strings
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.adapter.ChatAdapter
import `in`.dragonbra.vapulla.adapter.EmoteAdapter
import `in`.dragonbra.vapulla.adapter.FriendListItem
import `in`.dragonbra.vapulla.chat.PaperPlane
import `in`.dragonbra.vapulla.data.dao.ChatMessageDao
import `in`.dragonbra.vapulla.data.dao.EmoticonDao
import `in`.dragonbra.vapulla.data.dao.SteamFriendDao
import `in`.dragonbra.vapulla.data.entity.ChatMessage
import `in`.dragonbra.vapulla.data.entity.Emoticon
import `in`.dragonbra.vapulla.extension.*
import `in`.dragonbra.vapulla.manager.GameSchemaManager
import `in`.dragonbra.vapulla.presenter.ChatPresenter
import `in`.dragonbra.vapulla.service.ImgurAuthService
import `in`.dragonbra.vapulla.util.Utils
import `in`.dragonbra.vapulla.util.recyclerview.ChatAdapterDataObserver
import `in`.dragonbra.vapulla.view.ChatView
import android.annotation.SuppressLint
import android.arch.paging.PagedList
import android.content.ClipboardManager
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.NavUtils
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.PopupMenu
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.dialog_nickname.view.*
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.textColor
import javax.inject.Inject


class ChatActivity : VapullaBaseActivity<ChatView, ChatPresenter>(), ChatView, TextWatcher,
        PopupMenu.OnMenuItemClickListener, EmoteAdapter.EmoteListener {

    companion object {
        const val INTENT_STEAM_ID = "steam_id"

        const val REQUEST_IMAGE_GET = 100
    }

    @Inject
    lateinit var chatMessageDao: ChatMessageDao

    @Inject
    lateinit var steamFriendDao: SteamFriendDao

    @Inject
    lateinit var emoticonDao: EmoticonDao

    @Inject
    lateinit var imgurAuthService: ImgurAuthService

    @Inject
    lateinit var schemaManager: GameSchemaManager

    @Inject
    lateinit var clipboard: ClipboardManager

    private lateinit var paperPlane: PaperPlane

    private lateinit var chatAdapter: ChatAdapter

    private lateinit var emoteAdapter: EmoteAdapter

    private lateinit var layoutManager: LinearLayoutManager

    private var scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            val firstVisible = layoutManager.findFirstVisibleItemPosition()
            if (firstVisible == 0 && scrollDownButton.isShown) {
                presenter.firstVisible(true)
                scrollDownButton.hide()
            } else if (firstVisible != 0 && !scrollDownButton.isShown) {
                presenter.firstVisible(false)
                scrollDownButton.show()
            }
        }
    }

    private val fabPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var fabBaseline = 0.0f

    private var fabHeight = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        vapulla().graph.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        paperPlane = PaperPlane(this, 18.0f)
        chatAdapter = ChatAdapter(this, paperPlane, clipboard)

        layoutManager = LinearLayoutManager(this)
        layoutManager.reverseLayout = true
        chatList.layoutManager = layoutManager
        chatList.adapter = chatAdapter
        chatList.addOnScrollListener(scrollListener)

        chatAdapter.registerAdapterDataObserver(ChatAdapterDataObserver(
                chatAdapter,
                layoutManager,
                chatList
        ))

        emoteAdapter = EmoteAdapter(this, this)

        val emoteLayoutManager = FlexboxLayoutManager(this)
        emoteLayoutManager.flexDirection = FlexDirection.ROW
        emoteLayoutManager.justifyContent = JustifyContent.CENTER

        emoteList.layoutManager = emoteLayoutManager
        emoteList.adapter = emoteAdapter

        messageBox.addTextChangedListener(this)
        messageBox.setOnClickListener { emoteList.hide() }

        moreButton.setOnClickListener {
            val popup = PopupMenu(this@ChatActivity, it)
            popup.menuInflater.inflate(R.menu.menu_chat, popup.menu)
            popup.show()
            popup.setOnMenuItemClickListener(this@ChatActivity)
        }

        fabPaint.textSize = Utils.convertDpToPixel(12.0f, this)
        fabPaint.color = -1
        fabPaint.textAlign = Paint.Align.LEFT

        fabBaseline = -fabPaint.ascent()
        fabHeight = (fabBaseline + fabPaint.descent() + 0.0f).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        paperPlane.clearAll()
    }

    override fun createPresenter(): ChatPresenter {
        val steamId = SteamID(intent.getLongExtra(INTENT_STEAM_ID, 0L))
        return ChatPresenter(this, chatMessageDao, steamFriendDao, emoticonDao, imgurAuthService, schemaManager, steamId)
    }

    override fun closeApp() {
        runOnUiThread {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.addCategory(Intent.CATEGORY_HOME)
            startActivity(intent)
            finish()
        }
    }

    override fun showChat(list: PagedList<ChatMessage>?) {
        chatAdapter.submitList(list)
    }

    override fun updateFriendData(friend: FriendListItem?) {
        if (friend == null) {
            return
        }
        runOnUiThread {
            Glide.with(this@ChatActivity)
                    .load(Utils.getAvatarUrl(friend.avatar))
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .apply(Utils.avatarOptions)
                    .into(friendAvatar)

            val state = EPersonaState.from(friend.state ?: 0)
            friendUsername.text = friend.name

            if (Strings.isNullOrEmpty(friend.nickname)) {
                friendNickname.hide()
            } else {
                friendNickname.show()
                friendNickname.text = getString(R.string.nicknameFormat, friend.nickname)
            }

            if ((friend.lastMessageTime == null || friend.typingTs > friend.lastMessageTime!!)
                    && friend.typingTs > System.currentTimeMillis() - 20000L) {
                friendStatus.text = getString(R.string.statusTyping)
                friendStatus.textColor = ContextCompat.getColor(this@ChatActivity, R.color.colorAccent)
                friendStatus.bold()
            } else {
                friendStatus.text = Utils.getStatusText(this@ChatActivity, state, friend.gameAppId, friend.gameName, friend.lastLogOff)
                friendStatus.textColor = ContextCompat.getColor(this@ChatActivity, android.R.color.secondary_text_dark)
                friendStatus.normal()
            }

            (friend.newMessageCount ?: 0).let {
                if (it > 0) {
                    val text = if (it > 99) ":D" else it.toString()
                    scrollDownButton.setImageBitmap(textToBitmap(text))
                    scrollDownButton.backgroundTintList = ColorStateList.valueOf(
                            ContextCompat.getColor(this, R.color.colorAccent))
                } else {
                    scrollDownButton.setImageDrawable(getDrawable(R.drawable.ic_chevron_double_down))
                    scrollDownButton.backgroundTintList = ColorStateList.valueOf(
                            ContextCompat.getColor(this, R.color.colorPrimaryLight))
                }
            }
        }
    }

    private fun textToBitmap(text: String): Bitmap {
        val width = (fabPaint.measureText(text) + 0.0f).toInt()
        val image = Bitmap.createBitmap(width, fabHeight, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(image)
        canvas.drawText(text, 0.0f, fabBaseline, fabPaint)
        return image
    }

    override fun navigateUp() {
        hideKeyboardFrom(messageBox)
        NavUtils.navigateUpFromSameTask(this)
    }

    override fun afterTextChanged(s: Editable?) {
        presenter.typing()
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        if (s.isEmpty()) {
            imageButton.show()
        } else {
            imageButton.hide()
        }
    }

    override fun onMenuItemClick(item: MenuItem) = when (item.itemId) {
        R.id.removeFriend -> {
            presenter.removeFriend()
            true
        }
        R.id.blockFriend -> {
            presenter.blockFriend()
            true
        }
        R.id.setNickname -> {
            presenter.nicknameMenuClicked()
            true
        }
        R.id.viewAccount -> {
            presenter.viewAccountMenuClicked()
            true
        }
        R.id.viewAliases -> {
            presenter.viewAliasesMenuClicked()
            true
        }
        else -> false
    }

    override fun showRemoveFriendDialog(name: String) {
        val builder = AlertDialog.Builder(this)

        builder.setMessage(getString(R.string.dialogMessageRemoveFriend, name))
                .setTitle(getString(R.string.dialogTitleRemoveFriend, name))
                .setPositiveButton(R.string.dialogYes, { _, _ -> presenter.confirmRemoveFriend() })
                .setNegativeButton(R.string.dialogNo, null)

        builder.create().show()
    }

    override fun showBlockFriendDialog(name: String) {
        val builder = AlertDialog.Builder(this)

        builder.setMessage(getString(R.string.dialogMessageBlockFriend, name))
                .setTitle(getString(R.string.dialogTitleBlockFriend, name))
                .setPositiveButton(R.string.dialogYes, { _, _ -> presenter.confirmBlockFriend() })
                .setNegativeButton(R.string.dialogNo, null)

        builder.create().show()
    }

    override fun showNicknameDialog(nickname: String) {
        @SuppressLint("InflateParams")
        val v = LayoutInflater.from(this).inflate(R.layout.dialog_nickname, null)
        v.nickname.setText(nickname)

        val builder = AlertDialog.Builder(this)
                .setTitle(R.string.dialogTitleNickname)
                .setView(v)
                .setPositiveButton(R.string.dialogSet, { _, _ -> presenter.setNickname(v.nickname.text.toString()) })
                .setNegativeButton(R.string.dialogCancel, null)

        builder.create().show()
    }

    override fun browseUrl(url: String) {
        startActivity<WebActivity>(WebActivity.EXTRA_URL to url)
    }

    override fun showAliases(names: List<String>) {
        runOnUiThread {
            val builder = AlertDialog.Builder(this)

            builder.setTitle(R.string.dialogTitleAliases)
                    .setItems(names.toTypedArray(), null)
                    .setNegativeButton(R.string.dialogClose, null)

            builder.create().show()
        }
    }

    override fun showEmotes(list: List<Emoticon>) {
        emoteAdapter.swap(list)
    }

    override fun onEmoteSelected(emoticon: Emoticon) {
        messageBox.text.insert(messageBox.selectionStart, ":${emoticon.name}:")
    }

    override fun showImgurDialog() {
        val builder = AlertDialog.Builder(this)

        builder.setMessage(R.string.dialogMessageImgur)
                .setTitle(R.string.dialogTitleImgur)
                .setPositiveButton(R.string.dialogYes, { _, _ -> startActivity<SettingsActivity>() })
                .setNegativeButton(R.string.dialogCancel, null)

        builder.create().show()
    }

    override fun showPhotoSelector() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, REQUEST_IMAGE_GET)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == REQUEST_IMAGE_GET && resultCode == RESULT_OK) {
            presenter.sendImage(data.data)
        }
    }

    override fun showUploadDialog() {
        runOnUiThread {
            imageButton.isClickable = false
            uploadProgressBar.show()
            uploadProgressBar.isIndeterminate = true
        }
    }

    override fun imageUploadFail() {
        runOnUiThread {
            imageButton.isClickable = true
            uploadProgressBar.hide()
            Snackbar.make(rootLayout, R.string.snackbarImgurUploadFailed, Snackbar.LENGTH_LONG).show()
        }
    }

    override fun imageUploadSuccess() {
        runOnUiThread {
            imageButton.isClickable = true
            uploadProgressBar.hide()
        }
    }

    override fun imageUploadProgress(total: Int, progress: Int) {
        uploadProgressBar.max = total
        uploadProgressBar.progress = progress
        uploadProgressBar.isIndeterminate = false
    }

    @Suppress("UNUSED_PARAMETER")
    fun navigateUp(v: View) {
        navigateUp()
    }

    @Suppress("UNUSED_PARAMETER")
    fun sendMessage(v: View) {
        val message = messageBox.text.toString()

        if (!Strings.isNullOrEmpty(message)) {
            messageBox.setText("")
            presenter.sendMessage(message)
        }

    }

    @Suppress("UNUSED_PARAMETER")
    fun toggleEmote(v: View) {
        emoteList.toggleVisibility()

        if (emoteList.isVisible()) {
            hideKeyboardFrom(messageBox)
            presenter.requestEmotes()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun sendImage(v: View) {
        presenter.imageButtonClicked()
    }

    @Suppress("UNUSED_PARAMETER")
    fun scrollToBeginning(v: View) {
        if (layoutManager.findFirstCompletelyVisibleItemPosition() > 20) {
            chatList.scrollToPosition(0)
        } else {
            chatList.smoothScrollToPosition(0)
        }
    }
}

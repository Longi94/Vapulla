package `in`.dragonbra.vapulla.view

import `in`.dragonbra.vapulla.adapter.FriendListItem
import `in`.dragonbra.vapulla.data.entity.ChatMessage
import `in`.dragonbra.vapulla.data.entity.Emoticon
import android.arch.paging.PagedList
import com.hannesdorfmann.mosby3.mvp.MvpView

interface ChatView : MvpView {
    fun closeApp()
    fun showChat(list: PagedList<ChatMessage>?)
    fun updateFriendData(friend: FriendListItem?)
    fun showRemoveFriendDialog(name: String)
    fun navigateUp()
    fun showBlockFriendDialog(name: String)
    fun showNicknameDialog(nickname: String)
    fun browseUrl(url: String)
    fun showAliases(names: List<String>)
    fun showEmotes(list: List<Emoticon>)
    fun showImgurDialog()
    fun showPhotoSelector()
    fun showUploadDialog()
    fun imageUploadFail()
    fun imageUploadSuccess()
    fun imageUploadProgress(total: Int, progress: Int)
}
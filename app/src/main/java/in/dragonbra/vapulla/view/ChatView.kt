package `in`.dragonbra.vapulla.view

import `in`.dragonbra.vapulla.data.entity.ChatMessage
import `in`.dragonbra.vapulla.data.entity.SteamFriend
import android.arch.paging.PagedList
import com.hannesdorfmann.mosby3.mvp.MvpView

interface ChatView : MvpView {
    fun closeApp()
    fun showChat(list: PagedList<ChatMessage>?)
    fun updateFriendData(friend: SteamFriend?)
}
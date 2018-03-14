package `in`.dragonbra.vapulla.view

import `in`.dragonbra.vapulla.adapter.FriendListItem
import `in`.dragonbra.vapulla.manager.AccountManager
import com.hannesdorfmann.mosby3.mvp.MvpView

interface HomeView : MvpView{
    fun closeApp()
    fun showFriends(list: List<FriendListItem>)
    fun showAccount(account: AccountManager)
    fun showBlockFriendDialog(friend: FriendListItem)
}
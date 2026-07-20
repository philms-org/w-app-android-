
package com.vastlb.wing_me.DataClasses

data class CommentClass(val imageURL: String, val id: String, val userID: String, val name: String, val badgeImageURL: String, val badgeTitle: String, val age: String, val gender: String, val country: String, val city: String, val comment: String, var likes: Int, val isMyComment: Boolean, var isLiked: Boolean)

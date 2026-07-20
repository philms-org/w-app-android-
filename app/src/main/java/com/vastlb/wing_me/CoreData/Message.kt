
package com.vastlb.wing_me.CoreData

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Message(@PrimaryKey(autoGenerate = true) var id: Int? = null,
                   @ColumnInfo var userID: String = "",
                   @ColumnInfo var lastMessageID: String = "")
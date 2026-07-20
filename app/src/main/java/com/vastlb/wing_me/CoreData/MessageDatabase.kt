
package com.vastlb.wing_me.CoreData

import androidx.room.*

@Database(entities = arrayOf(Message::class), version = 1)
abstract class MessageDatabase: RoomDatabase() {
    abstract fun dataDao(): MessageDao
}
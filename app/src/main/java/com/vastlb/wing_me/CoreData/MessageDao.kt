
package com.vastlb.wing_me.CoreData

import androidx.room.*

@Dao
interface MessageDao {

    @Query("SELECT * FROM message")
    fun getAll(): List<Message>

    @Query("SELECT * FROM message WHERE userID = :id ")
    fun getItemByUserID(id: String): Message

    @Insert
    fun insert(item: Message)

    @Delete
    fun delete(item: Message)

    @Query("DELETE FROM message")
    fun clear()

    @Update
    fun update(item: Message)
}

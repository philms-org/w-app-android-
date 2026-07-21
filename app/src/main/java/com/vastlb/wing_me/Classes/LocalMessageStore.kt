
package com.vastlb.wing_me.Classes

// Drop-in replacement for the old Room-backed CoreData.MessageDao. Room needed
// kapt, which broke under this machine's JDK (persistent IllegalAccessError
// that survived --add-opens flags, a JDK 17 switch, and forcing in-process
// execution) -- removing kapt entirely was the actual fix. This class exists
// purely so ChatActivity/GroupChatActivity/MessagesFragment's "have I already
// shown a badge for this message" bookkeeping keeps compiling and working the
// same way, just in-memory instead of a local SQLite file (state resets on
// app restart instead of persisting -- acceptable for this local-only badge
// cache; the actual message data is now the real Supabase source of truth).

data class LocalMessage(var id: Int? = null, var userID: String = "", var lastMessageID: String = "")

class LocalMessageStore private constructor() {
    private val items = mutableListOf<LocalMessage>()
    private var nextId = 1

    @Synchronized fun getAll(): List<LocalMessage> = items.toList()

    @Synchronized fun getItemByUserID(id: String): LocalMessage = items.first { it.userID == id }

    @Synchronized fun insert(item: LocalMessage) {
        if (item.id == null) item.id = nextId++
        items.add(item)
    }

    @Synchronized fun delete(item: LocalMessage) {
        items.remove(item)
    }

    @Synchronized fun clear() {
        items.clear()
    }

    @Synchronized fun update(item: LocalMessage) {
        val index = items.indexOfFirst { it.id == item.id }
        if (index >= 0) items[index] = item
    }

    companion object {
        private val instances = HashMap<String, LocalMessageStore>()

        @Synchronized
        fun named(name: String): LocalMessageStore = instances.getOrPut(name) { LocalMessageStore() }
    }
}

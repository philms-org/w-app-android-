
package com.vastlb.wing_me.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.vastlb.wing_me.DataClasses.ChatDateClass
import com.vastlb.wing_me.DataClasses.ChatMessageClass
import com.vastlb.wing_me.R

class ChatAdapter(val array: ArrayList<Any>): RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

    var gender = ""

    class ViewHolder(itemView: View, p1: Int): RecyclerView.ViewHolder(itemView) {
        val p1 = p1

        lateinit var dateTextView: TextView

        lateinit var messageTextView: TextView
        lateinit var timeTextView: TextView

        init {
            if (p1 == 0) {
                dateTextView = itemView.findViewById(R.id.id_date_text_view)
            } else {
                messageTextView = itemView.findViewById(R.id.id_message_text_view)
                timeTextView = itemView.findViewById(R.id.id_time_text_view)
            }
        }
    }

    override fun getItemCount(): Int {
        return array.size
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder {
        if (p1 == 0) {
            val cell = LayoutInflater.from(p0.context).inflate(R.layout.row_layout_chat_date, p0, false)
            return ViewHolder(cell, p1)
        } else if (p1 == 1) {
            val cell = LayoutInflater.from(p0.context).inflate(R.layout.row_layout_chat_right, p0, false)
            return ViewHolder(cell, p1)
        } else {
            if (gender == "F") {
                val cell = LayoutInflater.from(p0.context).inflate(R.layout.row_layout_chat_left_pink, p0, false)
                return ViewHolder(cell, p1)
            } else {
                val cell = LayoutInflater.from(p0.context).inflate(R.layout.row_layout_chat_left_blue, p0, false)
                return ViewHolder(cell, p1)
            }
        }
    }

    override fun onBindViewHolder(p0: ViewHolder, p1: Int) {
        if (p0.p1 == 0) {
            val jsonObject = array[p1] as ChatDateClass

            p0.dateTextView.setText(jsonObject.date)
        } else {
            val jsonObject = array[p1] as ChatMessageClass

            p0.messageTextView.setText(jsonObject.message)
            p0.timeTextView.setText(jsonObject.time)
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (array[position] is ChatDateClass) {
            return 0
        } else {
            val jsonObject = array[position] as ChatMessageClass

            if (jsonObject.type == "send") {
                return 1
            } else {
                return 2
            }
        }
    }
}


package com.vastlb.wing_me.Adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.vastlb.wing_me.Classes.Constants
import com.vastlb.wing_me.DataClasses.MessageClass
import com.vastlb.wing_me.R

class MessagesAdapter(val context: Context, val array: ArrayList<MessageClass>, val select: (index: Int) -> Unit, val longClick: (index: Int) -> Unit): RecyclerView.Adapter<MessagesAdapter.ViewHolder>() {

    class ViewHolder(itemView: View, p1: Int): RecyclerView.ViewHolder(itemView) {
        val relativeLayout: RelativeLayout = itemView.findViewById(R.id.id_relative_layout)
        val imageLayout: RelativeLayout = itemView.findViewById(R.id.id_image_layout)
        val imageView: ImageView = itemView.findViewById(R.id.id_image_view)
        val titleTextView: TextView = itemView.findViewById(R.id.id_title_text_view)
        val checkmark: ImageView = itemView.findViewById(R.id.id_checkmark)
        val messageTextView: TextView = itemView.findViewById(R.id.id_message_text_view)
        val dateTextView: TextView = itemView.findViewById(R.id.id_date_text_view)
        val badgeView: View = itemView.findViewById(R.id.id_badge_view)
    }

    override fun getItemCount(): Int {
        return array.size
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder {
        val cell = LayoutInflater.from(p0.context).inflate(R.layout.row_layout_message, p0, false)
        return ViewHolder(cell, p1)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onBindViewHolder(p0: ViewHolder, p1: Int) {
        val jsonObject: MessageClass = array[p1]

        if (jsonObject.url.isEmpty()) {
            p0.imageView.setImageDrawable(context.getDrawable(R.drawable.icon_logo_profile))
        } else {
            Picasso.get().load(Constants.url + jsonObject.url).into(p0.imageView)
        }
        p0.titleTextView.setText(jsonObject.name)
        p0.dateTextView.setText(jsonObject.date)

        if (jsonObject.isMaster) {
            p0.checkmark.visibility = View.VISIBLE
        } else {
            p0.checkmark.visibility = View.GONE
        }
        if (jsonObject.message.isEmpty()) {
            p0.messageTextView.visibility = View.GONE
        } else {
            p0.messageTextView.visibility = View.VISIBLE
            p0.messageTextView.setText(jsonObject.message)
        }
        if (jsonObject.isSelected) {
            p0.badgeView.visibility = View.VISIBLE
        } else {
            p0.badgeView.visibility = View.GONE
        }

        if (jsonObject.blocked) {
            p0.imageLayout.setBackgroundResource(R.drawable.view_drawable_blocked_image)
        } else {
            if (jsonObject.gender == "F") {
                p0.imageLayout.setBackgroundResource(R.drawable.view_drawable_user_image_pink)
            } else {
                p0.imageLayout.setBackgroundResource(R.drawable.view_drawable_user_image_blue)
            }
        }

        p0.relativeLayout.setOnClickListener {
            select(p1)
        }

        p0.relativeLayout.setOnLongClickListener {
            longClick(p1)
            return@setOnLongClickListener true
        }
    }
}

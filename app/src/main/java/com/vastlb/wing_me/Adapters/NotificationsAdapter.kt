
package com.vastlb.wing_me.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.vastlb.wing_me.DataClasses.NotificationClass
import com.vastlb.wing_me.R

class NotificationsAdapter(val array: ArrayList<NotificationClass>): RecyclerView.Adapter<NotificationsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View, p1: Int): RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.id_title_text_view)
        val detailsTextView: TextView = itemView.findViewById(R.id.id_details_text_view)
        val dateTextView: TextView = itemView.findViewById(R.id.id_date_text_view)
    }

    override fun getItemCount(): Int {
        return array.size
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder {
        val cell = LayoutInflater.from(p0.context).inflate(R.layout.row_layout_notification, p0, false)
        return ViewHolder(cell, p1)
    }

    override fun onBindViewHolder(p0: ViewHolder, p1: Int) {
        val jsonObject: NotificationClass = array[p1]

        p0.titleTextView.setText(jsonObject.title)
        p0.detailsTextView.setText(jsonObject.details)
        p0.dateTextView.setText(jsonObject.date)
    }
}


package com.vastlb.wing_me.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.vastlb.wing_me.DataClasses.NationalityClass
import com.vastlb.wing_me.R

class NationalitiesAdapter(val array: ArrayList<NationalityClass>, val context: Context, val select: (index: Int) -> Unit): RecyclerView.Adapter<NationalitiesAdapter.ViewHolder>() {

    class ViewHolder(itemView: View, p1: Int): RecyclerView.ViewHolder(itemView) {
        val relativeLayout: RelativeLayout = itemView.findViewById(R.id.id_relative_layout)
        val titleTextView: TextView = itemView.findViewById(R.id.id_title_text_view)
        val emojiTextView: TextView = itemView.findViewById(R.id.id_emoji_text_view)
    }

    override fun getItemCount(): Int {
        return array.size
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder {
        val cell = LayoutInflater.from(p0.context).inflate(R.layout.row_layout_nationality, p0, false)
        return ViewHolder(cell, p1)
    }

    override fun onBindViewHolder(p0: ViewHolder, p1: Int) {
        val jsonObject: NationalityClass = array[p1]

        p0.titleTextView.setText(jsonObject.title)
        p0.emojiTextView.setText(jsonObject.emoji)

        if (jsonObject.isSelected) {
            p0.relativeLayout.setBackgroundResource(R.drawable.view_drawable_selected)
            p0.titleTextView.setTextColor(context.getColor(R.color.white))
        } else {
            p0.relativeLayout.setBackgroundResource(R.drawable.view_drawable_edit_text)
            p0.titleTextView.setTextColor(context.getColor(R.color.dark_gray))
        }

        p0.relativeLayout.setOnClickListener {
            select(p1)
        }
    }
}

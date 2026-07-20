
package com.vastlb.wing_me.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.vastlb.wing_me.DataClasses.CategoryClass
import com.vastlb.wing_me.R

class CategoriesAdapter(val array: ArrayList<CategoryClass>, val context: Context, val select: (index: Int) -> Unit): RecyclerView.Adapter<CategoriesAdapter.ViewHolder>() {

    class ViewHolder(itemView: View, p1: Int): RecyclerView.ViewHolder(itemView) {
        val relativeLayout: RelativeLayout = itemView.findViewById(R.id.id_relative_layout)
        val cardLayout: RelativeLayout = itemView.findViewById(R.id.id_card_layout)
        val titleTextView: TextView = itemView.findViewById(R.id.id_title_text_view)
        val countTextView: TextView = itemView.findViewById(R.id.id_count_text_view)
    }

    override fun getItemCount(): Int {
        return array.size
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder {
        val cell = LayoutInflater.from(p0.context).inflate(R.layout.row_layout_category, p0, false)
        return ViewHolder(cell, p1)
    }

    override fun onBindViewHolder(p0: ViewHolder, p1: Int) {
        val jsonObject: CategoryClass = array[p1]

        p0.titleTextView.setText(jsonObject.title)
        p0.countTextView.setText(jsonObject.count.toString())

        if (jsonObject.isSelected) {
            p0.cardLayout.setBackgroundResource(R.drawable.view_drawable_gradient_diagonal)
            p0.titleTextView.setTextColor(context.getColor(R.color.white))
        } else {
            p0.cardLayout.setBackgroundColor(context.getColor(R.color.white))
            p0.titleTextView.setTextColor(context.getColor(R.color.black))
        }

        p0.relativeLayout.setOnClickListener {
            select(p1)
        }
    }
}

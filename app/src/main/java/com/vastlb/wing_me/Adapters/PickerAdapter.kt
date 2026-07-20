
package com.vastlb.wing_me.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.vastlb.wing_me.DataClasses.PickerClass
import com.vastlb.wing_me.R

class PickerAdapter(val array: ArrayList<PickerClass>, val select: (index: Int) -> Unit): RecyclerView.Adapter<PickerAdapter.ViewHolder>() {

    class ViewHolder(itemView: View, p1: Int): RecyclerView.ViewHolder(itemView) {
        val relativeLayout: RelativeLayout = itemView.findViewById(R.id.id_relative_layout)
        val titleTextView: TextView = itemView.findViewById(R.id.id_title_text_view)
    }

    override fun getItemCount(): Int {
        return array.size
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder {
        val cell = LayoutInflater.from(p0.context).inflate(R.layout.row_layout_picker, p0, false)
        return ViewHolder(cell, p1)
    }

    override fun onBindViewHolder(p0: ViewHolder, p1: Int) {
        val jsonObject: PickerClass = array[p1]

        p0.titleTextView.setText(jsonObject.string2)

        p0.relativeLayout.setOnClickListener {
            select(p1)
        }
    }
}

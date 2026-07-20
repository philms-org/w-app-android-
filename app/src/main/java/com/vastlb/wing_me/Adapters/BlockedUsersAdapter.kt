
package com.vastlb.wing_me.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.vastlb.wing_me.Classes.Constants
import com.vastlb.wing_me.DataClasses.BlockedUserClass
import com.vastlb.wing_me.R

class BlockedUsersAdapter(val array: ArrayList<BlockedUserClass>, val select: (index: Int) -> Unit): RecyclerView.Adapter<BlockedUsersAdapter.ViewHolder>() {

    class ViewHolder(itemView: View, p1: Int): RecyclerView.ViewHolder(itemView) {
        val relativeLayout: RelativeLayout = itemView.findViewById(R.id.id_relative_layout)
        val imageView: ImageView = itemView.findViewById(R.id.id_image_view)
        val titleTextView: TextView = itemView.findViewById(R.id.id_title_text_view)
    }

    override fun getItemCount(): Int {
        return array.size
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder {
        val cell = LayoutInflater.from(p0.context).inflate(R.layout.row_layout_blocked_user, p0, false)
        return ViewHolder(cell, p1)
    }

    override fun onBindViewHolder(p0: ViewHolder, p1: Int) {
        val jsonObject: BlockedUserClass = array[p1]

        Picasso.get().load(Constants.url + jsonObject.url).into(p0.imageView)

        p0.titleTextView.setText(jsonObject.name)

        p0.relativeLayout.setOnClickListener {
            select(p1)
        }
    }
}

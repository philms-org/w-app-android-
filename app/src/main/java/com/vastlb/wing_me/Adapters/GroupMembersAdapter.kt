
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
import com.vastlb.wing_me.DataClasses.UserClass
import com.vastlb.wing_me.R

class GroupMembersAdapter(val context: Context, val array: ArrayList<UserClass>, val select: (index: Int) -> Unit, val longClick: (index: Int) -> Unit): RecyclerView.Adapter<GroupMembersAdapter.ViewHolder>() {

    class ViewHolder(itemView: View, p1: Int): RecyclerView.ViewHolder(itemView) {
        val relativeLayout: RelativeLayout = itemView.findViewById(R.id.id_relative_layout)
        val imageLayout: RelativeLayout = itemView.findViewById(R.id.id_image_layout)
        val imageView: ImageView = itemView.findViewById(R.id.id_image_view)
        val titleTextView: TextView = itemView.findViewById(R.id.id_title_text_view)
        val checkmark: ImageView = itemView.findViewById(R.id.id_checkmark)
        val detailsTextView: TextView = itemView.findViewById(R.id.id_details_text_view)
    }

    override fun getItemCount(): Int {
        return array.size
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder {
        val cell = LayoutInflater.from(p0.context).inflate(R.layout.row_layout_user, p0, false)
        return ViewHolder(cell, p1)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onBindViewHolder(p0: ViewHolder, p1: Int) {
        val jsonObject: UserClass = array[p1]

        if (jsonObject.id == "0") {
            p0.imageView.setImageDrawable(context.getDrawable(R.drawable.icon_logo_profile))
            p0.detailsTextView.visibility = View.GONE
        } else {
            Picasso.get().load(Constants.url + jsonObject.url).into(p0.imageView)
            p0.detailsTextView.visibility = View.VISIBLE
            p0.detailsTextView.setText(jsonObject.details)
        }
        p0.titleTextView.setText(jsonObject.name)

        if (jsonObject.details.isEmpty()) {
            p0.detailsTextView.visibility = View.GONE
        } else {
            p0.detailsTextView.visibility = View.VISIBLE
        }
        if (jsonObject.isMaster) {
            p0.checkmark.visibility = View.VISIBLE
        } else {
            p0.checkmark.visibility = View.GONE
        }
        if (jsonObject.gender == "F") {
            p0.imageLayout.setBackgroundResource(R.drawable.view_drawable_user_image_pink)
        } else {
            p0.imageLayout.setBackgroundResource(R.drawable.view_drawable_user_image_blue)
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


package com.vastlb.wing_me.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.vastlb.wing_me.Classes.Constants
import com.vastlb.wing_me.DataClasses.BannerClass
import com.vastlb.wing_me.R

class HomeBannerAdapter(val array: ArrayList<BannerClass>, val select: (index: Int) -> Unit): RecyclerView.Adapter<HomeBannerAdapter.ViewHolder>() {

    class ViewHolder(itemView: View, p1: Int): RecyclerView.ViewHolder(itemView) {
        val relativeLayout: RelativeLayout = itemView.findViewById(R.id.id_relative_layout)
        val imageView: ImageView = itemView.findViewById(R.id.id_image_view)
    }

    override fun getItemCount(): Int {
        return array.size
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder {
        val cell = LayoutInflater.from(p0.context).inflate(R.layout.row_layout_home_banner, p0, false)
        return ViewHolder(cell, p1)
    }

    override fun onBindViewHolder(p0: ViewHolder, p1: Int) {
        val jsonObject: BannerClass = array[p1]

        Picasso.get().load(Constants.url + jsonObject.imageURL).into(p0.imageView)

        p0.relativeLayout.setOnClickListener {
            select(p1)
        }
    }
}


package com.vastlb.wing_me.Adapters

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
import com.vastlb.wing_me.DataClasses.LocationBannerClass
import com.vastlb.wing_me.R
import jp.wasabeef.picasso.transformations.BlurTransformation

class NewLocationBannerAdapter(val context: Context, val array: ArrayList<LocationBannerClass>, val select: (index: Int) -> Unit): RecyclerView.Adapter<NewLocationBannerAdapter.ViewHolder>() {

    class ViewHolder(itemView: View, p1: Int): RecyclerView.ViewHolder(itemView) {
        val relativeLayout: RelativeLayout = itemView.findViewById(R.id.id_relative_layout)
        val imageView: ImageView = itemView.findViewById(R.id.id_image_view)
        val textView: TextView = itemView.findViewById(R.id.id_text_view)
    }

    override fun getItemCount(): Int {
        return array.size
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder {
        val cell = LayoutInflater.from(p0.context).inflate(R.layout.row_layout_location_banner_new, p0, false)
        return ViewHolder(cell, p1)
    }

    override fun onBindViewHolder(p0: ViewHolder, p1: Int) {
        val jsonObject: LocationBannerClass = array[p1]

        if (jsonObject.blurred) {
            p0.textView.setText(jsonObject.title)
            Picasso.get().load(Constants.url + jsonObject.imageURL).transform(BlurTransformation(context, 25, 1)).into(p0.imageView)
        } else {
            p0.textView.setText("")
            Picasso.get().load(Constants.url + jsonObject.imageURL).into(p0.imageView)
        }

        p0.relativeLayout.setOnClickListener {
            select(p1)
        }
    }
}


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
import com.vastlb.wing_me.DataClasses.ReplyClass
import com.vastlb.wing_me.R

class RepliesAdapter(val context: Context, val array: ArrayList<ReplyClass>, val select: (index: Int) -> Unit, val longClick: (index: Int) -> Unit, val openProfile: (index: Int) -> Unit, val like: (index: Int, like: Boolean) -> Unit, val add: (index: Int, add: Boolean) -> Unit): RecyclerView.Adapter<RepliesAdapter.ViewHolder>() {

    var isMyComment = false

    class ViewHolder(itemView: View, p1: Int): RecyclerView.ViewHolder(itemView) {
        val relativeLayout: RelativeLayout = itemView.findViewById(R.id.id_relative_layout)
        val add: ImageView = itemView.findViewById(R.id.id_add)
        val imageLayout: RelativeLayout = itemView.findViewById(R.id.id_image_layout)
        val imageView: ImageView = itemView.findViewById(R.id.id_image_view)
        val nameTextView: TextView = itemView.findViewById(R.id.id_name_text_view)
        val badgeImageView: ImageView = itemView.findViewById(R.id.id_badge_image_view)
        val badgeTextView: TextView = itemView.findViewById(R.id.id_badge_text_view)
        val detailsTextView: TextView = itemView.findViewById(R.id.id_details_text_view)
        val commentTextView: TextView = itemView.findViewById(R.id.id_comment_text_view)
        val like: ImageView = itemView.findViewById(R.id.id_like)
        val likesTextView: TextView = itemView.findViewById(R.id.id_likes_text_view)
    }

    override fun getItemCount(): Int {
        return array.size
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder {
        val cell = LayoutInflater.from(p0.context).inflate(R.layout.row_layout_reply, p0, false)
        return ViewHolder(cell, p1)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onBindViewHolder(p0: ViewHolder, p1: Int) {
        val jsonObject: ReplyClass = array[p1]

        Picasso.get().load(Constants.url + jsonObject.imageURL).into(p0.imageView)
        Picasso.get().load(Constants.url + jsonObject.badgeImageURL).into(p0.badgeImageView)

        p0.nameTextView.setText(jsonObject.name)
        p0.badgeTextView.setText(jsonObject.badgeTitle)
        p0.commentTextView.setText(jsonObject.comment)
        p0.likesTextView.setText(jsonObject.likes.toString())

        if (isMyComment) {
            if (jsonObject.isMyComment) {
                p0.add.visibility = View.GONE
            } else {
                p0.add.visibility = View.VISIBLE

                if (jsonObject.isAdded) {
                    p0.add.setImageDrawable(context.getDrawable(R.drawable.icon_remove))
                } else {
                    p0.add.setImageDrawable(context.getDrawable(R.drawable.icon_add))
                }
            }
        } else {
            p0.add.visibility = View.GONE
        }

        var details = ""

        p0.detailsTextView.visibility = View.VISIBLE

        if (jsonObject.age.isEmpty()) {
            if (jsonObject.city.isEmpty()) {
                if (jsonObject.country.isEmpty()) {
                    p0.detailsTextView.visibility = View.GONE
                } else {
                    val flags = Constants.getFlags()
                    val flag = flags.first {
                        flag ->
                        flag.id == jsonObject.country
                    }
                    details = flag.emoji
                }
            } else {
                if (jsonObject.country.isEmpty()) {
                    details = jsonObject.city
                } else {
                    val flags = Constants.getFlags()
                    val flag = flags.first {
                        flag ->
                        flag.id == jsonObject.country
                    }
                    details = "${jsonObject.city} ${flag.emoji}"
                }
            }
        } else {
            if (jsonObject.city.isEmpty()) {
                if (jsonObject.country.isEmpty()) {
                    details = "Age: ${jsonObject.age}"
                } else {
                    val flags = Constants.getFlags()
                    val flag = flags.first {
                        flag ->
                        flag.id == jsonObject.country
                    }
                    details = "Age: ${jsonObject.age} ${flag.emoji}"
                }
            } else {
                if (jsonObject.country.isEmpty()) {
                    details = "Age: ${jsonObject.age}, ${jsonObject.city}"
                } else {
                    val flags = Constants.getFlags()
                    val flag = flags.first {
                        flag ->
                        flag.id == jsonObject.country
                    }
                    details = "Age: ${jsonObject.age}, ${jsonObject.city} ${flag.emoji}"
                }
            }
        }
        p0.detailsTextView.setText(details)

        if (jsonObject.gender == "F") {
            p0.imageLayout.setBackgroundResource(R.drawable.view_drawable_user_image_pink)
        } else {
            p0.imageLayout.setBackgroundResource(R.drawable.view_drawable_user_image_blue)
        }

        if (jsonObject.isLiked) {
            p0.like.setImageDrawable(context.getDrawable(R.drawable.icon_heart_full))
            p0.like.setColorFilter(context.getColor(R.color.red))
        } else {
            p0.like.setImageDrawable(context.getDrawable(R.drawable.icon_heart))
            p0.like.setColorFilter(context.getColor(R.color.white))
        }

        p0.imageView.setOnClickListener {
            openProfile(p1)
        }

        p0.add.setOnClickListener {
            add(p1, !jsonObject.isAdded)
        }

        p0.like.setOnClickListener {
            like(p1, !jsonObject.isLiked)
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


package com.vastlb.wing_me.Adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.vastlb.wing_me.DataClasses.LookingForClass
import com.vastlb.wing_me.R

class LookingForAdapter(val array: ArrayList<LookingForClass>, val context: Context, val change: (index: Int, progress: Int) -> Unit): RecyclerView.Adapter<LookingForAdapter.ViewHolder>() {

    class ViewHolder(itemView: View, p1: Int): RecyclerView.ViewHolder(itemView) {
        val relativeLayout: RelativeLayout = itemView.findViewById(R.id.id_relative_layout)
        val titleTextView: TextView = itemView.findViewById(R.id.id_title_text_view)
        val emoji1: TextView = itemView.findViewById(R.id.id_emoji_1)
        val title1: TextView = itemView.findViewById(R.id.id_title_1)
        val emoji2: TextView = itemView.findViewById(R.id.id_emoji_2)
        val title2: TextView = itemView.findViewById(R.id.id_title_2)
        val emoji3: TextView = itemView.findViewById(R.id.id_emoji_3)
        val title3: TextView = itemView.findViewById(R.id.id_title_3)
        val emoji4: TextView = itemView.findViewById(R.id.id_emoji_4)
        val title4: TextView = itemView.findViewById(R.id.id_title_4)
        val seekBar: SeekBar = itemView.findViewById(R.id.id_seek_bar)
    }

    override fun getItemCount(): Int {
        return array.size
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder {
        val cell = LayoutInflater.from(p0.context).inflate(R.layout.row_layout_looking_for, p0, false)
        return ViewHolder(cell, p1)
    }

    override fun onBindViewHolder(p0: ViewHolder, @SuppressLint("RecyclerView") p1: Int) {
        val jsonObject: LookingForClass = array[p1]

        p0.titleTextView.setText(jsonObject.title)
        p0.emoji1.setText(jsonObject.emoji1.first())
        p0.title1.setText(jsonObject.emoji1.last())
        p0.emoji2.setText(jsonObject.emoji2.first())
        p0.title2.setText(jsonObject.emoji2.last())
        p0.emoji3.setText(jsonObject.emoji3.first())
        p0.title3.setText(jsonObject.emoji3.last())
        p0.emoji4.setText(jsonObject.emoji4.first())
        p0.title4.setText(jsonObject.emoji4.last())

        p0.seekBar.setProgress(jsonObject.progress)

        p0.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            @SuppressLint("SetTextI18n")
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                change(p1, progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

            }
        })
    }
}

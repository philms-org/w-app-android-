
package com.vastlb.wing_me.Main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vastlb.wing_me.Adapters.PickerAdapter
import com.vastlb.wing_me.DataClasses.PickerClass
import com.vastlb.wing_me.R

class PickerFragment: Fragment() {

    lateinit var close: () -> Unit
    lateinit var select: (index: Int) -> Unit
    lateinit var adapter: PickerAdapter

    val array = ArrayList<PickerClass>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_picker, container, false)
        setViews(view)
        return view
    }

    companion object {
        fun newInstance(): PickerFragment = PickerFragment()
    }

    fun setViews(view: View) {
        val relativeLayout: RelativeLayout = view.findViewById(R.id.id_relative_layout)
        val recyclerView: RecyclerView = view.findViewById(R.id.id_recycler_view)

        relativeLayout.setOnClickListener {
            close()
        }

        adapter = PickerAdapter(array) {
            index ->
            select(index)
            close()
        }

        val linearLayoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.adapter = adapter
    }
}

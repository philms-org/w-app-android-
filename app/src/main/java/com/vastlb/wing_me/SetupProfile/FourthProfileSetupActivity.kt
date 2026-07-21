
package com.vastlb.wing_me.SetupProfile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.vastlb.wing_me.Adapters.NationalitiesAdapter
import com.vastlb.wing_me.Classes.Constants
import com.vastlb.wing_me.DataClasses.NationalityClass
import com.vastlb.wing_me.Main.MainActivity
import com.vastlb.wing_me.R
import com.vastlb.wing_me.Supabase.SupabaseAuth
import com.vastlb.wing_me.Supabase.SupabaseData
import kotlinx.android.synthetic.main.activity_profile_setup_fourth.*
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList

class FourthProfileSetupActivity: AppCompatActivity() {

    lateinit var adapter: NationalitiesAdapter

    var allArray = ArrayList<NationalityClass>()
    var searchArray = ArrayList<NationalityClass>()

    var height = ""
    var relationship = ""
    var datingID = ""
    var socialisingID = ""
    var networkingID = ""
    var nationality = ""
    var city = ""
    var drink = ""
    var activity = ""
    var profession = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_setup_fourth)
        setViews()
    }

    fun setViews() {
        height = intent.getStringExtra("Height")!!
        relationship = intent.getStringExtra("Relationship")!!
        datingID = intent.getStringExtra("DatingID")!!
        socialisingID = intent.getStringExtra("SocialisingID")!!
        networkingID = intent.getStringExtra("NetworkingID")!!
        nationality = intent.getStringExtra("Nationality")!!
        city = intent.getStringExtra("City")!!
        drink = intent.getStringExtra("Drink")!!
        activity = intent.getStringExtra("Activity")!!
        profession = intent.getStringExtra("Profession")!!

        val flags = Constants.getFlags()

        for (index in 0..(flags.size - 1)) {
            val jsonObject = flags[index]
            val locale = Locale("en_US", jsonObject.id)
            val name = locale.getDisplayCountry()
            allArray.add(NationalityClass(jsonObject.id, name, jsonObject.emoji, nationality == jsonObject.id))
        }
        val sortedArray = allArray.sortedWith(compareBy {
            jsonObject ->
            jsonObject.title
        })
        allArray = ArrayList(sortedArray)
        searchArray = ArrayList(sortedArray)

        id_back.setOnClickListener {
            showBackAlert()
        }

        id_previous.setOnClickListener {
            finish()
        }

        id_next.setOnClickListener {
            val intent = Intent(this, FifthProfileSetupActivity::class.java)
            intent.putExtra("Height", height)
            intent.putExtra("Relationship", relationship)
            intent.putExtra("DatingID", datingID)
            intent.putExtra("SocialisingID", socialisingID)
            intent.putExtra("NetworkingID", networkingID)
            intent.putExtra("Nationality", nationality)
            intent.putExtra("City", city)
            intent.putExtra("Drink", drink)
            intent.putExtra("Activity", activity)
            intent.putExtra("Profession", profession)
            startActivity(intent)
        }

        id_search_edit_text.addTextChangedListener(object: TextWatcher {

            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                onTextChanged()
            }
        })

        adapter = NationalitiesAdapter(searchArray, this) {
            index ->
            if (searchArray.any {
                jsonObject ->
                jsonObject.id == nationality
            }) {
                val index = searchArray.indexOfFirst {
                    jsonObject ->
                    jsonObject.id == nationality
                }
                searchArray[index].isSelected = false
            }
            if (allArray.any {
                jsonObject ->
                jsonObject.id == nationality
            }) {
                val index = allArray.indexOfFirst {
                    jsonObject ->
                    jsonObject.id == nationality
                }
                allArray[index].isSelected = false
            }
            searchArray[index].isSelected = true
            adapter.notifyDataSetChanged()
            nationality = searchArray[index].id
        }

        val linearLayoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        id_recycler_view.layoutManager = linearLayoutManager
        id_recycler_view.adapter = adapter
    }

    fun onTextChanged() {
        if (id_search_edit_text.text.toString().isEmpty()) {
            searchArray.clear()

            for (index in 0..(allArray.size - 1)) {
                val jsonObject = allArray[index]
                searchArray.add(jsonObject)
            }
        } else {
            searchArray.clear()

            for (index in 0..(allArray.size - 1)) {
                val jsonObject = allArray[index]

                if (jsonObject.title.lowercase().startsWith(id_search_edit_text.text.toString().lowercase())) {
                    searchArray.add(jsonObject)
                }
            }
        }
        adapter.notifyDataSetChanged()
    }

    fun showBackAlert() {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle("Do you want to save your changed info?")
        alertDialog.setNegativeButton(getString(R.string.cancel), null)
        val dialogItems = arrayOf("Save Changes", "Discard Changes")

        alertDialog.setItems(dialogItems) { dialog, which ->

            when (which) {
                0 -> {
                    id_back.visibility = View.GONE
                    id_progress_bar.visibility = View.VISIBLE
                    send()
                }
                1 -> {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(intent)
                }
            }
        }
        alertDialog.show()
    }

    fun send() {
        val userId = SupabaseAuth.getUserId(this)

        if (userId == null) {
            sendError("Not signed in")
            return
        }
        val fields = JSONObject()
        fields.put("id", userId)
        if (height.isNotEmpty()) fields.put("height", height.toDoubleOrNull())
        if (relationship.isNotEmpty()) fields.put("relationship", relationship)
        if (datingID.isNotEmpty()) fields.put("dating_id", datingID.toIntOrNull())
        if (socialisingID.isNotEmpty()) fields.put("socialising_id", socialisingID.toIntOrNull())
        if (networkingID.isNotEmpty()) fields.put("networking_id", networkingID.toIntOrNull())
        if (nationality.isNotEmpty()) fields.put("nationality", nationality)
        if (city.isNotEmpty()) fields.put("city", city)

        SupabaseData.upsertProfile(this, fields, onSuccess = {
            sendSuccess()
        }, onError = { message ->
            sendError(message)
        })
    }

    fun sendError(message: String) {
        val toast = Toast.makeText(this, message, Toast.LENGTH_LONG)
        toast.show()
        id_back.visibility = View.VISIBLE
        id_progress_bar.visibility = View.GONE
    }

    fun sendSuccess() {
        val intent = Intent(this, MainActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        id_back.visibility = View.VISIBLE
        id_progress_bar.visibility = View.GONE
    }
}

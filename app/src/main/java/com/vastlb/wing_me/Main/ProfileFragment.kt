
package com.vastlb.wing_me.Main

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.squareup.picasso.Picasso
import com.vastlb.wing_me.Classes.BackgroundService
import com.vastlb.wing_me.Classes.Constants
import com.vastlb.wing_me.Classes.Singleton
import com.vastlb.wing_me.Profile.EditProfileActivity
import com.vastlb.wing_me.R
import com.vastlb.wing_me.Settings.NotificationsActivity
import com.vastlb.wing_me.Settings.SettingsActivity
import com.vastlb.wing_me.SetupProfile.SecondProfileSetupActivity
import com.vastlb.wing_me.SetupProfile.ThirdProfileSetupActivity
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class ProfileFragment: Fragment() {

    lateinit var close: () -> Unit

    lateinit var logout: TextView
    lateinit var logoutProgressBar: ProgressBar

    lateinit var imageView: ImageView
    lateinit var nameTextView: TextView
    lateinit var setupProfile: TextView
    lateinit var editProfileLayout: RelativeLayout
    lateinit var settingsLayout: RelativeLayout
    lateinit var phoneTextView: TextView
    lateinit var emailTextView: TextView
    lateinit var cityTextView: TextView
    lateinit var nationalityTextView: TextView
    lateinit var ageTextView: TextView
    lateinit var genderTextView: TextView
    lateinit var heightTextView: TextView
    lateinit var relationshipTextView: TextView
    lateinit var lookingForTextView: TextView
    lateinit var drinkTextView: TextView
    lateinit var fridayActivityTextView: TextView
    lateinit var professionTextView: TextView

    lateinit var scrollView: ScrollView
    lateinit var progressBar: ProgressBar

    var imageURL = ""
    var name = ""
    var phone = ""
    var email = ""
    var birthDate = ""
    var agePrivacy = ""
    var gender = ""

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        setViews(view)
        return view
    }

    companion object {
        fun newInstance(): ProfileFragment = ProfileFragment()
    }

    fun setViews(view: View) {
        Constants.reloadProfile = {
            reload()
        }

        val notification: ImageView = view.findViewById(R.id.id_notifications)

        val editProfile: TextView = view.findViewById(R.id.id_edit_profile)
        val settings: TextView = view.findViewById(R.id.id_settings)
        logout = view.findViewById(R.id.id_logout)
        logoutProgressBar = view.findViewById(R.id.id_logout_progress_bar)

        imageView = view.findViewById(R.id.id_image_view)
        nameTextView = view.findViewById(R.id.id_name_text_view)
        val setupProfileLayout: RelativeLayout = view.findViewById(R.id.id_setup_profile_layout)
        setupProfile = view.findViewById(R.id.id_setup_profile)
        editProfileLayout = view.findViewById(R.id.id_edit_profile_layout)
        settingsLayout = view.findViewById(R.id.id_settings_layout)
        phoneTextView = view.findViewById(R.id.id_phone_text_view)
        emailTextView = view.findViewById(R.id.id_email_text_view)
        cityTextView = view.findViewById(R.id.id_city_text_view)
        nationalityTextView = view.findViewById(R.id.id_nationality_text_view)
        ageTextView = view.findViewById(R.id.id_age_text_view)
        genderTextView = view.findViewById(R.id.id_gender_text_view)
        heightTextView = view.findViewById(R.id.id_height_text_view)
        relationshipTextView = view.findViewById(R.id.id_relationship_text_view)
        lookingForTextView = view.findViewById(R.id.id_looking_for_text_view)
        drinkTextView = view.findViewById(R.id.id_drink_text_view)
        fridayActivityTextView = view.findViewById(R.id.id_friday_night_text_view)
        professionTextView = view.findViewById(R.id.id_profession_text_view)

        scrollView = view.findViewById(R.id.id_scroll_view)
        progressBar = view.findViewById(R.id.id_progress_bar)

        scrollView.visibility = View.GONE

        val preferences = requireContext().getSharedPreferences("Preferences", Context.MODE_PRIVATE)
        val setup = preferences.getString("Setup", "")

        if (setup == "true") {
            setupProfileLayout.visibility = View.GONE
        }

        notification.setOnClickListener {
            val intent = Intent(context, NotificationsActivity::class.java)
            startActivity(intent)
        }

        setupProfile.setOnClickListener {
            openSetupProfile()
        }

        editProfile.setOnClickListener {
            openEditProfile()
        }

        settings.setOnClickListener {
            val intent = Intent(context, SettingsActivity::class.java)
            startActivity(intent)
        }

        logout.setOnClickListener {
            val alertDialog = AlertDialog.Builder(context)
            alertDialog.setTitle(getString(R.string.alert_logout))
            alertDialog.setPositiveButton(getString(R.string.logout)) {
                _, _ ->
                logout.visibility = View.GONE
                logoutProgressBar.visibility = View.VISIBLE
                logout()
            }
            alertDialog.setNegativeButton(getString(R.string.cancel), null)
            val alert = alertDialog.create()
            alert.show()
        }

        request()
    }

    fun openEditProfile() {
        val intent = Intent(context, EditProfileActivity::class.java)
        intent.putExtra("Image", imageURL)
        intent.putExtra("Name", name)
        intent.putExtra("Phone", phone)
        intent.putExtra("Email", email)
        intent.putExtra("Birth", birthDate)
        intent.putExtra("AgePrivacy", agePrivacy)
        intent.putExtra("Gender", gender)

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

    fun reload() {
        scrollView.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
        request()
    }

    fun request() {
        val preferences = requireContext().getSharedPreferences("Preferences", Context.MODE_PRIVATE)
        val token = preferences.getString("Token", "")
        val url = Constants.url + "get_info.php"

        val request = object: StringRequest(
            Method.POST, url,
            Response.Listener { response ->
                try {
                    requestSuccess(response)
                } catch (e: JSONException) {
                    val toast = Toast.makeText(context, e.toString(), Toast.LENGTH_LONG)
                    toast.show()
                }
            },
            Response.ErrorListener {
                connectionError()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = token!!
                return headers
            }
            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["language"] = getString(R.string.language)
                return params
            }
        }
        Singleton.getInstance(requireContext()).addToRequestQueue(request)
    }

    fun connectionError() {
        println("Error2")
        request()
    }

    @SuppressLint("SetTextI18n")
    fun requestSuccess(response: String) {
        val json = JSONObject(response)
        val error = json.getString("error")

        if (error == "0") {
            val message = json.getJSONObject("message")
            imageURL = message.getString("image")
            Picasso.get().load(Constants.url + imageURL).into(imageView)

            name = message.getString("name")
            phone = message.getString("phone")
            email = message.getString("email")

            datingID = message.getString("dating_Id")
            socialisingID = message.getString("socialising_Id")
            networkingID = message.getString("networking_Id")

            city = message.getString("city")
            drink = message.getString("drink")
            activity = message.getString("activity")
            profession = message.getString("profession")

            nameTextView.setText(name)
            phoneTextView.setText(phone)
            emailTextView.setText(email)

            if (city.isEmpty()) {
                cityTextView.setText("---")
            } else {
                cityTextView.setText(city)
            }
            if (drink.isEmpty()) {
                drinkTextView.setText("---")
            } else {
                drinkTextView.setText(drink)
            }
            if (activity.isEmpty()) {
                fridayActivityTextView.setText("---")
            } else {
                fridayActivityTextView.setText(activity)
            }
            if (profession.isEmpty()) {
                professionTextView.setText("---")
            } else {
                professionTextView.setText(profession)
            }

            nationality = message.getString("nationality")

            if (nationality.isEmpty()) {
                nationalityTextView.setText("---")
            } else {
                val flags = Constants.getFlags()
                val jsonObject = flags.first { jsonObject ->
                    jsonObject.id == nationality
                }
                val locale = Locale("en_US", jsonObject.id)
                val name = locale.getDisplayCountry()
                nationalityTextView.setText("${name} ${jsonObject.emoji}")
            }

            birthDate = message.getString("birth")
            val age = getAge(birthDate)
            ageTextView.setText(age)

            agePrivacy = message.getString("age_privacy")

            gender = message.getString("gender")
            val genderString = getGender(gender)
            genderTextView.setText(genderString)

            height = message.getString("height")

            if (height.isEmpty()) {
                heightTextView.setText("---")
            } else {
                heightTextView.setText("${height}m")
            }

            relationship = message.getString("relationship")

            if (relationship.isEmpty()) {
                relationshipTextView.setText("---")
            } else {
                val array = Constants.getRelationships(0)
                val jsonObject = array.first { jsonObject ->
                    jsonObject.id == relationship
                }
                relationshipTextView.setText(jsonObject.title)
            }

            Constants.datingID = message.getString("dating_Id")
            Constants.socialisingID = message.getString("socialising_Id")
            Constants.networkingID = message.getString("networking_Id")
            val lookingFor = Constants.getLookingFor(Constants.datingID, Constants.socialisingID, Constants.networkingID)

            if (lookingFor.isEmpty()) {
                lookingForTextView.setText("---")
            } else {
                lookingForTextView.setText(lookingFor)
            }

            if (gender == "F") {
                editProfileLayout.setBackgroundResource(R.drawable.view_drawable_profile_button_pink)
                settingsLayout.setBackgroundResource(R.drawable.view_drawable_profile_button_pink)
            } else {
                editProfileLayout.setBackgroundResource(R.drawable.view_drawable_profile_button_blue)
                settingsLayout.setBackgroundResource(R.drawable.view_drawable_profile_button_blue)
            }
            scrollView.visibility = View.VISIBLE

            if (city.isEmpty() || nationality.isEmpty() || height.isEmpty() || drink.isEmpty() || activity.isEmpty()) {
                setup()
            }
        } else if (error == "6") {
            Constants.deleteUserData(requireContext())
            close()
        } else {
            val message = json.getString("message")
            val toast = Toast.makeText(context, message, Toast.LENGTH_LONG)
            toast.show()
        }
        progressBar.visibility = View.GONE
    }

    fun getGender(gender: String): String {
        if (gender == "M") {
            return "Male"
        } else if (gender == "F") {
            return "Female"
        } else if (gender == "O") {
            return "Other"
        } else {
            return ""
        }
    }

    fun getAge(dateString: String): String {
        val dateParser = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        val date = dateParser.parse(dateString)!!

        val birth = Calendar.getInstance()
        val today = Calendar.getInstance()

        birth.time = date

        var age = today[Calendar.YEAR] - birth[Calendar.YEAR]

        if (today[Calendar.DAY_OF_YEAR] < birth[Calendar.DAY_OF_YEAR]) {
            age--
        }
        val ageInt = age

        return ageInt.toString()
    }

    fun logout() {
        val preferences = requireContext().getSharedPreferences("Preferences", Context.MODE_PRIVATE)
        val token = preferences.getString("Token", "")
        val url = Constants.url + "logout.php"

        val request = object: StringRequest(
            Method.POST, url,
            Response.Listener { response ->
                try {
                    logoutSuccess()
                } catch (e: JSONException) {
                    val toast = Toast.makeText(context, e.toString(), Toast.LENGTH_LONG)
                    toast.show()
                }
            },
            Response.ErrorListener {
                logoutError()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = token!!
                return headers
            }
            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["firebase_token"] = Constants.firebaseToken
                return params
            }
        }
        Singleton.getInstance(requireContext()).addToRequestQueue(request)
    }

    fun logoutError() {
        println("Error2")
        logout()
    }

    fun logoutSuccess() {
        Constants.hideLocation()
        Constants.deleteUserData(requireContext())

        logout.visibility = View.VISIBLE
        logoutProgressBar.visibility = View.GONE

        BackgroundService.stopService(requireContext())
        close()
    }

    fun setup() {
        Handler().postDelayed({
            showSetupAlert()
        }, 30000)
    }

    fun showSetupAlert() {
        val context = context ?: return

        val preferences = context.getSharedPreferences("Preferences", Context.MODE_PRIVATE)
        val setup = preferences.getString("Setup", "")!!

        if (!setup.isEmpty()) {
            return
        }
        val alertDialog = AlertDialog.Builder(context)
        alertDialog.setTitle(getString(R.string.alert_setup))
        alertDialog.setNegativeButton(getString(R.string.cancel), null)
        val dialogItems = arrayOf("Setup Now", "Later", "Never")

        alertDialog.setItems(dialogItems) {
            dialog, which ->

            when (which) {
                0 -> {
                    openSetupProfile()
                }
                2 -> {
                    val editor = preferences.edit()
                    editor.putString("Setup", "false")
                    editor.apply()
                }
            }
        }
        alertDialog.show()
    }

    fun openSetupProfile() {
        val intent = Intent(context, SecondProfileSetupActivity::class.java)
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
}

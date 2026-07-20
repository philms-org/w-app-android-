
package com.vastlb.wing_me.Profile

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.android.volley.Response
import com.squareup.picasso.Picasso
import com.vastlb.wing_me.Classes.Constants
import com.vastlb.wing_me.Classes.FileDataPart
import com.vastlb.wing_me.Classes.Singleton
import com.vastlb.wing_me.Classes.VolleyFileUploadRequest
import com.vastlb.wing_me.R
import com.vastlb.wing_me.SetupProfile.FirstProfileSetupActivity
import com.vastlb.wing_me.SetupProfile.SecondProfileSetupActivity
import com.vastlb.wing_me.SetupProfile.ThirdProfileSetupActivity
import kotlinx.android.synthetic.main.activity_edit_profile.*
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

class EditProfileActivity: AppCompatActivity() {

    val calendar = Calendar.getInstance()

    val GALLERY_RESULT= 1
    val CAMERA_RESULT = 2

    var imageURI: Uri? = null
    var bitmap: Bitmap? = null

    var phone = ""
    var genderIndex = 0
    var birthDate = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)
        setViews()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode:Int, resultCode:Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == GALLERY_RESULT) {
            if (data != null) {
                bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, data.data)
                bitmap = resize(bitmap!!, 1080)
                id_image_view.setImageBitmap(bitmap)
            }
        } else if (requestCode == CAMERA_RESULT && resultCode == Activity.RESULT_OK) {
            bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, imageURI)
            bitmap = resize(bitmap!!, 1080)
            id_image_view.setImageBitmap(bitmap)
        }
    }

    fun setViews() {
        val image = intent.getStringExtra("Image")!!
        val name = intent.getStringExtra("Name")!!
        phone = intent.getStringExtra("Phone")!!
        val email = intent.getStringExtra("Email")!!
        val birth = intent.getStringExtra("Birth")!!
        val agePrivacy = intent.getStringExtra("AgePrivacy")!!
        val gender = intent.getStringExtra("Gender")!!

        val height = intent.getStringExtra("Height")!!
        val relationship = intent.getStringExtra("Relationship")!!
        val datingID = intent.getStringExtra("DatingID")!!
        val socialisingID = intent.getStringExtra("SocialisingID")!!
        val networkingID = intent.getStringExtra("NetworkingID")!!
        val nationality = intent.getStringExtra("Nationality")!!
        val city = intent.getStringExtra("City")!!
        val drink = intent.getStringExtra("Drink")!!
        val activity = intent.getStringExtra("Activity")!!
        val profession = intent.getStringExtra("Profession")!!

        Picasso.get().load(Constants.url + image).into(id_image_view)

        id_name_edit_text.setText(name)
        id_email_edit_text.setText(email)

        setBirthdate(birth)
        setGender(gender)

        if (agePrivacy == "private") {
            id_switch.isChecked = true
        } else {
            id_switch.isChecked = false
        }

        id_back.setOnClickListener {
            finish()
        }

        id_replace_image.setOnClickListener {
            setPermission()
        }

        id_male.setOnClickListener {
            setGenderUI(1)
        }

        id_female.setOnClickListener {
            setGenderUI(2)
        }

        id_other.setOnClickListener {
            setGenderUI(3)
        }

        id_birth_date.setOnClickListener {
            showDatePicker()
        }

        id_save.setOnClickListener {
            if (id_name_edit_text.text.toString().isEmpty() || id_email_edit_text.text.toString().isEmpty() || genderIndex == 0 || birthDate.isEmpty()) {
                val toast = Toast.makeText(this, getString(R.string.alert_empty), Toast.LENGTH_LONG)
                toast.show()
            } else {
                id_save.visibility = View.GONE
                id_save_progress_bar.visibility = View.VISIBLE
                save()
            }
        }

        id_setup_profile.setOnClickListener {
            val intent = Intent(this, SecondProfileSetupActivity::class.java)
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

        id_delete_account.setOnClickListener {
            openURL(Constants.deleteAccountURL)
        }
    }

    fun setGender(gender: String) {
        if (gender == "M") {
            genderIndex = 1
        } else if (gender == "F") {
            genderIndex = 2
        } else if (gender == "O") {
            genderIndex = 3
        }
        setGenderUI(genderIndex)
    }

    fun setBirthdate(date: String) {
        birthDate = date

        val dateParser = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        val dateFormatter = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        val date = dateParser.parse(date)!!
        val dateString = dateFormatter.format(date)

        calendar.time = date
        id_birth_date.setText(dateString)
        id_birth_date.setTextColor(getColor(R.color.black))
    }

    fun setPermission() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.CAMERA), 100)
        }
        showPictureDialog()
    }

    fun showPictureDialog() {
        val pictureDialog = AlertDialog.Builder(this)
        pictureDialog.setTitle(getString(R.string.select_option))
        val pictureDialogItems = arrayOf(getString(R.string.gallery), getString(R.string.camera))
        pictureDialog.setNegativeButton(getString(R.string.cancel), null)

        pictureDialog.setItems(pictureDialogItems) {
            _, which ->

            when (which) {
                0 -> choosePhotoFromGallary()
                1 -> takePhotoFromCamera()
            }
        }
        pictureDialog.show()
    }

    fun choosePhotoFromGallary() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, GALLERY_RESULT)
    }

    fun takePhotoFromCamera() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "New Image")
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera")
        imageURI = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageURI)
        startActivityForResult(intent, CAMERA_RESULT)
    }

    fun resize(bitmap: Bitmap, scale: Int): Bitmap {
        val max = Math.max(bitmap.width, bitmap.height)
        val newWidth = (bitmap.width * scale) / max
        val newHeight = (bitmap.height * scale) / max
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, false)
    }

    fun setGenderUI(index: Int) {
        val textViews = arrayOf(id_male, id_female, id_other)
        val layouts = arrayOf(id_male_layout, id_female_layout, id_other_layout)

        if (genderIndex != 0) {
            textViews[genderIndex - 1].setTextColor(getColor(R.color.dark_gray))
            layouts[genderIndex - 1].setBackgroundResource(R.drawable.view_drawable_edit_text)
        }
        if (index != 0) {
            textViews[index - 1].setTextColor(getColor(R.color.white))
            layouts[index - 1].setBackgroundResource(R.drawable.view_drawable_selected)
        }
        genderIndex = index
    }

    fun showDatePicker() {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePicker = DatePickerDialog(this, {
            _, year, monthOfYear, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, monthOfYear)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            var dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
            birthDate = dateFormatter.format(calendar.time)
            dateFormatter = SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH)
            val dateString = dateFormatter.format(calendar.time)
            id_birth_date.setText(dateString)
            id_birth_date.setTextColor(getColor(R.color.black))
        }, year, month, day)

        datePicker.show()
    }

    fun save() {
        val preferences = getSharedPreferences("Preferences", MODE_PRIVATE)
        val token = preferences.getString("Token", "")
        val url = Constants.url + "edit_profile.php"

        val request = object: VolleyFileUploadRequest(
            Method.POST, url,
            Response.Listener { response ->
                try {
                    saveSuccess(response.data.toString(Charsets.UTF_8))
                } catch (e: JSONException) {
                    val toast = Toast.makeText(this, e.toString(), Toast.LENGTH_LONG)
                    toast.show()
                }
            },
            Response.ErrorListener {
                saveError()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = token!!
                return headers
            }
            override fun getByteData(): MutableMap<String, FileDataPart> {
                val params = HashMap<String, FileDataPart>()
                if (bitmap != null) {
                    val outputStream = ByteArrayOutputStream()
                    bitmap!!.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
                    val byteArray = outputStream.toByteArray()
                    params["image"] = FileDataPart("image.jpeg", byteArray, "jpeg")
                }
                return params
            }
            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["language"] = getString(R.string.language)
                params["name"] = id_name_edit_text.text.toString()
                params["email"] = id_email_edit_text.text.toString()
                params["phone"] = phone
                params["gender"] = getGender()
                params["birth"] = birthDate
                params["age_privacy"] = getAgePrivacy()
                return params
            }
        }
        Singleton.getInstance(this).addToRequestQueue(request)
    }

    fun saveError() {
        println("Error2")
        save()
    }

    fun saveSuccess(response: String) {
        val json = JSONObject(response)
        val error = json.getString("error")

        if (error == "0") {
            Constants.reloadProfile()

            val alertDialog = AlertDialog.Builder(this)
            alertDialog.setTitle(getString(R.string.alert_info_edited))
            alertDialog.setPositiveButton(getString(R.string.ok)) {
                _, _ ->
                finish()
            }
            val alert = alertDialog.create()
            alert.show()
        } else {
            val message = json.getString("message")
            val toast = Toast.makeText(this, message, Toast.LENGTH_LONG)
            toast.show()
        }
        id_save.visibility = View.VISIBLE
        id_save_progress_bar.visibility = View.GONE
    }

    fun getGender(): String {
        val genders = arrayOf("M", "F", "O")
        return genders[genderIndex - 1]
    }

    fun getAgePrivacy(): String {
        if (id_switch.isChecked) {
            return "private"
        } else {
            return "public"
        }
    }

    fun openURL(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))

        try {
            startActivity(intent)
        } catch (e: Exception) {

        }
    }
}
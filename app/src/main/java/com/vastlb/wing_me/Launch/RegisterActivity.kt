
package com.vastlb.wing_me.Launch

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ContentValues
import android.content.Context
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
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.GraphRequest
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.vastlb.wing_me.Classes.Constants
import com.vastlb.wing_me.DataClasses.PickerClass
import com.vastlb.wing_me.Main.MainActivity
import com.vastlb.wing_me.Main.PickerFragment
import com.vastlb.wing_me.R
import com.vastlb.wing_me.Supabase.SupabaseAuth
import com.vastlb.wing_me.Supabase.SupabaseData
import kotlinx.android.synthetic.main.activity_register.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class RegisterActivity: AppCompatActivity() {

    val fragmentManager = supportFragmentManager
    val pickerFragment = PickerFragment.newInstance()

    val callbackManager = CallbackManager.Factory.create()
    val calendar = Calendar.getInstance()

    val GALLERY_RESULT= 1
    val CAMERA_RESULT = 2

    var imageURI: Uri? = null
    var bitmap: Bitmap? = null

    var genderIndex = 0
    var birthDate = ""

    var isOpen = false

    var code = "1"

    // Same two-step OTP pattern as LoginActivity: tap 1 sends the code, tap 2 (same
    // button, same text field reused) verifies it and then creates the profile row.
    // Avatar upload is intentionally NOT wired to Supabase Storage — it wasn't part of
    // the requested SupabaseData surface for this pass, so the photo the user picks is
    // shown locally but not persisted (avatar_url stays null). See final report.
    var otpSent = false
    var otpPhone = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
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
        } else {
            callbackManager.onActivityResult(requestCode, resultCode, data)
        }
    }

    @SuppressLint("MissingSuperCall")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (isOpen) {
            isOpen = false
            fragmentManager.beginTransaction().hide(pickerFragment).commit()
        } else {
            finish()
        }
    }

    fun setViews() {
        fragmentManager.beginTransaction().add(R.id.id_container, pickerFragment).hide(pickerFragment).commit()

        pickerFragment.close = {
            isOpen = false
            fragmentManager.beginTransaction().hide(pickerFragment).commit()
        }

        id_country_code.setOnClickListener {
            openPicker()
        }

        id_back.setOnClickListener {
            finish()
        }

        id_add_image.setOnClickListener {
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

        id_register.setOnClickListener {
            if (!otpSent) {
                if (id_name_edit_text.text.toString().isEmpty() || Constants.getPhone(id_phone_edit_text).isEmpty()
                    || genderIndex == 0 || birthDate.isEmpty()) {

                    val toast = Toast.makeText(this, getString(R.string.alert_empty), Toast.LENGTH_LONG)
                    toast.show()
                } else {
                    id_register.visibility = View.GONE
                    id_register_progress_bar.visibility = View.VISIBLE
                    sendOtp()
                }
            } else {
                if (id_password_edit_text.text.toString().isEmpty()) {
                    val toast = Toast.makeText(this, getString(R.string.alert_empty), Toast.LENGTH_LONG)
                    toast.show()
                } else {
                    id_register.visibility = View.GONE
                    id_register_progress_bar.visibility = View.VISIBLE
                    verifyOtpAndCreateProfile()
                }
            }
        }

        id_facebook.setOnClickListener {
            id_facebook.visibility = View.GONE
            id_facebook_progress_bar.visibility = View.VISIBLE
            facebookRegister()
        }
    }

    @SuppressLint("SetTextI18n")
    fun openPicker() {
        pickerFragment.select = {
            index ->
            val jsonObject = pickerFragment.array[index]
            code = jsonObject.string1
            id_country_code.setText("+${code}")
        }
        pickerFragment.array.clear()

        val countries = Constants.getCountries()

        for (index in 0..(countries.size - 1)) {
            val jsonObject = countries[index]
            val pickerClass = PickerClass(jsonObject.string2, jsonObject.string1)
            pickerFragment.array.add(pickerClass)
        }
        pickerFragment.adapter.notifyDataSetChanged()

        isOpen = true
        fragmentManager.beginTransaction().show(pickerFragment).commit()
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
            dialog, which ->
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
        textViews[index - 1].setTextColor(getColor(R.color.white))
        layouts[index - 1].setBackgroundResource(R.drawable.view_drawable_selected)

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

    fun sendOtp() {
        otpPhone = "+" + code + Constants.getPhone(id_phone_edit_text)

        SupabaseAuth.sendPhoneOtp(this, otpPhone, onSuccess = {
            otpSent = true
            id_password_edit_text.setText("")
            id_password_edit_text.hint = "Enter the code we sent you"
            id_register.visibility = View.VISIBLE
            id_register_progress_bar.visibility = View.GONE

            val toast = Toast.makeText(this, "Code sent to $otpPhone", Toast.LENGTH_LONG)
            toast.show()
        }, onError = { message ->
            id_register.visibility = View.VISIBLE
            id_register_progress_bar.visibility = View.GONE

            val toast = Toast.makeText(this, message, Toast.LENGTH_LONG)
            toast.show()
        })
    }

    fun verifyOtpAndCreateProfile() {
        val otpCode = id_password_edit_text.text.toString()

        SupabaseAuth.verifyPhoneOtp(this, otpPhone, otpCode, onSuccess = { _, userId ->
            val fields = JSONObject()
            fields.put("id", userId)
            fields.put("display_name", id_name_edit_text.text.toString())
            fields.put("phone", otpPhone)
            fields.put("gender", getGender())
            fields.put("date_of_birth", birthDate)

            SupabaseData.upsertProfile(this, fields, onSuccess = {
                openMain()
            }, onError = { message ->
                // Auth already succeeded and the session is saved — don't strand the
                // user on this screen over a profile-save hiccup, but let them know.
                val toast = Toast.makeText(this, message, Toast.LENGTH_LONG)
                toast.show()
                openMain()
            })
        }, onError = { message ->
            id_register.visibility = View.VISIBLE
            id_register_progress_bar.visibility = View.GONE

            val toast = Toast.makeText(this, message, Toast.LENGTH_LONG)
            toast.show()
        })
    }

    fun getGender(): String {
        val genders = arrayOf("M", "F", "O")
        return genders[genderIndex - 1]
    }

    fun openMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    fun facebookRegister() {
        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("public_profile", "email"))

        LoginManager.getInstance().registerCallback(callbackManager, object:
            FacebookCallback<LoginResult> {

            override fun onSuccess(result: LoginResult) {
                val request = GraphRequest.newMeRequest(result.accessToken) {
                    jsonObject, response ->

                    try {
                        facebookSuccess(jsonObject!!)
                    } catch (e: Exception) {
                        id_facebook.visibility = View.VISIBLE
                        id_facebook_progress_bar.visibility = View.GONE

                        val toast = Toast.makeText(this@RegisterActivity, e.toString(), Toast.LENGTH_LONG)
                        toast.show()
                    }
                }
                val parameters = Bundle()
                parameters.putString("fields", "id, name, email, gender, birthday, picture.width(1080).height(1080)")
                request.parameters = parameters
                request.executeAsync()
            }

            override fun onCancel() {
                id_facebook.visibility = View.VISIBLE
                id_facebook_progress_bar.visibility = View.GONE

                val toast = Toast.makeText(this@RegisterActivity, "Cancelled", Toast.LENGTH_LONG)
                toast.show()
            }

            override fun onError(error: FacebookException) {
                id_facebook.visibility = View.VISIBLE
                id_facebook_progress_bar.visibility = View.GONE

                val toast = Toast.makeText(this@RegisterActivity, error.toString(), Toast.LENGTH_LONG)
                toast.show()
            }
        })
    }

    fun facebookSuccess(jsonObject: JSONObject) {
        val id = jsonObject.getString("id")
        val name = jsonObject.getString("name")
        val email = jsonObject.getString("email")
        val picture = jsonObject.getJSONObject("picture")
        val data = picture.getJSONObject("data")
        val url = data.getString("url")

        val intent = Intent(this, FacebookRegisterActivity::class.java)
        intent.putExtra("ID", id)
        intent.putExtra("Name", name)
        intent.putExtra("Email", email)
        intent.putExtra("ImageURL", url)
        startActivity(intent)

        id_facebook.visibility = View.VISIBLE
        id_facebook_progress_bar.visibility = View.GONE
    }
}
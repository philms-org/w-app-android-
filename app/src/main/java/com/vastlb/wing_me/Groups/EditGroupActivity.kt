
package com.vastlb.wing_me.Groups

import android.app.Activity
import android.app.AlertDialog
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
import kotlinx.android.synthetic.main.activity_edit_group.*
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.*

class EditGroupActivity: AppCompatActivity() {

    val GALLERY_RESULT= 1
    val CAMERA_RESULT = 2

    var imageURI: Uri? = null
    var bitmap: Bitmap? = null

    var groupID = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_group)
        setViews()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode:Int, resultCode:Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == GALLERY_RESULT) {
            if (data != null) {
                bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, data.data)
                bitmap = resize(bitmap!!, 600)
                id_image_view.setImageBitmap(bitmap)
            }
        } else if (requestCode == CAMERA_RESULT && resultCode == Activity.RESULT_OK) {
            bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, imageURI)
            bitmap = resize(bitmap!!, 600)
            id_image_view.setImageBitmap(bitmap)
        }
    }

    fun setViews() {
        groupID = intent.getStringExtra("GroupID")!!
        val image = intent.getStringExtra("Image")!!
        val name = intent.getStringExtra("Name")!!

        Picasso.get().load(Constants.url + image).into(id_image_view)

        id_name_edit_text.setText(name)

        id_back.setOnClickListener {
            finish()
        }

        id_add_image.setOnClickListener {
            setPermission()
        }

        id_save.setOnClickListener {
            if (id_name_edit_text.text.toString().isEmpty()) {
                val toast = Toast.makeText(this, getString(R.string.alert_empty), Toast.LENGTH_LONG)
                toast.show()
            } else {
                id_save.visibility = View.GONE
                id_save_progress_bar.visibility = View.VISIBLE
                save()
            }
        }
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

    fun save() {
        val preferences = getSharedPreferences("Preferences", MODE_PRIVATE)
        val token = preferences.getString("Token", "")
        val url = Constants.url + "edit_group.php"

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
                    params["main_image"] = FileDataPart("main_image.jpeg", byteArray, "jpeg")
                }
                return params
            }
            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["language"] = getString(R.string.language)
                params["comment_Id"] = groupID
                params["title"] = id_name_edit_text.text.toString()
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
            Constants.reloadGroup()
            finish()
        } else {
            val message = json.getString("message")
            val toast = Toast.makeText(this, message, Toast.LENGTH_LONG)
            toast.show()
        }
        id_save.visibility = View.VISIBLE
        id_save_progress_bar.visibility = View.GONE
    }
}
package com.muzafferatmaca.artbook

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.muzafferatmaca.artbook.databinding.ActivityDetailBinding
import java.io.ByteArrayOutputStream
import java.lang.Exception

class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    var selectedBitmap: Bitmap? = null
    private lateinit var database : SQLiteDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        database = this.openOrCreateDatabase("Arts", MODE_PRIVATE,null)

        registerLauncher()

        val intent = intent
        val info = intent.getStringExtra("info")
        if (info.equals("new")){

            binding.artNameText.setText("")
            binding.artistNameText.setText("")
            binding.yearText.setText("")
            binding.selectImageView.setImageResource(R.drawable.ic_baseline_image_search_24)
            binding.saveButton.visibility = View.VISIBLE

        }else{
            binding.saveButton.visibility= View.INVISIBLE
            val selectedId = intent.getIntExtra("id",1)

            val cursor = database.rawQuery("SELECT * FROM arts WHERE id = ?", arrayOf(selectedId.toString()))

            val artNameIx = cursor.getColumnIndex("artname")
            val artistNameIx = cursor.getColumnIndex("artistname")
            val yearIx = cursor.getColumnIndex("year")
            val imageIx = cursor.getColumnIndex("image")

            while (cursor.moveToNext()){
                binding.artNameText.setText(cursor.getString(artNameIx))
                binding.artistNameText.setText(cursor.getString(artistNameIx))
                binding.yearText.setText(cursor.getString(yearIx))

                val byteArray = cursor.getBlob(imageIx)
                val bitmap = BitmapFactory.decodeByteArray(byteArray,0,byteArray.size)
                binding.selectImageView.setImageBitmap(bitmap)
            }

            cursor.close()

        }


    }

    fun save(view: View) {

        var artName = binding.artNameText.text.toString()
        var artistName = binding.artistNameText.text.toString()
        var year = binding.yearText.text.toString()

        if (selectedBitmap != null){
            val smallBitmap = makeSmallerBitmap(selectedBitmap!!,300)

            val outputStream = ByteArrayOutputStream()
            smallBitmap.compress(Bitmap.CompressFormat.PNG,50,outputStream)
            val byteArray = outputStream.toByteArray()

            try {

                database = this.openOrCreateDatabase("Arts", MODE_PRIVATE,null)
                database.execSQL("CREATE TABLE IF NOT EXISTS arts (id INTEGER PRIMARY KEY, artname VARCHAR, artistname VARCHAR, year VARCHAR, image BLOB)")
                val sqlString = "INSERT INTO arts (artname, artistname, year, image) VALUES (?, ?, ?, ?)"
                val statement = database.compileStatement(sqlString)
                statement.bindString(1,artName)
                statement.bindString(2,artistName)
                statement.bindString(3,year)
                statement.bindBlob(4,byteArray)
                statement.execute()

            }catch (e : Exception){
                e.printStackTrace()
            }

            val intent = Intent(this@DetailActivity,MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)

        }


    }

    private fun makeSmallerBitmap(image: Bitmap,maximumSize : Int) : Bitmap{

        var width = image.width
        var height = image.height

        val bitmapRatio : Double = width.toDouble() / height.toDouble()

        if (bitmapRatio>1){
            //landscape
            width = maximumSize
            val scaledHeight = width / bitmapRatio
            height = scaledHeight.toInt()

        }else{
            //portrait
            height = maximumSize
            val scaledWidth = height * bitmapRatio
            width = scaledWidth.toInt()

        }


        return Bitmap.createScaledBitmap(image,width,height,true)
    }

    fun selectImage(view: View) {

        if (ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE)) {
                //rational
                Snackbar.make(view, "needed permission to gallery", Snackbar.LENGTH_INDEFINITE).setAction("give permission", View.OnClickListener {

                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)


                    }).show()

            } else {
                //request permission
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }

        } else {
            val intentToGallery =Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            activityResultLauncher.launch(intentToGallery)


        }


    }

    private fun registerLauncher() {

        activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

                if (result.resultCode == RESULT_OK) {
                    val resultFromData = result.data
                    if (resultFromData != null) {
                        val imageData = resultFromData.data
                        //binding.selectImageView.setImageURI(imageData)

                        if (imageData != null) {

                            try {

                                if (Build.VERSION.SDK_INT >= 28) {
                                    val source = ImageDecoder.createSource(
                                        this@DetailActivity.contentResolver,
                                        imageData
                                    )
                                    selectedBitmap = ImageDecoder.decodeBitmap(source)
                                    binding.selectImageView.setImageBitmap(selectedBitmap)
                                } else {
                                    selectedBitmap = MediaStore.Images.Media.getBitmap(
                                        contentResolver,
                                        imageData
                                    )
                                    binding.selectImageView.setImageBitmap(selectedBitmap)
                                }

                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                        }
                    }
                }
            }
        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->

                if (result) {
                    //permission granted
                    val intentToGallery =
                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    activityResultLauncher.launch(intentToGallery)

                } else {
                    //permission denied
                    Toast.makeText(this@DetailActivity, "permission needed", Toast.LENGTH_LONG)
                        .show()

                }
            }
    }
}
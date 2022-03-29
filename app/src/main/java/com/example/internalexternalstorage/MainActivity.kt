package com.example.internalexternalstorage

import android.Manifest
import android.R.attr
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import java.io.*
import java.lang.Exception
import java.nio.charset.Charset
import android.R.attr.path
import android.content.ContentValues
import android.graphics.Bitmap
import android.provider.MediaStore
import android.view.View
import androidx.activity.result.launch
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {

    private var isPersistent = false
    private var isInternal = false
    private val fileName = "pdp_internal.txt"
    private var readPermissionGranted = false
    private var writePermissionGranted = false
    private lateinit var b_call_permission: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        createInternalFIle()

        requestPermissions()

        initViews()

      //  checkStoragePath()

    }

    private fun initViews() {
        val b_save_int: Button = findViewById(R.id.b_save_int)
        val b_read_int: Button = findViewById(R.id.b_read_int)
        val b_delete_int: Button = findViewById(R.id.b_delete_int)
        val b_save_ext: Button = findViewById(R.id.b_save_ext)
        val b_read_ext: Button = findViewById(R.id.b_read_ext)
        val b_delete_ext: Button = findViewById(R.id.b_delete_ext)
        val b_take_photo: Button = findViewById(R.id.b_take_photo)
        b_call_permission = findViewById(R.id.b_call_permission)

        b_save_int.setOnClickListener{
            saveInternalFile("we will do the best project for people!!!")
        }

        b_read_int.setOnClickListener{
            readInternalFile()
        }

        b_delete_int.setOnClickListener{
            deleteInternalFile()
        }

        b_save_ext.setOnClickListener{
            saveExternalFile("SAVE EXTERNAL STORAGE")
        }

        b_read_ext.setOnClickListener{
            readExternalFIle()
        }

        b_delete_ext.setOnClickListener{
            deleteExternalStorage()
        }

        b_take_photo.setOnClickListener{
            takePhoto.launch()
        }

        b_call_permission.setOnClickListener{
            callAppSettings()
        }

    }


    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicturePreview()){bitmap ->

        val fileName1 = UUID.randomUUID().toString()

        val isPhotoSaved = if (isInternal){
            savePhotoToInternalStorage(fileName1, bitmap!!)
        }else{
            if (writePermissionGranted){
                savePhotoToExternalStorage(fileName1, bitmap!!)
            }else{
                false
            }
        }
        if (isPhotoSaved){
            Toast.makeText(this, "Photo saved Successfully", Toast.LENGTH_SHORT).show()
        }else{
            Toast.makeText(this, "Failed to save photo", Toast.LENGTH_SHORT).show()
        }

    }

    private fun savePhotoToExternalStorage(fileName1: String, bitmap: Bitmap?): Boolean {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        }else{
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$fileName1.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.WIDTH, bitmap!!.width)
            put(MediaStore.Images.Media.HEIGHT, bitmap.height)
        }
        return try {
            contentResolver.insert(collection, contentValues)?.also { uri ->
                contentResolver.openOutputStream(uri).use { outputStream ->
                    if (!bitmap!!.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)){
                        throw IOException("Couldn`t save bitmap")
                    }
                }
            }?: throw IOException("Couldn`t create MediaStore entry")
            true
        }catch (e: IOException){
            e.printStackTrace()
            false
        }

    }

    private fun savePhotoToInternalStorage(fileName1: String, bitmap: Bitmap?): Boolean {
        return try{
            openFileOutput("$fileName1.jpg", MODE_PRIVATE).use { stream ->
                if (!bitmap!!.compress(Bitmap.CompressFormat.JPEG, 95, stream)){
                    throw IOException("Couldn`t save bitmap.")
                }
            }
            true
        }catch (e: IOException){
            e.printStackTrace()
            false
        }
    }


    private fun deleteExternalStorage(){
        val absolutePath = if(isPersistent) getExternalFilesDir(null)!!.absolutePath else externalCacheDir!!.absolutePath
        val file = File(absolutePath, fileName)
        val checked = file.delete()
        if (checked) {
            Toast.makeText(this, String.format("Delete %s successful", fileName), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, String.format("Delete %s Failed", fileName), Toast.LENGTH_SHORT).show()
        }
    }

    private fun readExternalFIle(){
        val file: File = if (isPersistent){
            File(getExternalFilesDir(null), fileName)
        }else{
            File(externalCacheDir, fileName)
        }
        try {
            val fileInputStream = FileInputStream(file)
            val inputStreamReader = InputStreamReader(fileInputStream, Charset.forName("UTF-8"))
            val lines: MutableList<String?> = ArrayList()
            val reader = BufferedReader(inputStreamReader)
            var line = reader.readLine()
            while (line != null){
                lines.add(line)
                line = reader.readLine()
            }
            val readText = TextUtils.join("\n", lines)
            Toast.makeText(this, String.format("Read from file %s successful", fileName), Toast.LENGTH_SHORT).show()
        }catch (e: java.lang.Exception){
            e.printStackTrace()
            Toast.makeText(this, String.format("Read from file %s failed", fileName), Toast.LENGTH_SHORT).show()
        }

    }

    private fun saveExternalFile(data: String){
        val file: File = if (isPersistent){
            File(getExternalFilesDir(null), fileName)
        }else{
            File(externalCacheDir, fileName)
        }
        try {
            val fileOutputStream = FileOutputStream(file)
            fileOutputStream.write(data.toByteArray(Charset.forName("UTF-8")))
            Toast.makeText(this, String.format("Write to %s successful", fileName), Toast.LENGTH_SHORT).show()
        }catch (e: java.lang.Exception){
            e.printStackTrace()
            Toast.makeText(this, String.format("Write to %s failed", fileName), Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestPermissions(){
        val hasReadPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        val hasWritePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        val minSdk29 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        readPermissionGranted = hasReadPermission
        writePermissionGranted = hasWritePermission || minSdk29

        val permissionsToRequest = mutableListOf<String>()
        if (!readPermissionGranted){
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (!writePermissionGranted){
            permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (permissionsToRequest.isNotEmpty()){
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){ permissions ->
        readPermissionGranted = permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: readPermissionGranted
        writePermissionGranted = permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] ?: writePermissionGranted

        if (writePermissionGranted) Toast.makeText(this, "WRITE_EXTERNAL_STORAGE", Toast.LENGTH_SHORT).show()
        if (readPermissionGranted){
            Toast.makeText(this, "READ_EXTERNAL_STORAGE", Toast.LENGTH_SHORT).show()
            b_call_permission.visibility = View.GONE
        } else {
            Toast.makeText(this, "READ_EXTERNAL_STORAGE is Failed", Toast.LENGTH_SHORT).show()
            b_call_permission.visibility = View.VISIBLE
        }

    }

    private fun callAppSettings(){
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri: Uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    private fun readInternalFile(){
        try {
            val fileInputStream: FileInputStream
            fileInputStream = if (isPersistent){
                openFileInput(fileName)
            }else{
                val file = File(cacheDir, fileName)
                FileInputStream(file)
            }
            val inputStreamReader = InputStreamReader(fileInputStream, Charset.forName("UTF-8"))
            val lines: MutableList<String?> = ArrayList()
            val reader = BufferedReader(inputStreamReader)
            var line = reader.readLine()
            while (line != null){
                lines.add(line)
                line = reader.readLine()
            }
            val readText = TextUtils.join("\n", lines)
            Toast.makeText(this, String.format("Read from file %s successful", fileName), Toast.LENGTH_SHORT).show()
        }catch (e: java.lang.Exception){
            e.printStackTrace()
            Toast.makeText(this, String.format("Read from file %s failed", fileName), Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveInternalFile(data: String){
        try {
            val fileOutputStream: FileOutputStream
            fileOutputStream = if (isPersistent){
                openFileOutput(fileName, MODE_PRIVATE)
            }else{
                val file = File(cacheDir, fileName)
                FileOutputStream(file)
            }
            fileOutputStream.write(data.toByteArray(Charset.forName("UTF-8")))
            Toast.makeText(this, String.format("Write to %s successful", fileName), Toast.LENGTH_SHORT).show()
        }catch (e: Exception){
            e.printStackTrace()
            Toast.makeText(this, String.format("Write to file %s failed", fileName), Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteInternalFile(){
        val file = if(isPersistent) File(filesDir.absolutePath, fileName) else File(cacheDir.absolutePath, fileName)
        val checked = file.delete()
        if (checked) Toast.makeText(this, String.format("Deleted %s Internal Storage is Successful", fileName), Toast.LENGTH_SHORT).show()
        else Toast.makeText(this, String.format("Deleted %s Internal Storage is Failed", fileName), Toast.LENGTH_SHORT).show()
    }

    private fun createInternalFIle() {
        val file: File = if (isPersistent) {
            File(filesDir, fileName)
        } else {
            File(cacheDir, fileName)
        }

        if (!file.exists()) {
            try {
                file.createNewFile()
                Toast.makeText(this, String.format("File %s has been created", fileName), Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                Toast.makeText(this, String.format("File %s creation failed", fileName), Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, String.format("File %s already exists", file), Toast.LENGTH_SHORT).show()
        }

    }

    private fun checkStoragePath() {
        val internal_m1 = getDir("custom", 0)
        val internal_m2 = filesDir

        val external_m1 = getExternalFilesDir(null)
        val external_m2 = externalCacheDir
        val external_m3 = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        Log.d("StorageActivity", internal_m1.absolutePath)
        Log.d("StorageActivity", internal_m2.absolutePath)
        Log.d("StorageActivity", external_m1!!.absolutePath)
        Log.d("StorageActivity", external_m2!!.absolutePath)
        Log.d("StorageActivity", external_m3!!.absolutePath)

    }

}
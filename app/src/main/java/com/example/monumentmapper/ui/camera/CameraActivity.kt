package com.example.monumentmapper.ui.camera

import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.example.monumentmapper.databinding.ActivityCameraBinding
import com.example.monumentmapper.ui.CustomInfoWindow
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Take a photo.
 *
 * Based on:
 * - https://www.youtube.com/watch?v=XUN6mUQiDpg (set-up)
 * - https://www.youtube.com/watch?v=fazzQs-O31U (camera controller)
 */
class CameraActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityCameraBinding
    private lateinit var cameraController: LifecycleCameraController
    private lateinit var monumentName: String

    override fun onCreate(savedInstanceState: Bundle?) {

        // Get monument name
        // How to get info from intent from: https://stackoverflow.com/a/8765766
        val extras = intent.extras
        if (extras != null && extras.containsKey(CustomInfoWindow.NAME_KEY)) {
            monumentName = extras.getString(CustomInfoWindow.NAME_KEY).toString()
        }

        // Create the view
        super.onCreate(savedInstanceState)
        viewBinding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        //setContentView(R.layout.activity_camera)

        // Check if permissions were already granted, maybe in a prev session
        if (!hasPermissions(baseContext)) {
            activityResultLauncher.launch(REQUIRED_PERMISSIONS)
        }
        else {
            // Start the camera
            startCamera()
        }

        viewBinding.takePhotoButton.setOnClickListener { takePhoto() }

    }


    /**
     * Start the camera.
     */
    private fun startCamera() {
        val previewView: PreviewView = viewBinding.viewFinder
        cameraController = LifecycleCameraController(baseContext)
        cameraController.bindToLifecycle(this)
        // will use back camera by default, so no need to set it here
        previewView.controller = cameraController
    }


    /**
     * Take a photo and save it with the monument's name + timestamp.
     */
    private fun takePhoto() {

        val timestamp = SimpleDateFormat(TIMESTAMP_FORMAT, Locale.UK).format(System.currentTimeMillis())
        val fileName = "$monumentName $timestamp"

        // Create MediaStore entry
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Monument Mapper")
            }

        }

        // Create output options object (file + metadata)
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
            .build()

        // Actually take the picture
        cameraController.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {

                override fun onError(exc: ImageCaptureException) {
                    val message = "Photo could not be saved"
                    Toast.makeText(baseContext, message, Toast.LENGTH_SHORT).show()
                    Log.e("CAMERA", "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val message = "Photo saved at: ${output.savedUri}"
                    Toast.makeText(baseContext, message, Toast.LENGTH_SHORT).show()
                    Log.i("CAMERA", "Photo saved")
                }
            }
        )
    }



    private val activityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()) {
                permissions ->
                    var permissionGranted = true

                    permissions.entries.forEach {
                        if (it.key in REQUIRED_PERMISSIONS && !it.value) {
                            permissionGranted = false
                        }
                    }

                if (!permissionGranted) {
                    Toast.makeText(this, "Permission request denied", Toast.LENGTH_LONG).show()
                }
                else {
                    // Can start the camera
                    startCamera()
                }
            }

    companion object {

        private const val TIMESTAMP_FORMAT = "yyyy-MM-dd HH-mm-ss"

        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                android.Manifest.permission.CAMERA
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()

        fun hasPermissions(context: Context) = REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}
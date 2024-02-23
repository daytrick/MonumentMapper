package com.example.monumentmapper.ui.camera

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.example.monumentmapper.R
import com.example.monumentmapper.databinding.ActivityCameraBinding

/**
 * Take a photo.
 *
 * Based on: https://www.youtube.com/watch?v=XUN6mUQiDpg
 */
class CameraActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityCameraBinding

    private lateinit var cameraController: LifecycleCameraController
    override fun onCreate(savedInstanceState: Bundle?) {
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


    private fun takePhoto() {

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
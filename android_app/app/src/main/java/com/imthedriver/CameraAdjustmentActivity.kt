package com.imthedriver

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.imthedriver.databinding.ActivityCameraAdjustmentBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraAdjustmentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraAdjustmentBinding
    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var camera: Camera? = null
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraAdjustmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize the camera executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Start the camera when the activity is created
        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: throw IllegalStateException("Camera initialization failed.")
        val rotation = windowManager.defaultDisplay.rotation

        // Select the back camera as default
        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

        preview = Preview.Builder()
            .setTargetRotation(rotation)
            .build()

        preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)

        cameraProvider.unbindAll()

        try {
            camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview
            )
            Log.d("CameraAdjustment", "Camera bound to lifecycle successfully")
        } catch (exc: Exception) {
            Log.e("CameraAdjustment", "Use case binding failed", exc)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Shutdown the camera executor
        cameraExecutor.shutdown()
    }
}

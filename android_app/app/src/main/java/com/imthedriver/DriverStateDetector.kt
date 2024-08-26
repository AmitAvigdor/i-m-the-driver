package com.imthedriver

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.imthedriver.databinding.ActivityDriverStateDetectorBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.bumptech.glide.Glide
import com.imthedriver.R
import java.io.ByteArrayOutputStream


class DriverStateDetector : AppCompatActivity(), Detector.DetectorListener {

    private lateinit var binding: ActivityDriverStateDetectorBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var detector: Detector
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var preview: Preview? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var yawnCount = 0
    private val yawnThreshold = 3
    private val yawnResetInterval: Long = 60000 // 1 minute
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var sharedPreferences: SharedPreferences
    private var eyeClosureSoundEnabled = true
    private var yawnSoundEnabled = true

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDriverStateDetectorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set camera preview view to remain hidden
        binding.viewFinder.visibility = View.VISIBLE

        // Display the monitoring text
        binding.monitoringText.visibility = View.VISIBLE

        // Load the GIF using Glide
        Glide.with(this)
            .asGif()
            .load(R.drawable.car_animation)
            .into(binding.drivingGifView)

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Load settings and initialize detector
        sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE)
        eyeClosureSoundEnabled = sharedPreferences.getBoolean("eyeClosureSound", true)
        yawnSoundEnabled = sharedPreferences.getBoolean("yawnSound", true)

        detector = Detector(baseContext, Constants.MODEL_PATH, Constants.LABELS_PATH, this)
        detector.setup()

        // Check permissions and start camera
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        // Emergency Call and Text Button Setup
        setupEmergencyButtons()

        setupBottomNavigation()
    }

    private fun setupEmergencyButtons() {
        binding.emergencyCallButton.setOnClickListener {
            makeEmergencyCall()
        }

        binding.sendEmergencyTextButton.setOnClickListener {
            sendEmergencyText()
        }
    }

    private fun makeEmergencyCall() {
        val emergencyNumber = sharedPreferences.getString("emergencyCallNumber", "")
        if (emergencyNumber.isNullOrEmpty()) {
            Toast.makeText(this, "Please set an emergency contact number in the settings.", Toast.LENGTH_LONG).show()
        } else {
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$emergencyNumber"))
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                startActivity(intent)
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), REQUEST_CODE_CALL)
            }
        }
    }

    private fun sendEmergencyText() {
        val emergencyNumber = sharedPreferences.getString("emergencyCallNumber", "")
        if (emergencyNumber.isNullOrEmpty()) {
            Toast.makeText(this, "Please set an emergency contact number in the settings.", Toast.LENGTH_LONG).show()
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        val message = "Emergency! Please help me, I need assistance. Location: https://maps.google.com/?q=${location.latitude},${location.longitude}"
                        val smsManager = SmsManager.getDefault()
                        smsManager.sendTextMessage(emergencyNumber, null, message, null, null)
                        Toast.makeText(this, "Emergency message sent with location.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Could not get location. Emergency message sent without location.", Toast.LENGTH_LONG).show()
                        sendEmergencyTextWithoutLocation(emergencyNumber)
                    }
                }
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_CODE_SMS)
            }
        }
    }

    private fun sendEmergencyTextWithoutLocation(emergencyNumber: String) {
        val message = "Emergency! Please help me, I need assistance."
        val smsManager = SmsManager.getDefault()
        smsManager.sendTextMessage(emergencyNumber, null, message, null, null)
        Toast.makeText(this, "Emergency message sent.", Toast.LENGTH_LONG).show()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            try {
                bindCameraUseCases()
            } catch (e: Exception) {
                Log.e(TAG, "Use case binding failed: ${e.message}", e)
                Toast.makeText(this, "Camera initialization failed. Please restart the app.", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }


    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: throw IllegalStateException("Camera initialization failed.")
        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

        preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .build()

        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalyzer?.setAnalyzer(cameraExecutor) { imageProxy ->
            processImageProxy(imageProxy)
        }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageAnalyzer
            )
            preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            Log.d(TAG, "Camera bound to lifecycle successfully")
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed: ${exc.message}", exc)
        }
    }


    internal fun processImageProxy(imageProxy: ImageProxy) {
        val bitmap = imageProxyToBitmap(imageProxy)
        if (bitmap != null) {
            Log.d(TAG, "Image proxy to bitmap conversion successful")
            detector.detect(bitmap)
        } else {
            Log.e(TAG, "Image proxy to bitmap conversion failed")
        }
        imageProxy.close()
    }

    @OptIn(ExperimentalGetImage::class)
    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        val image = imageProxy.image ?: return null
        if (image.format != ImageFormat.YUV_420_888) {
            return null
        }

        val planes = imageProxy.planes
        val yPlane = planes[0].buffer
        val uPlane = planes[1].buffer
        val vPlane = planes[2].buffer

        val ySize = yPlane.remaining()
        val uSize = uPlane.remaining()
        val vSize = vPlane.remaining()

        val nv21ByteArray = ByteArray(ySize + uSize + vSize)
        yPlane.get(nv21ByteArray, 0, ySize)

        val chromaStride = planes[1].pixelStride
        for (i in 0 until uSize step chromaStride) {
            nv21ByteArray[ySize + i] = vPlane.get(i)
            nv21ByteArray[ySize + i + 1] = uPlane.get(i)
        }

        val yuvImage = android.graphics.YuvImage(
            nv21ByteArray,
            ImageFormat.NV21,
            imageProxy.width,
            imageProxy.height,
            null
        )

        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(
            android.graphics.Rect(0, 0, imageProxy.width, imageProxy.height),
            100,
            out
        )
        val jpegByteArray = out.toByteArray()
        return BitmapFactory.decodeByteArray(jpegByteArray, 0, jpegByteArray.size)
    }

    private fun startMonitoring() {
        // Ensure that the camera preview remains hidden (background operation)
        binding.viewFinder.visibility = View.VISIBLE
        binding.monitoringText.visibility = View.VISIBLE

        yawnCount = 0
        Log.d(TAG, "Monitoring started.")
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onEmptyDetect() {
        Log.d(TAG, "No objects detected.")
    }

    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
        runOnUiThread {
            var isYawning = false
            var isEyesClosed = false

            boundingBoxes.forEach {
                when (it.clsName) {
                    "Close" -> {
                        isEyesClosed = true
                        if (eyeClosureSoundEnabled) {
                            SoundUtils.playSound(this, R.raw.alert_sound)
                        }
                    }
                    "Yawn" -> isYawning = true
                }
            }

            // Show/Hide "Wake up!" alert based on eye closure detection
            if (isEyesClosed) {
                binding.eyesClosedAlert.visibility = View.VISIBLE
            } else {
                binding.eyesClosedAlert.visibility = View.GONE
            }

            // Show/Hide yawning recommendation based on yawn detection
            if (isYawning) {
                binding.yawnAlert.visibility = View.VISIBLE
                if (yawnSoundEnabled) {
                    SoundUtils.playSound(this, R.raw.yawn_alert_sound)
                }
            } else {
                binding.yawnAlert.visibility = View.GONE
            }

            // Ensure monitoring message remains visible
            binding.monitoringText.visibility = View.VISIBLE
        }
    }


    private fun stopCamera() {
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
        Log.d(TAG, "Camera stopped and resources released")
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    stopCamera()
                    finish()
                    true
                }
                R.id.nav_settings -> {
                    stopCamera()
                    finish()
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCamera()
    }


    fun getCameraProvider(): ProcessCameraProvider? {
        return cameraProvider
    }

    fun setDetector(detector: Detector) {
        this.detector = detector
    }


    companion object {
        private const val TAG = "DriverStateDetector"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private const val REQUEST_CODE_CALL = 20
        private const val REQUEST_CODE_SMS = 30
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
}
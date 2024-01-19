package com.i69.ui.screens.main.camera

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.i69.R
import com.i69.databinding.ActivityCameraBinding
import com.i69.utils.setViewVisible
import com.i69.utils.setVisibleOrInvisible
import com.i69.utils.snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class CameraActivity : AppCompatActivity() {

    private lateinit var camera: Camera
    private lateinit var cameraProvider: ProcessCameraProvider
    private var preview: Preview? = null
    private var cameraSelector: CameraSelector? = null
    private lateinit var imageCapture: ImageCapture
    private lateinit var videoCapture: VideoCapture<Recorder>
    private lateinit var recording: Recording
    private lateinit var outputDirectory: File

    private var isVideoRecording = false

    private lateinit var binding: ActivityCameraBinding
    private lateinit var cameraVM: CameraVM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        supportActionBar?.hide()
        cameraVM  = ViewModelProvider(this)[CameraVM::class.java]
        setContentView(binding.root)

        outputDirectory = getOutputDirectory()

        setUpRecyclerView()
        startCameraIfPermissionsGranted()
        clickListeners()
        lifecycleScope.launchWhenStarted { attachObservers() }
    }

    private fun attachObservers() {
        cameraVM.onPreviewSelectionChanged.observe(this) {
            val preview = it.second
            preview.isSelected = !preview.isSelected
        }
    }

    private fun setUpRecyclerView(){
        binding.rvPreview.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        binding.rvPreview.setHasFixedSize(true)
//        binding.rvPreview.adapter = cameraVM.getPreviewAdapter(this)
    }

    private fun clickListeners() {
        binding.ibCapture.setOnClickListener { takePhoto() }

        binding.ivSwitchCamera.setOnClickListener { initImageCamera() }

        binding.ivGallery.setOnClickListener {
            galleryImageLauncher.launch(
                Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.INTERNAL_CONTENT_URI
                )
            )
        }

        binding.ivRecord.setOnClickListener {
            if (isVideoRecording) {
                recording.stop()
            }
            else {
                initVideoCamera()
            }
        }

        binding.ibClose.setOnClickListener { finish() }
    }

    private val galleryImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
            val data = activityResult.data
            if (activityResult.resultCode == Activity.RESULT_OK) {

                val result = data?.data?.path
                val openInputStream = contentResolver?.openInputStream(data?.data!!)
                val type = if (result?.contains("video") == true) ".mp4" else ".jpg"
                val outputFile = filesDir.resolve("${System.currentTimeMillis()}$type")
                openInputStream?.copyTo(outputFile.outputStream())

                val intent = Intent()
                intent.putExtra("result", outputFile.path)
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
        }

    private fun startCameraIfPermissionsGranted() {
        if (allPermissionsGranted()) {
            initImageCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    private fun initVideoCamera() {
        val recorder = Recorder.Builder()
            .build()

        videoCapture = VideoCapture.withOutput(recorder)

        try {
            camera = cameraProvider.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_BACK_CAMERA,
                videoCapture,
                preview
            )

            val videoFile = File(outputDirectory, "${System.currentTimeMillis()}.mp4")
            val outputOptions = FileOutputOptions.Builder(videoFile).build()

            recording = videoCapture.output
                .prepareRecording(this, outputOptions)
                .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(this), recordingListener)
        } catch (exc: Exception) {
            // we are on the main thread, let's reset the controls on the UI.
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private val recordingListener = Consumer<VideoRecordEvent> { event ->
        when(event) {
            is VideoRecordEvent.Start -> {
                isVideoRecording = true
                binding.ivRecord.setImageResource(R.drawable.stop)
                binding.ibCapture.setVisibleOrInvisible(false)
                // update app internal recording state
            }
            is VideoRecordEvent.Finalize -> {
                isVideoRecording = false
                binding.ivRecord.setImageResource(R.drawable.record)
                binding.ibCapture.setViewVisible()
                if (!event.hasError()) {
                    // update app internal state

                    val path = event.outputResults.outputUri.path
                    Log.d("CAX", "$path ")
                    val intent = Intent()
                    intent.putExtra("result", path)
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                } else {
                    // update app state when the capture failed.
                    recording.close()
                    binding.root.snackbar("Failed with error: ${event.error}")
                }
            }
        }
    }

    private fun initImageCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({ cameraProvider = cameraProviderFuture.get()

            preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            cameraSelector =
                if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA)
                    CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()

                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector!!, preview, imageCapture
                )

            } catch (e: Exception) {
                Log.e(TAG, "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val photoFile = File(outputDirectory, "${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val intent = Intent()
                    intent.putExtra("result", photoFile.path)
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                }

                override fun onError(exception: ImageCaptureException) {
                    binding.root.snackbar("Error capturing photo: ${exception.message}")
                }
            })
    }

    private fun allPermissionsGranted(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(
                baseContext, it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        startCameraIfPermissionsGranted()
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


    companion object {
        private const val TAG = "CameraXApp"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    }
}
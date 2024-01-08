package com.i69.ui.screens.main.camera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.i69.R
import com.i69.databinding.ActivityCameraBinding
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

@AndroidEntryPoint
class CameraActivity : AppCompatActivity() {

    private lateinit var imageCapture: ImageCapture
    private lateinit var outputDirectory: File

    private lateinit var binding: ActivityCameraBinding
    private lateinit var cameraVM: CameraVM

    private var exitId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        cameraVM  = ViewModelProvider(this)[CameraVM::class.java]
        setContentView(binding.root)

        exitId = intent.getStringExtra("exitId").toString()

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

//            updateImageSelection(preview.isSelected, preview.imageUri, it.first)

//            cameraVM.getPreviewAdapter(this).refreshItem(position = it.first, preview)
        }

//        cameraVM.imagesSelected.observe(this) {
//            if (it.isNotEmpty()) {
//                binding.tvImageCount.text = it.size.toString()
//                binding.fabDone.visibility = View.VISIBLE
//                binding.tvImageCount.visibility = View.VISIBLE
//            }
//            else {
//                binding.fabDone.visibility = View.INVISIBLE
//                binding.tvImageCount.visibility = View.INVISIBLE
//            }
//        }

        cameraVM.onImagePreview.observe(this) {
//            showCapturePreview(it.imageUri, false)
        }

//        cameraVM.uploadEntregaArticleImg.observe(this) {
//            if (cameraVM.isUploadPending) {
//                uploadImagesOneByOne()
//            }
//            else {
//                binding.pbLoading.visibility = View.GONE
//                binding.fabDone.visibility = View.VISIBLE
//                binding.tvImageCount.visibility = View.VISIBLE
//                if (it.requestStatus == "SUCCESS") {
//                    showUpdateSuccessDialog()
//                } else
//                    ShowError(it.requestStatus.toString()).show(supportFragmentManager, "Error")
//            }
//
//            if (it.requestStatus != "SUCCESS")
//                ShowError(it.requestStatus.toString()).show(supportFragmentManager, "Error")
//        }

//        cameraVM.uploadEntregaArticleImg.observe(this) {
//            binding.pbLoading.visibility = View.GONE
//            binding.fabDone.visibility = View.VISIBLE
//            binding.tvImageCount.visibility = View.VISIBLE
//            if (it.requestStatus == "SUCCESS") {
//                showUpdateSuccessDialog()
//            } else
//                ShowError(it.requestStatus.toString()).show(supportFragmentManager, "Error")
//        }
    }

//    private fun showUpdateSuccessDialog() {
//        val dialog = showAlert("Updated Successfully")
//
//        Handler(Looper.getMainLooper()).postDelayed({
//            if (dialog?.isShowing == true)
//                dialog.dismiss()
//            finish()
//        }, 1500)
//    }

//    private fun updateImageSelection(isSelected: Boolean, imageUri: Uri?, position: Int) {
//        val imageList = cameraVM.imagesSelected.value ?: mutableListOf()
//
//        val uploadList = cameraVM.imagesToUpload.value ?: mutableListOf()
//        val uploadFile = getIdImage(File(imageUri?.path.toString()))
//
//        if (isSelected) {
//            imageList.add(imageUri)
//            uploadList.add(position, uploadFile)
//        }
//        else {
//            imageList.remove(imageUri)
//            if (position < uploadList.size) uploadList.removeAt(position)
//        }
//
//        cameraVM.imagesToUpload.postValue(uploadList)
//        cameraVM.imagesSelected.postValue(imageList)
//
//        Log.d("TAG", "updateImageSelection: ${cameraVM.imagesToUpload.value}")
//        Log.d("TAG", "updateImageSelection: ${cameraVM.imagesSelected.value}")
//    }

    private fun setUpRecyclerView(){
        binding.rvPreview.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        binding.rvPreview.setHasFixedSize(true)
//        binding.rvPreview.adapter = cameraVM.getPreviewAdapter(this)
    }

    private fun clickListeners() {
        binding.ibCapture.setOnClickListener { takePhoto() }

        binding.ibClose.setOnClickListener { finish() }

        binding.fabDone.setOnClickListener {
//            if (cameraVM.imagesToUpload.value != null) {
//                binding.pbLoading.visibility = View.VISIBLE
//                binding.fabDone.visibility = View.INVISIBLE
//                binding.tvImageCount.visibility = View.INVISIBLE
//            }
        }
    }

    private fun startCameraIfPermissionsGranted() {
        if (allPermissionsGranted()) {
            startCamera()
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

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector =
                CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

            try {
                cameraProvider.unbindAll()

                val camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
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
//                    showCapturePreview(outputFileResults.savedUri, true)

                    val msg = "Photo captured successfully: ${photoFile.absolutePath}"
                    Toast.makeText(this@CameraActivity, msg, Toast.LENGTH_SHORT).show()
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(this@CameraActivity, "Error capturing photo", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun getIdImage(imageFile: File): MultipartBody.Part {
        return run {

            var fos: FileOutputStream? = null
            val bitmapImage = BitmapFactory.decodeFile(imageFile.path)
            try {
                fos = FileOutputStream(imageFile)
                // Use the compress method on the BitMap object to write image to the OutputStream
                bitmapImage.compress(Bitmap.CompressFormat.JPEG, 10, fos)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    fos?.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            MultipartBody.Part.createFormData(
                "files",
                imageFile.name,
                imageFile.asRequestBody("image/*".toMediaTypeOrNull()))
        }
    }

//    private fun showCapturePreview(savedUri: Uri?, isControlsActive: Boolean) {
//        val capturePreviewDialogFragment = CapturePreviewDialogFragment(savedUri, isControlsActive) {
//            if (it) {
//                cameraVM.getPreviewAdapter(this).addItem(Preview(savedUri, true))
//
//                val imageList = cameraVM.imagesSelected.value ?: mutableListOf()
//                imageList.add(savedUri)
//                cameraVM.imagesSelected.postValue(imageList)
//
//                val uploadFile = getIdImage(File(savedUri?.path.toString()))
//                val uploadList = cameraVM.imagesToUpload.value ?: mutableListOf()
//                uploadList.add(uploadFile)
//                cameraVM.imagesToUpload.postValue(uploadList)
//            }
//        }
//        capturePreviewDialogFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialogStyle)
//        capturePreviewDialogFragment.show(supportFragmentManager, CapturePreviewDialogFragment::class.java.simpleName)
//    }

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
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
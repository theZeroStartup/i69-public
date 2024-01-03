package com.i69.ui.screens.main.moment

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.i69.BuildConfig
import com.i69.R
import com.i69.applocalization.AppStringConstant
import com.i69.applocalization.AppStringConstant1
import com.i69.applocalization.AppStringConstantViewModel
import com.i69.data.models.User
import com.i69.databinding.BottomsheetShareOptionsBinding
import com.i69.databinding.DialogPreviewImageBinding
import com.i69.databinding.FragmentNewUserMomentBinding
import com.i69.ui.base.BaseFragment
import com.i69.ui.screens.ImagePickerActivity
import com.i69.ui.screens.main.MainActivity
import com.i69.ui.viewModels.UserViewModel
import com.i69.utils.*
import com.i69.utils.KeyboardUtils.SoftKeyboardToggleListener
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone


class NewUserMomentFragment : BaseFragment<FragmentNewUserMomentBinding>() {

    private val viewModel: UserViewModel by activityViewModels()
    private var mFilePath: String = ""
    lateinit var file: File
    lateinit var fileType: String
    protected var mUser: User? = null
    final var DRAWABLE_LEFT = 0
    final var DRAWABLE_TOP = 1
    final var DRAWABLE_RIGHT = 2
    final var DRAWABLE_BOTTOM = 3
    private val viewStringConstModel: AppStringConstantViewModel by activityViewModels()

    private val photosLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
            val data = activityResult.data
            if (activityResult.resultCode == Activity.RESULT_OK) {
                mFilePath = data?.getStringExtra("result").toString()
                file = File(mFilePath)
                fileType = ".jpg"
                Timber.d("fileBase64 $mFilePath")

                showFilePreview(file, fileType)
                binding.imgUploadFile.loadCircleImage(mFilePath)
            }
        }

    val galleryImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
            val data = activityResult.data
            if (activityResult.resultCode == Activity.RESULT_OK) {

                mFilePath = data?.data.toString()
                val result = data?.data?.path
                val openInputStream =
                    requireActivity().contentResolver?.openInputStream(data?.data!!)
                val type = if (result?.contains("video") == true) ".mp4" else ".jpg"
                fileType = type
                val outputFile =
                    requireContext().filesDir.resolve("${System.currentTimeMillis()}$type")
                openInputStream?.copyTo(outputFile.outputStream())
                file = File(outputFile.toURI())

                showFilePreview(file, fileType)
                binding.imgUploadFile.loadCircleImage(mFilePath)
            }
        }


    override fun getFragmentBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentNewUserMomentBinding.inflate(inflater, container, false).apply {

        stringConstant = AppStringConstant(requireContext())
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun setupTheme() {
        viewStringConstModel.data.observe(this@NewUserMomentFragment) { data ->

            binding.stringConstant = data

        }
        viewStringConstModel.data?.also {
            binding.stringConstant = it.value
//            Log.e("MydataBasesss", it.value!!.messages)
        }

//        binding.editWhatsGoing.setOnClickListener {
//            if(binding.editWhatsGoing.hasFocus()){
//                closeKeyboard()
//            }else{
//                openKeyboard()
//            }
//        }
//        binding.editWhatsGoing.setOnTouchListener { view_, event ->
//
//
//
////            if(event.getRawX() <= (binding.editWhatsGoing.getCompoundDrawables()[DRAWABLE_LEFT].getBounds().width()))
////            {
//
//            if(event.getRawY() <= (binding.editWhatsGoing.getCompoundDrawables()[DRAWABLE_BOTTOM].getBounds().width()))
//            {
//                // your action here
//
//                if(binding.editWhatsGoing.hasFocus()){
//                    closeKeyboard()
//                }else{
//                    openKeyboard()
//                }
//
//                return@setOnTouchListener true;
//            }
//            return@setOnTouchListener false;
//
//        }

        binding.btnSelectFileToUpload.setOnClickListener {
            hideKeyboard(it)
            val inflater =
                requireContext().applicationContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val view = inflater.inflate(R.layout.layout_attach_moment, null)
            view.findViewById<TextView>(R.id.header_title).text =
                AppStringConstant1.select_moment_pic
            val mypopupWindow = PopupWindow(
                view,
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                true
            )

            val llCamera = view.findViewById<View>(R.id.llCamera) as LinearLayout
            val llGallary = view.findViewById<View>(R.id.ll_gallery) as LinearLayout

            llCamera.setOnClickListener {
                val intent = Intent(requireActivity(), ImagePickerActivity::class.java)
                intent.putExtra("withCrop", false)
                photosLauncher.launch(intent)
                mypopupWindow.dismiss()
            }

            llGallary.setOnClickListener {
                galleryImageLauncher.launch(
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
                )
                mypopupWindow.dismiss()
            }
            mypopupWindow.showAsDropDown(it, (-it.x).toInt(), 0)
        }

        binding.imgUploadFile.setOnClickListener { if (this::file.isInitialized) showFilePreview(file, fileType) }

        /*binding.editWhatsGoing.setOnTouchListener(OnTouchListener { _, event ->

            val DRAWABLE_RIGHT = 2

            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= binding.editWhatsGoing.getRight() - binding.editWhatsGoing.getCompoundDrawables()
                        .get(DRAWABLE_RIGHT).getBounds().width()
                ) {
                    binding.editWhatsGoing.clearFocus()
                    hideKeyboard(binding.root);
//                    Toast.makeText(requireContext(), "Hello", Toast.LENGTH_SHORT).show()
                    // your action here
                    return@OnTouchListener true
                }
            }
            false
        })*/

        binding.cdAllowedComment.setOnClickListener {
            binding.rbAllowed.isChecked = true
            binding.rbNotAllowed.isChecked = false
        }

        binding.cdNotAllowedComment.setOnClickListener {
            binding.rbAllowed.isChecked = false
            binding.rbNotAllowed.isChecked = true
        }

        binding.rbAllowed.setOnClickListener { binding.cdAllowedComment.performClick() }

        binding.rbNotAllowed.setOnClickListener { binding.cdNotAllowedComment.performClick() }

//        if(binding.checkPermission.isChecked){
//            binding.checkPermission.isChecked = false
//            binding.allowDesciption.text = resources.getString(R.string.comment_not_allowed)
//        }else{
//            binding.checkPermission.isChecked = true
//            binding.allowDesciption.text = resources.getString(R.string.comment_allowed)
//        }

        binding.editWhatsGoing.setOnTouchListener(object :
            DrawableClickListener.BottomDrawableClickListener(binding.editWhatsGoing) {
            override fun onDrawableClick(): Boolean {
                if (binding.editWhatsGoing.hasFocus()) {
//                    closeKeyboard()
                    binding.editWhatsGoing.clearFocus()
                    hideKeyboard(binding.root);
                } else {
//                    openKeyboard(binding.root)l

                    binding.editWhatsGoing.requestFocus()
                    binding.editWhatsGoing.showKeyboard()
                }
//                binding.editWhatsGoing.clearFocus()
//                hideKeyboard(binding.root);
                return true
            }
        })

        KeyboardUtils.addKeyboardToggleListener(requireActivity(),
            SoftKeyboardToggleListener { isVisible: Boolean ->
                binding.lblPostingMomentTip.setVisibility(
                    if (isVisible) GONE else VISIBLE
                )
            })

        binding.btnShareMoment.setOnClickListener {
            if (mFilePath == null || !this::file.isInitialized) {
                binding.root.snackbar(AppStringConstant1.you_cant_share_moment)
                return@setOnClickListener
            }

            showShareOptions {}
        }

        showProgressView()
        lifecycleScope.launch {
            val userId = getCurrentUserId()!!
            val token = getCurrentUserToken()!!
            viewModel.getCurrentUser(userId, token = token, false)
                .observe(viewLifecycleOwner) { user ->
                    user?.let {
                        if (it != null) {
                            mUser = it.copy()
                            Timber.d("Userrname ${mUser?.username}")

                            if (mUser != null) {

                                if (mUser!!.avatarPhotos != null && mUser!!.avatarPhotos!!.size != 0) {

                                    if (mUser!!.avatarPhotos!!.size != 0 && mUser?.avatarPhotos?.size!! > mUser?.avatarIndex!!) {
                                        binding.imgCurrentUser.loadCircleImage(
                                            mUser!!.avatarPhotos!!.get(
                                                mUser!!.avatarIndex!!
                                            ).url?.replace(
                                                "http://95.216.208.1:8000/media/",
                                                "${BuildConfig.BASE_URL}media/"
                                            ).toString()
                                        )

                                    }

                                }
                            }
                        }

                    }
                    hideProgressView()
                }
        }
    }

    fun Fragment.hideKeyboard() {
        view?.let { activity?.hideKeyboard(it) }
    }

    fun Activity.hideKeyboard() {
        hideKeyboard(currentFocus ?: View(this))
    }
    fun Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun showFilePreview(file: File?, fileType: String) {
        val dialogBinding = DialogPreviewImageBinding.inflate(layoutInflater, null, false)

        val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawableResource(R.color.color_transparant_80)


        if (fileType == ".jpg") {
            dialogBinding.ivPreview.setViewVisible()
            dialogBinding.vvPreview.setViewGone()
            Glide.with(requireContext())
                .load(file)
                .into(dialogBinding.ivPreview)
        }
        else {
            dialogBinding.ivPreview.setViewGone()
            dialogBinding.vvPreview.setViewVisible()
            dialogBinding.vvPreview.setVideoPath(file?.path)
            dialogBinding.vvPreview.start()
            dialogBinding.vvPreview.setOnCompletionListener {
                if (dialog.isShowing && it != null) dialogBinding.vvPreview.start()
            }
        }

        dialogBinding.ibClose.setViewGone()

        dialogBinding.tvShare.text = getString(R.string.proceed)
        dialogBinding.btnShareMoment.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun showShareOptions(onShared: () -> Unit) {
        val shareOptionsDialog = BottomSheetDialog(requireContext())
        val bottomsheet = BottomsheetShareOptionsBinding.inflate(layoutInflater, null, false)

        var shareAt = ""

        if (isUserHasPremiumSubscription()) {
            bottomsheet.llShareLaterRoot.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.CE4F3FF, null))
            bottomsheet.rbShareLater.setViewVisible()
            bottomsheet.ivLocked.setViewGone()
        }
        else {
            bottomsheet.llShareLaterRoot.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.profileTransBlackOverlayColor, null))
            bottomsheet.rbShareLater.setViewGone()
            bottomsheet.ivLocked.setViewVisible()
        }

        bottomsheet.rbShareNow.setOnClickListener { bottomsheet.cvShareNow.performClick() }
        bottomsheet.rbShareLater.setOnClickListener { bottomsheet.cvShareLater.performClick() }

        bottomsheet.cvShareNow.setOnClickListener {
            bottomsheet.rbShareNow.isChecked = true
            bottomsheet.rbShareLater.isChecked = false
        }

        bottomsheet.cvShareLater.setOnClickListener {
            if (isUserHasPremiumSubscription()) {
                bottomsheet.rbShareLater.isChecked = true
                bottomsheet.rbShareNow.isChecked = false
                showDateTimePicker { displayTime, apiTime ->
                    if (displayTime.isNotEmpty() && apiTime.isNotEmpty()) {
                        bottomsheet.tvShareLater.text = "Scheduled for $displayTime"
                        shareAt = apiTime
                    }
                    else {
                        bottomsheet.rbShareLater.isChecked = false
                        bottomsheet.rbShareNow.isChecked = true
                    }
                }
            }
            else {
            binding.root.snackbar("Feature Locked. Please purchase a package to unlock")
        }
        }

        bottomsheet.btnShareMoment.setOnClickListener {
            if (shareOptionsDialog.isShowing) shareOptionsDialog.dismiss()
            onShared.invoke()
            if (bottomsheet.rbShareNow.isChecked) {
                shareNow()
            }
            else if (bottomsheet.rbShareLater.isChecked) {
                if (shareAt.isNotEmpty()) {
                    shareLater(shareAt)
                }
            }
        }

        shareOptionsDialog.setContentView(bottomsheet.root)
        shareOptionsDialog.show()
    }

    private fun isUserHasPremiumSubscription(): Boolean {
//        return mUser?.userSubscription?.isActive == true
        return true
    }

    private fun showDateTimePicker(onDateAndTimePicked: (String, String) -> Unit) {
        val currentDate: Calendar = Calendar.getInstance()
        val date = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { view, year, monthOfYear, dayOfMonth ->
                date.set(year, monthOfYear, dayOfMonth)
                TimePickerDialog(context,
                    { view, hourOfDay, minute ->
                        if (getMainActivity().isValidTime(year, monthOfYear, dayOfMonth, hourOfDay, minute)) {
                            date.set(Calendar.HOUR_OF_DAY, hourOfDay)
                            date.set(Calendar.MINUTE, minute)

                            val sdf1 = SimpleDateFormat("dd MMM hh:mm a", Locale.getDefault())
                            val sdf2 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
                            sdf2.timeZone = TimeZone.getTimeZone("UTC")
                            val now = date.time
                            val displayTime = sdf1.format(now)
                            val formattedTime = sdf2.format(now)
                            onDateAndTimePicked.invoke(displayTime, formattedTime)

                            Log.v("UMF", "The choosen one $formattedTime")
                        }
                        else {
                            onDateAndTimePicked.invoke("", "")
                            binding.root.snackbarOnTop(getString(R.string.please_select_a_future_time))
                        }                    },
                    currentDate.get(Calendar.HOUR_OF_DAY),
                    currentDate.get(Calendar.MINUTE),
                    false
                ).show()
            },
            currentDate.get(Calendar.YEAR),
            currentDate.get(Calendar.MONTH),
            currentDate.get(Calendar.DATE)
        ).show()
    }

    private fun shareNow() {
        val description = binding.editWhatsGoing.text.toString()

        Timber.d("filee $mFilePath")

        Log.d("NewUserMomentFragment", "calling")

        getMainActivity().openUserAllMoments(file,description,binding.rbAllowed.isChecked)
        binding.editWhatsGoing.setText("")
    }

    private fun shareLater(shareAt: String) {
        val description = binding.editWhatsGoing.text.toString()

        Timber.d("filee $mFilePath")

        Log.d("NewUserMomentFragment", "calling")

        getMainActivity().openUserAllMoments(file,description,binding.rbAllowed.isChecked, shareAt)
        binding.editWhatsGoing.setText("")
    }

    override fun setupClickListeners() {
        binding.toolbarHamburger.setOnClickListener {
            hideKeyboard(binding.root)
            binding.editWhatsGoing.clearFocus()
            getMainActivity().drawerSwitchState()
        }
    }

    fun hideKeyboard(view: View) =
        (requireContext().getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as? InputMethodManager)!!
            .hideSoftInputFromWindow(view.windowToken, 0)


//    private fun closeKeyboard() {
//        val inputManager =
//            requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//        inputManager.hideSoftInputFromWindow(
//            requireActivity().currentFocus!!.windowToken,
//            InputMethodManager.HIDE_NOT_ALWAYS
//        )
//    }

    private fun openKeyboard(view: View) {
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
//        imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0)
    }

    private fun onFailureListener(error: String) {
        hideProgressView()
        Timber.e("${getString(R.string.something_went_wrong)} $error")
        binding.root.snackbar("${getString(R.string.something_went_wrong)} $error")
    }

    override fun onResume() {
        super.onResume()
        getMainActivity().setDrawerItemCheckedUnchecked(null)

    }

    fun getMainActivity() = activity as MainActivity
}

package com.i69.ui.screens.main.moment

import android.annotation.SuppressLint
import android.app.Activity
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
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.i69.BuildConfig
import com.i69.R
import com.i69.applocalization.AppStringConstant
import com.i69.applocalization.AppStringConstant1
import com.i69.applocalization.AppStringConstantViewModel
import com.i69.data.models.User
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


class NewUserMomentFragment : BaseFragment<FragmentNewUserMomentBinding>() {

    private val viewModel: UserViewModel by activityViewModels()
    private var mFilePath: String = ""
    lateinit var file: File
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
                Timber.d("fileBase64 $mFilePath")
//            val uri=Uri.parse(mFilePath)
                //binding.cropView.setUri(uri);
                /* CropLayout.addOnCropListener(object : OnCropListener {
                     override fun onSuccess(bitmap: Bitmap) {
                         // do somethhing with bitmap.
                     }

                     override fun onFailure(e: Exception) {
                         // do error handling.
                     }
                 })*/

                //binding.cropView.isOffFrame() // optionally check if the image is off the frame.

                //binding.cropView.crop()

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
                val outputFile =
                    requireContext().filesDir.resolve("${System.currentTimeMillis()}$type")
                openInputStream?.copyTo(outputFile.outputStream())
                file = File(outputFile.toURI())

                Timber.d("fileBase64 $mFilePath")
//                val uri=Uri.parse(mFilePath)
                //binding.cropView.setUri(uri);
                /* CropLayout.addOnCropListener(object : OnCropListener {
                     override fun onSuccess(bitmap: Bitmap) {
                         // do somethhing with bitmap.
                     }

                     override fun onFailure(e: Exception) {
                         // do error handling.
                     }
                 })*/

                //binding.cropView.isOffFrame() // optionally check if the image is off the frame.

                //binding.cropView.crop()

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
            /*        val intent = Intent(requireActivity(), ImagePickerActivity::class.java)
                    photosLauncher.launch(intent)*/
//            val dialog = Dialog(requireContext())
//            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
//            dialog.setCancelable(true)
//            dialog.setContentView(R.layout.dialog_image_option)
//            dialog.findViewById<TextView>(R.id.header_title).text=
//                AppStringConstant1.select_moment_pic
////                resources.getString(R.string.select_moment_pic)
//            dialog.findViewById<LinearLayoutCompat>(R.id.ll_camera).setOnClickListener {
//                val intent = Intent(requireActivity(), ImagePickerActivity::class.java)
//                intent.putExtra("withCrop", false)
//                photosLauncher.launch(intent)
//                dialog.dismiss()
//            }
//            dialog.findViewById<LinearLayoutCompat>(R.id.ll_gallery).setOnClickListener {
//                galleryImageLauncher.launch(
//                    Intent(
//                        Intent.ACTION_PICK,
//                        MediaStore.Images.Media.INTERNAL_CONTENT_URI
//                    )
//                )
//                dialog.dismiss()
//            }
//            dialog.show()

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
//                binding.root.snackbar(getString(R.string.you_cant_share_moment))
                return@setOnClickListener
            }

            showProgressView()

            val description = binding.editWhatsGoing.text.toString()
            /*val fileBase64 = BitmapFactory.decodeFile(mFilePath).convertBitmapToString()
            lifecycleScope.launch(Dispatchers.Main) {
                val userToken = getCurrentUserToken()!!
                Timber.d("fileBase64 [${fileBase64.length}] $mFilePath")
                when (val response = mViewModel.shareUserMoment("Moment Image", fileBase64, description, token = userToken)) {
                    is Resource.Success -> {
                        hideProgressView()
                    }
                    is Resource.Error -> onFailureListener(response.message ?: "")
                }
            }*/
            Timber.d("filee $mFilePath")

            Log.d("NewUserMomentFragment", "calling")

            getMainActivity().openUserAllMoments(file,description,binding.rbAllowed.isChecked)
            binding.editWhatsGoing.setText("")

//            val bundle = Bundle()
////            bundle.putString("fromUser", "1")
//            bundle.putString("filePath", mFilePath)
//            bundle.putString("description", description)
//            bundle.putBoolean("commentAllowed", binding.rbAllowed.isChecked)
//            bundle.putBoolean("isShare", true)
//            findNavController().navigate(R.id.newActionHome, bundle)

            hideProgressView()
            /*    lifecycleScope.launchWhenCreated {

                     val f = file
                     val buildder = DefaultUpload.Builder()
                     buildder.contentType("Content-Disposition: form-data;")
                     buildder.fileName(f.name)
                     val upload = buildder.content(f).build()
                     Timber.d("filee ${f.exists()}")
                     val userToken = getCurrentUserToken()!!

                     Timber.d("useriddd ${mUser?.id}")
                     if (mUser?.id != null) {
                         val response = try {

                             apolloClient(context = requireContext(), token = userToken).
                             mutation(
                                 MomentMutation(
                                     file = upload,
                                     detail = description,
                                     userField = mUser?.id!!,
                                     allowComment = binding.rbAllowed.isChecked
                                 )
                             ).execute()


                         } catch (e: ApolloException) {
                             hideProgressView()
                             Timber.d("filee Apollo Exception ApolloException ${e.message}")
                             binding.root.snackbar(" ${e.message}")
                             return@launchWhenCreated
                         } catch (e: Exception) {
                             hideProgressView()
                             Timber.d("filee General Exception ${e.message} $userToken")
                             binding.root.snackbar(" ${e.message}")
                             return@launchWhenCreated
                         }
                         Log.e("222","--->"+Gson().toJson(response))
                         hideProgressView()

                         if(response.hasErrors())
                         {
                             Log.d("NewUserMomentFragment","${response.errors}")
                             //binding.root.snackbar("${response.errors?.get(0)?.message}")
                             binding.root.snackbarOnTop("${response.errors?.get(0)?.message}", Snackbar.LENGTH_INDEFINITE,callback = {
                                 //TODO: navigate to package/subscription screen
                                 findNavController().navigate(R.id.action_global_plan)
                             })
                             //  Log.e("ddd1dddww","-->"+response.errors!!.get(0).nonStandardFields!!.get("code"))
                             /*      if(response.errors!!.get(0).nonStandardFields!!.get("code").toString().equals("InvalidOrExpiredToken"))
                                   {
                                       // error("User doesn't exist")

                                     if(activity!=null)
                                       {
                                           App.userPreferences.clear()
                                           //App.userPreferences.saveUserIdToken("","","")
                                           val intent = Intent(MainActivity.mContext, SplashActivity::class.java)
                                           startActivity(intent)
                                           Log.e("ddd1dddwwds","-->"+response.errors!!.get(0).nonStandardFields!!.get("code"))
                                           activity?.finishAffinity()
                                       }

                                   }*/
                         }
                         else {
                             //val bundle = Bundle()
                             //bundle.putString("fromUser", "1")
                             //findNavController().navigate(R.id.newActionHome,bundle)
                             binding.editWhatsGoing.text=null
                             getMainActivity().mViewModelUser.userMomentsList.clear()
                             getMainActivity().openUserMoments()

                         }

                     } else {

     //                    binding.root.snackbar("username is null")
     //                    binding.root.snackbar("Exception ${mUser?.id}")
                     }
                     hideProgressView()
                     //binding.root.snackbar("Exception (${response.hasErrors()}) ${response.data?.insertMoment?.moment?.momentDescription}")
                     //Timber.d("filee response = (${response.hasErrors()}) ${response.data?.insertMoment?.moment?.momentDescription}")
                     //Timber.d("filee response = (${response.hasErrors()}) [${response.errors?.get(0)?.message}] ${response.data?.insertMoment?.moment?.momentDescription}")
                     //filee response = com.apollographql.apollo3.api.ApolloResponse@3f798dc
                 }*/
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

                                    if (mUser!!.avatarPhotos!!.size != 0) {
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

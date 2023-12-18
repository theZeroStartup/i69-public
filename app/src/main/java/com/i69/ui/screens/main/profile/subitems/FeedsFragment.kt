package com.i69.ui.screens.main.profile.subitems

import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.apollographql.apollo3.exception.ApolloException
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.gson.Gson
import com.i69.*
import com.i69.applocalization.AppStringConstantViewModel
import com.i69.databinding.FragmentFeedBinding
import com.i69.di.modules.AppModule
import com.i69.ui.adapters.NearbySharedMomentAdapter
import com.i69.ui.adapters.StoryLikesAdapter
import com.i69.ui.base.BaseFragment
import com.i69.ui.viewModels.CommentsModel
import com.i69.ui.viewModels.UserMomentsModelView
import com.i69.utils.apolloClient
import com.i69.utils.getResponse
import com.i69.utils.snackbar
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.roundToInt

class FeedsFragment : BaseFragment<FragmentFeedBinding>() ,
    NearbySharedMomentAdapter.NearbySharedMomentListener {

    private var userToken: String? = null
    private var userName: String? = null

    private lateinit var sharedMomentAdapter: NearbySharedMomentAdapter
    private lateinit var GiftbottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>
    private lateinit var receivedGiftbottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>
    private lateinit var LikebottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>
    var momentLikesUsers: java.util.ArrayList<CommentsModel> = java.util.ArrayList()
    var likeMomentItemPK: String? = null
    var momentLikeUserAdapters :
            StoryLikesAdapter? = null


    var width = 0
    var size = 0
    var endCursor: String=""
    var giftUserid: String? = null
    var hasNextPage: Boolean= false
    var allUserMoments: ArrayList<GetAllUserMomentsQuery.Edge> = ArrayList()
    var layoutManager: LinearLayoutManager? = null

    private var userId: String? = null
    private val mViewModel: UserMomentsModelView by activityViewModels()
    private val viewStringConstModel: AppStringConstantViewModel by activityViewModels()

    override fun getFragmentBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentFeedBinding.inflate(inflater, container, false)

    override fun setupTheme() {
        navController = findNavController()
        viewStringConstModel.data.observe(this@FeedsFragment) { data ->

            binding.stringConstant = data

        }
        viewStringConstModel.data?.also {
            binding.stringConstant = it.value
//            Log.e("MydataBasesss", it.value!!.messages)
        }
        val displayMetrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
        val height = displayMetrics.heightPixels
        width = displayMetrics.widthPixels

        val densityMultiplier =getResources().getDisplayMetrics().density;
        val scaledPx = 14 * densityMultiplier;
        val paint = Paint()
        paint.setTextSize(scaledPx);
        size = paint.measureText("s").roundToInt()

        setUpData()
    }

    override fun setupClickListeners() {

//        GiftbottomSheetBehavior = BottomSheetBehavior.from(binding.giftbottomSheet)
//        GiftbottomSheetBehavior.setBottomSheetCallback(object :
//            BottomSheetBehavior.BottomSheetCallback() {
//            override fun onSlide(bottomSheet: View, slideOffset: Float) {
//
//            }
//
//            override fun onStateChanged(bottomSheet: View, newState: Int) {
//                when (newState) {
//                    BottomSheetBehavior.STATE_COLLAPSED -> {
//                        Timber.d("Slide Up")
//                    }
//                    BottomSheetBehavior.STATE_HIDDEN -> {
//
//                    }
//                    BottomSheetBehavior.STATE_EXPANDED -> {
//                        Timber.d("Slide Down")
//                    }
//                    BottomSheetBehavior.STATE_DRAGGING -> {
//
//                    }
//                    BottomSheetBehavior.STATE_SETTLING -> {
//
//                    }
//                }
//            }
//        })


//        childFragmentManager.beginTransaction().replace(R.id.receivedGiftContainer, FragmentReceivedGifts()).commitAllowingStateLoss()
//
//        receivedGiftbottomSheetBehavior = BottomSheetBehavior.from(binding.receivedGiftBottomSheet)
//        receivedGiftbottomSheetBehavior.addBottomSheetCallback(object :
//            BottomSheetBehavior.BottomSheetCallback() {
//            override fun onSlide(bottomSheet: View, slideOffset: Float) {
//
//            }
//
//            override fun onStateChanged(bottomSheet: View, newState: Int) {
//                when (newState) {
//                    BottomSheetBehavior.STATE_COLLAPSED -> {
//                        Timber.d("Slide Up")
//                        receivedGiftbottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
//                    }
//                    BottomSheetBehavior.STATE_HIDDEN -> {
//
//                    }
//                    BottomSheetBehavior.STATE_EXPANDED -> {
//                        Timber.d("Slide Down")
//                    }
//                    BottomSheetBehavior.STATE_DRAGGING -> {
//
//                    }
//                    BottomSheetBehavior.STATE_SETTLING -> {
//
//                    }
//                }
//            }
//        })

    }

    private fun setUpData() {

        lifecycleScope.launch {
            //handling in case current user Id is null
            if (getCurrentUserId() != null) {
                userId = getCurrentUserId()!!
            }
            getCurrentUserToken()?.let {
                userToken = it
            }
            userName= getCurrentUserName()
        }

        allUserMoments = ArrayList()
        sharedMomentAdapter = NearbySharedMomentAdapter(
            requireActivity(),
            this,
            allUserMoments,
            userId,
            false
        )
        layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        binding.rvMoments.setLayoutManager(layoutManager)



        getAllUserMoments(width,size)

        binding.scrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            if (hasNextPage) {

                binding.rvMoments?.let {


                    if (it.bottom - (binding.scrollView.height + binding.scrollView.scrollY) == 0)
                        allusermoments1(width,size,10,endCursor)
                }

            }
        })

    }


    private fun getAllUserMoments(width: Int, size: Int) {
        lifecycleScope.launch() {
            val res = try {

                Log.e("getAllUserMoments", "${userToken}")
                apolloClient(requireContext(), userToken!!).query(GetAllUserMomentsQuery(
                    width,size,10,"",""
                )).execute()
            } catch (e: ApolloException) {
                Log.e("getAllUserMoments", "${e.message}")
                Timber.d("apolloResponse ${e.message}")

                return@launch
            }

            val allmoments = res.data?.allUserMoments!!.edges
            Log.e("getAllUserMoments", "${allmoments.size}")
            if(allmoments.size!=0)
            {
                endCursor = res.data?.allUserMoments!!.pageInfo.endCursor!!
                hasNextPage = res.data?.allUserMoments!!.pageInfo.hasNextPage!!


                val allUserMomentsFirst: ArrayList<GetAllUserMomentsQuery.Edge> = ArrayList()

                allmoments.indices.forEach { i ->


                    allUserMomentsFirst.add(allmoments[i]!!)
                }

               // sharedMomentAdapter.addAll(allUserMomentsFirst)

                binding.rvMoments.adapter = sharedMomentAdapter
                allUserMoments.addAll(allUserMomentsFirst)
                sharedMomentAdapter.submitList1(allUserMoments)
            }

            if (binding.rvMoments.itemDecorationCount == 0) {
                binding.rvMoments.addItemDecoration(object : RecyclerView.ItemDecoration() {
                    override fun getItemOffsets(
                        outRect: Rect,
                        view: View,
                        parent: RecyclerView,
                        state: RecyclerView.State
                    ) {
                        outRect.top = 10
                    }
                })
            }
        }
    }

    private fun getParticularMoments(pos: Int, ids: String) {


        lifecycleScope.launchWhenResumed {
            val res = try {
                apolloClient(requireContext(), userToken!!).query(GetAllUserMomentsQuery(width,size,1,"",ids))
                    .execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse Exception all moments${e.message}")
                binding.root.snackbar(" ${e.message}")
                return@launchWhenResumed
            }


            val allmoments = res.data?.allUserMoments!!.edges


            allmoments.indices.forEach { i ->
                if(ids.equals(allmoments[i]!!.node!!.pk.toString()))
                {
                    allUserMoments[pos] = allmoments[i]!!
                    sharedMomentAdapter.submitList1(allUserMoments)
                    sharedMomentAdapter.notifyItemChanged(pos)
                    return@forEach
                }
            }
        }
    }

    fun fireLikeNotificationforreceiver(item: GetAllUserMomentsQuery.Edge) {


        lifecycleScope.launchWhenResumed {


            val queryName = "sendNotification"
            val query = StringBuilder()
                .append("mutation {")
                .append("$queryName (")
                .append("userId: \"${item!!.node!!.user!!.id}\", ")
                .append("notificationSetting: \"LIKE\", ")
                .append("data: {momentId:${item!!.node!!.pk}}")
                .append(") {")
                .append("sent")
                .append("}")
                .append("}")
                .toString()

            val result= AppModule.provideGraphqlApi().getResponse<Boolean>(
                query,
                queryName, userToken)
            Timber.d("RSLT",""+result.message)

        }

    }

    fun allusermoments1(width: Int, size: Int, i: Int, endCursors: String) {
        lifecycleScope.launchWhenResumed {
            val res = try {
                apolloClient(requireContext(), userToken!!).query(GetAllUserMomentsQuery(width,size,10,endCursors,""))
                    .execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse Exception all moments ${e.message}")
                Log.e("allusermoments1Feed", "${e.message}")


                binding.root.snackbar(" ${e.message}")
                return@launchWhenResumed
            }

            val allusermoments = res.data?.allUserMoments!!.edges


            endCursor = res.data?.allUserMoments!!.pageInfo.endCursor!!
            hasNextPage = res.data?.allUserMoments!!.pageInfo.hasNextPage

            if(allusermoments.size!=0)
            {
                val allUserMomentsNext: ArrayList<GetAllUserMomentsQuery.Edge> = ArrayList()


                allusermoments.indices.forEach { i ->


                    allUserMomentsNext.add(allusermoments[i]!!)
                }
                allUserMoments.addAll(allUserMomentsNext)
                sharedMomentAdapter.submitList1(allUserMoments)

               // sharedMomentAdapter.addAll(allUserMomentsNext)

            }


            if (binding.rvMoments.itemDecorationCount == 0) {
                binding.rvMoments.addItemDecoration(object : RecyclerView.ItemDecoration() {
                    override fun getItemOffsets(
                        outRect: Rect,
                        view: View,
                        parent: RecyclerView,
                        state: RecyclerView.State
                    ) {
                        outRect.top = 10
                    }
                })
            }
            if (allusermoments?.size!! > 0) {
                Timber.d("apolloResponse: ${allusermoments?.get(0)?.node!!.file}")
                Timber.d("apolloResponse: ${allusermoments?.get(0)?.node!!.id}")
                Timber.d("apolloResponse: ${allusermoments?.get(0)?.node!!.createdDate}")
                Timber.d("apolloResponse: ${allusermoments?.get(0)?.node!!.momentDescriptionPaginated}")
                Timber.d("apolloResponse: ${allusermoments?.get(0)?.node!!.user?.fullName}")
            }
        }
    }

    override fun onCommentofMomentClick(
        position: Int,
        item: GetAllUserMomentsQuery.Edge?
    ) {
        val bundle = Bundle()
        bundle.putString("momentID", item?.node!!.pk!!.toString())

        bundle.putString("filesUrl", item?.node!!.file!!)
        bundle.putString("Likes", item?.node!!.like!!.toString())
        bundle.putString("Comments", item?.node!!.comment!!.toString())
        val gson = Gson()
        bundle.putString("Desc",gson.toJson(item.node!!.momentDescriptionPaginated))
        if(item.node.user!!.gender != null)
        {
            bundle.putString("gender", item.node.user.gender!!.name)

        }
        else
        {
            bundle.putString("gender", null)

        }
        bundle.putString("fullnames",item.node!!.user!!.fullName)
        bundle.putString("momentuserID", item.node!!.user!!.id.toString())



        navController.navigate(R.id.momentsAddCommentFragment,bundle)
    }

    override fun onMomentGiftClick(position: Int, item: GetAllUserMomentsQuery.Edge?) {
//        var bundle = Bundle()
//        bundle.putString("userId", userId)
//        navController.navigate(R.id.action_userProfileFragment_to_userGiftsFragment,bundle)

//        if (userId!! != item!!.node!!.user!!.id) {
//            giftUserid = item.node!!.user!!.id.toString()
//            binding.sendgiftto.text =
//                context?.resources?.getString(R.string.send_git_to) + " " + item.node.user!!.fullName
//            if (GiftbottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
//                GiftbottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
////            buttonSlideUp.text = "Slide Down";
//            } else {
//                GiftbottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
////            buttonSlideUp.text = "Slide Up"
//            }
//        } else {
//            //TODO: show receive gifts
//            /*Toast.makeText(
//                requireContext(),
//                getString(R.string.user_cant_bought_gift_yourseld),
//                Toast.LENGTH_LONG
//            ).show()*/
//            if (receivedGiftbottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
//                receivedGiftbottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
//            } else {
//                receivedGiftbottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
//            }
//        }
    }


    override fun onDotMenuofMomentClick(
        position: Int,
        item: GetAllUserMomentsQuery.Edge?,
        types: String
    ) {

        if(types.equals("delete"))
        {

            showProgressView()
            lifecycleScope.launchWhenResumed {
                val res = try {
                    apolloClient(
                        requireContext(),
                        userToken!!
                    ).mutation(DeletemomentMutation(item?.node!!.pk!!.toString()))
                        .execute()
                } catch (e: ApolloException) {
                    Timber.d("apolloResponseException ${e.message}")
                    binding.root.snackbar(" ${e.message}")
                    hideProgressView()
                    return@launchWhenResumed
                }

                hideProgressView()

                val positionss = allUserMoments.indexOf(item)
                allUserMoments.remove(item)
                sharedMomentAdapter.notifyItemRemoved(position)
            }
        }
        else if(types.equals("report"))
        {
            showProgressView()
            lifecycleScope.launchWhenResumed {
                val res = try {
                    apolloClient(
                        requireContext(),
                        userToken!!
                    ).mutation(ReportonmomentMutation(item?.node!!.pk!!.toString(), "This is not valid post"))
                        .execute()
                } catch (e: ApolloException) {
                    Timber.d("apolloResponse Exception ${e.message}")
                    binding.root.snackbar(" ${e.message}")
                    hideProgressView()
                    return@launchWhenResumed
                }

                hideProgressView()
            }
        }

    }


    override fun onMoreShareMomentClick() {

    }
    override fun onSharedMomentClick(position: Int, item: GetAllUserMomentsQuery.Edge?) {
    }



    override fun onLikeofMomentClick(position: Int, item: GetAllUserMomentsQuery.Edge?) {

        showProgressView()
        lifecycleScope.launchWhenResumed {
            userId = getCurrentUserId()!!
            userName = getCurrentUserName()!!
            Log.i("FeedsFragment", "onLikeofMomentClick: UserId: $userId   Username: $userName")
//            val selectedUserId = item?.node?.user?.id
//            if (selectedUserId == userId) {
//                getMomentLikes(item)
//            }else {
                val res = try {
                    apolloClient(
                        requireContext(),
                        userToken!!
                    ).mutation(LikeOnMomentMutation(item?.node!!.pk!!.toString()))
                        .execute()
                } catch (e: ApolloException) {
                    Timber.d("apolloResponse Exception ${e.message}")
                    binding.root.snackbar(" ${e.message}")
                    hideProgressView()
                    return@launchWhenResumed
                }

                hideProgressView()

//            if (res.hasErrors()) {
//                //binding.txtNearbyUserLikeCount.setText("0")
//            } else if (res.data != null) {
////                            if (res.data!!.likeMoment!!.like!!.momemt != null) {
//
//                sharedMomentAdapter.updateUI(res.data?.likeMoment!!)
//            }

                fireLikeNotificationforreceiver(item)

                getParticularMoments(position, item.node.pk.toString())
//            }


        }

    }

    override fun onLikeofMomentshowClick(position: Int, item: GetAllUserMomentsQuery.Edge?) {

//        getMomentLikes(item)
    }


    private fun getMomentLikes(item: GetAllUserMomentsQuery.Edge?) {
        likeMomentItemPK = (item?.node?.pk ?: "").toString()

        lifecycleScope.launchWhenResumed {
            userToken?.let {
                Log.d("UserMomentsFragment", "UserMomentNextPage Calling")
                mViewModel.getMomentLikes(it, ((item?.node?.pk ?: "").toString())) { error ->
                    if (error == null) {
                        activity?.runOnUiThread({
//                            loadLikesDialog(mViewModel.coinPrice)

                        })

                    } else {

                    }
                }
            }
        }

//        lifecycleScope.launch(Dispatchers.Main) {
//        lifecycleScope.launchWhenResumed{
//            val token = getCurrentUserToken()!!
//            mViewModel.getMomentLikes(token, (item?.node?.pk ?: "").toString())
//                .observe(viewLifecycleOwner) {
//                    it?.let { momentLikes ->
////                        loadLikesDialog(momentLikes)
//                    }
//                }
//        }
    }

//    private fun loadLikesDialog(momentLikes: java.util.ArrayList<MomentLikes>) {
////        lifecycleScope.launch(Dispatchers.Main) {
//        lifecycleScope.launchWhenResumed{
//            momentLikesUsers.clear()
////        momentLikesUsers.addAll(momentLikes)
//            showLikeBottomSheet()
//            val rvLikes = binding.rvLikes
//            val no_data = binding.noDatas
//            val txtHeaderLike = binding.txtheaderlike
////
//            txtHeaderLike.text = momentLikes.size.toString() + " Likes"
//            if (momentLikes.isNotEmpty()) {
//                no_data.visibility = View.GONE
//                rvLikes.visibility = View.VISIBLE
//                if (rvLikes.itemDecorationCount == 0) {
//                    rvLikes.addItemDecoration(object : RecyclerView.ItemDecoration() {
//                        override fun getItemOffsets(
//                            outRect: Rect,
//                            view: View,
//                            parent: RecyclerView,
//                            state: RecyclerView.State
//                        ) {
//                            outRect.top = 25
//                        }
//                    })
//                }
//                momentLikes.forEach { i ->
//                    val models = CommentsModel()
//                    models.commenttext = i.user.fullName
//                    models.uid = i.user.id
//                    models.userurl = i.user.avatar?.url
//                    momentLikesUsers.add(models)
//                }
//
////                if (momentLikeUserAdapters != null) {
////                    momentLikeUserAdapters?.notifyDataSetChanged()
////                } else {
//                momentLikeUserAdapters =
//                    StoryLikesAdapter(
//                        requireActivity(),
//                        momentLikesUsers,
//                        Glide.with(requireContext())
//                    )
//                rvLikes.adapter = momentLikeUserAdapters
//                momentLikeUserAdapters?.notifyDataSetChanged()
//                momentLikeUserAdapters?.userProfileClicked {
//                    LikebottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
//                    LogUtil.debug("UserStoryDetailsFragment : : : $it")
//                    val bundle = Bundle()
//                    bundle.putBoolean(SearchUserProfileFragment.ARGS_FROM_CHAT, false)
//                    bundle.putString("userId", it.uid)
//                    if (userId == it.uid) {
//                        MainActivity.getMainActivity()?.binding?.bottomNavigation?.selectedItemId =
//                            R.id.nav_user_profile_graph
//                    } else {
//                        findNavController().navigate(
//                            destinationId = R.id.action_global_otherUserProfileFragment,
//                            popUpFragId = null,
//                            animType = AnimationTypes.SLIDE_ANIM,
//                            inclusive = true,
//                            args = bundle
//                        )
//                    }
//                }
//                rvLikes.layoutManager = LinearLayoutManager(activity)
////                }
//            } else {
//                no_data.visibility = View.VISIBLE
//                rvLikes.visibility = View.GONE
//            }
//            if (LikebottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
//                LikebottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
//                Log.d("UserStoryDetails", "STATE_EXPANDED")
//            } else {
//                LikebottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
//                Log.d("UserStoryDetails", "STATE_COLLAPSED")
//                likeMomentItemPK = ""
//
////                momentLikesUsers.clear()
////                momentLikeUserAdapters?.addAll(momentLikesUsers)
//            }
//        }
//    }
//
//    private fun showLikeBottomSheet() {
////        Log.i(TAG, "showLikeBottomSheet: UserId: ${item?.node?.pk}")
//        val likebottomSheet = binding.likebottomSheet
//        LikebottomSheetBehavior = BottomSheetBehavior.from(likebottomSheet)
//        LikebottomSheetBehavior.setBottomSheetCallback(object :
//            BottomSheetBehavior.BottomSheetCallback() {
//            override fun onSlide(bottomSheet: View, slideOffset: Float) {
//
//            }
//
//            override fun onStateChanged(bottomSheet: View, newState: Int) {
//                when (newState) {
//                    BottomSheetBehavior.STATE_COLLAPSED -> {
//                        Timber.d("STATE_COLLAPSED")
//                    }
//                    BottomSheetBehavior.STATE_HIDDEN -> {
//                        Timber.d("STATE_HIDDEN")
//                    }
//                    BottomSheetBehavior.STATE_EXPANDED -> {
//                        Timber.d("STATE_EXPANDED")
//                    }
//                    BottomSheetBehavior.STATE_DRAGGING -> {
//                        Timber.d("STATE_DRAGGING")
//                    }
//                    BottomSheetBehavior.STATE_SETTLING -> {
//                        Timber.d("STATE_SETTLING")
//                    }
//                }
//            }
//        })
//    }


}
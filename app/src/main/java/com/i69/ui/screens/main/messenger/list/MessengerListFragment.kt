package com.i69.ui.screens.main.messenger.list


import android.app.AlertDialog
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.apollographql.apollo3.exception.ApolloException
import com.i69.*
import com.i69.applocalization.AppStringConstant
import com.i69.databinding.FragmentMessengerListBinding
import com.i69.databinding.ItemRequestPreviewLongBinding
import com.i69.applocalization.AppStringConstantViewModel
import com.i69.R
import com.i69.type.MessageMessageType
import com.i69.ui.base.BaseFragment
import com.i69.ui.screens.SplashActivity
import com.i69.ui.screens.main.MainActivity
import com.i69.ui.screens.main.messenger.chat.MessengerNewChatFragment
import com.i69.ui.screens.main.messenger.list.MessengerListAdapter.MessagesListListener
import com.i69.ui.viewModels.UserViewModel
import com.i69.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*


class MessengerListFragment : BaseFragment<FragmentMessengerListBinding>(), MessagesListListener {

    private lateinit var job: Job
    private var firstMessage: GetAllRoomsQuery.Edge? = null
    private var broadcastMessage: GetAllRoomsQuery.Edge? = null
    private var allRoom: MutableList<GetAllRoomsQuery.Edge?> = mutableListOf()
    private var isRunning = false
    private val viewModel: UserViewModel by activityViewModels()
    private lateinit var messengerListAdapter: MessengerListAdapter
    var endCursor: String = ""
    var hasNextPage: Boolean = false
    private var userId: String? = null
    private var userToken: String? = null
    lateinit var handler: Handler
    private val viewStringConstModel: AppStringConstantViewModel by activityViewModels()

    override fun getFragmentBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentMessengerListBinding.inflate(inflater, container, false).apply {
            stringConstant = AppStringConstant(requireContext())
        }

    fun performSearch(query: String): List<GetAllRoomsQuery.Edge?> {
        // Convert the query to lowercase for case-insensitive search
        val lowercaseQuery = query.toLowerCase()

        // Use the filter function to find rooms whose target's fullName contains the query
        return allRoom.filter { room ->
            room?.node?.target?.fullName?.toLowerCase()?.contains(lowercaseQuery) == true
                    || room?.node?.userId?.fullName?.toLowerCase()?.contains(lowercaseQuery) == true
        }
    }

    override fun setupTheme() {
        navController = findNavController()

        binding.chatSearch1.setOnClickListener{
            binding.chatSearch1.visibility = View.GONE
            binding.chatSearch.visibility = View.VISIBLE
            binding.chatSearch.isIconified = false
        }

        binding.chatSearch.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                // Handle the search query submission here
//                val filteredRooms = performSearch(query)
//                messengerListAdapter.updateList(filteredRooms)

                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                // Handle the search query text changes here (e.g., filter data)
//                filterData(newText)
                val filteredRooms = performSearch(newText)
                messengerListAdapter.updateList(filteredRooms)
                return true
            }
        })

        binding.chatSearch.setOnCloseListener {
            // Handle the search view close event here
            // You can perform any actions needed when the search view is closed
            // For example, resetting the search results or clearing filters.
//            resetSearch()
            messengerListAdapter.updateList(allRoom)
            binding.chatSearch1.visibility = View.VISIBLE
            binding.chatSearch.visibility = View.GONE
            true // Return true to indicate that you've handled the event.
        }

        viewStringConstModel.data.observe(this@MessengerListFragment) { data ->

            binding.stringConstant = data

        }
        viewStringConstModel.data.also {
            binding.stringConstant = it.value
    //            Log.e("MydataBasesss", it.value!!.messages)
        }

        lifecycleScope.launch {
            Log.e("u id", "-->" + getCurrentUserId())
            Log.e("userToken", "-->" + getCurrentUserToken())
            userId = getCurrentUserId()!!
            userToken = getCurrentUserToken()!!
            Timber.i("usertokenn $userToken")

//            viewStringConstModel.data.observe(viewLifecycleOwner, Observer {
//                binding.stringConstant = it
//
//                Log.e("MyObjectGetted", it.YOUR_CONSTANT_NAME)
//            })
        }


        Timber.i("usertokenn 2 $userToken")
        handler.postDelayed(object : Runnable {
            override fun run() {
                checkForUpdates()
                messengerListAdapter.notifyDataSetChanged()
                handler.postDelayed(this, 30 * 1000)
            }
        }, 30 * 1000)

//        val mDivider: Drawable? = ContextCompat.getDrawable(requireContext(), com.i69app.R.drawable.chat_divider_line)
//        binding.messengerList.addItemDecoration(SimpleDividerItemDecoration(requireContext()))
        messengerListAdapter = MessengerListAdapter(this@MessengerListFragment, userId)
        binding.messengerList.adapter = messengerListAdapter
        getTypeActivity<MainActivity>()?.enableNavigationDrawer()
        //getFirstMessage()
        //getBroadcastMessage()
        if (getMainActivity().pref.getString("chatListRefresh", "false").equals("true")) {
            getMainActivity().pref.edit()?.putString("chatListRefresh", "false")?.apply()
            allRoom.clear()
        }
        isRunning = false
        lifecycleScope.launch {
            viewModel.shouldUpdateAdapter.collect {
                Timber.tag(MainActivity.CHAT_TAG).i("Collecting Data: Update ($it)")
                //if (it) updateList(true)
            }
        }
        try {
            job = lifecycleScope.launch {
                viewModel.newMessageFlow.collect { message ->
                    message?.let { newMessage ->
                        LogUtil.debug("Her in New Message")
                        try {
                            val index = allRoom.indexOfFirst {
                                it?.node?.id == newMessage.roomId.id
                            }
                            val selectedRoom = allRoom[index]!!
                            val room = GetAllRoomsQuery.Edge(
                                GetAllRoomsQuery.Node(
                                    id = selectedRoom.node?.id!!,
                                    name = selectedRoom.node.name,
                                    lastModified = newMessage.timestamp,
                                    blocked = 0,
                                    unread = selectedRoom.node.unread?.toInt()?.plus(1)?.toString(),
                                    messageSet = GetAllRoomsQuery.MessageSet(
                                        edges = selectedRoom.node.messageSet.edges.toMutableList()
                                            .apply {
                                                set(
                                                    0, GetAllRoomsQuery.Edge1(
                                                        GetAllRoomsQuery.Node1(
                                                            content = newMessage.content,
                                                            id = newMessage.id,
                                                            roomId = GetAllRoomsQuery.RoomId(id = newMessage.roomId.id),
                                                            timestamp = newMessage.timestamp,
                                                            messageType = newMessage.messageType,
                                                            read = newMessage.read
                                                        )
                                                    )
                                                )
                                            }
                                    ),
                                    userId = selectedRoom.node.userId,
                                    target = selectedRoom.node.target,
                                )
                            )
                            allRoom.set(index = index, room)
                            Log.e("submitList1", "202")
                            messengerListAdapter.submitList1(allRoom)
                        } catch (e: IndexOutOfBoundsException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        } catch (e: IndexOutOfBoundsException) {
            e.printStackTrace()
        }
        checkForUpdates()
    }

    fun onNewMessage(roomId: String) {
        var position = 0
        allRoom.forEachIndexed { index, edge ->
            if ((edge?.node?.id ?: "") == roomId) {
                position = index
            }
        }
        if (position != 0) {
            getParticularRoomForUpdate(position, roomId, true)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //broadcast = NotificationBroadcast(this);
        val arguments = arguments
        if (arguments != null) {
            val roomID = arguments.get("roomIDNotify") as String?
            if (roomID != null) {
                getParticularRoom(roomID)
                arguments.run {
                    remove("roomIDNotify")
                    clear()
                }
            }
        }
        handler = Handler()
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    private fun checkForUpdates() {
        if (getMainActivity().pref.getString("roomIDNotify", "false").equals("true")) {
            LogUtil.debug("Check for updates In roomIDNotify")
            getMainActivity().pref.edit().putString("roomIDNotify", "false").apply()
            getParticularRoom(getMainActivity().pref.getString("roomID", ""))
        }
        if (getMainActivity().pref.getString("readCount", "false").equals("false")) {
            LogUtil.debug("Check for updates In readCount is false")
            if (getMainActivity().pref.getString("newChat", "false").equals("true")) {
                LogUtil.debug("Check for updates In newChat")
                getMainActivity().pref.edit().putString("newChat", "false").apply()
                //if (allRoom.size!=0)
                //updateList(true)
            }
        }
        if (getMainActivity().pref.getString("readCount", "false").equals("true")) {
            LogUtil.debug("Check for updates In readCount is true")
            getMainActivity().pref.edit().putString("readCount", "false").apply()
            if (getMainActivity().pref.getString("type", "").equals("001")) {
                LogUtil.debug("Check for updates In readCount type is 001")
                //firstMessage
                val position = getMainActivity().pref.getInt("position", 0)
                val id = getMainActivity().pref.getString("id", "")
                if (position > 0) {
                    if (allRoom.size != 0)
                        getParticularFirstMessageUpdate(position, id!!)
                }
            } else if (getMainActivity().pref.getString("type", "").equals("000")) {
                LogUtil.debug("Check for updates In readCount type is 000")
                //braodcastMessage
                val position = getMainActivity().pref.getInt("position", 0)
                val id = getMainActivity().pref.getString("id", "")
                if (allRoom.size != 0)
                    getParticularBraodCastUpdate(position, id!!)
            } else {
                LogUtil.debug("Check for updates In readCount type is else")
                //userMessage
                val position = getMainActivity().pref.getInt("position", 0)
                val id = getMainActivity().pref.getString("id", "")
                if (position > 0) {
                    if (allRoom.size != 0)
                        getParticularRoomForUpdate(position = position, id!!)
                }
            }
            //getMainActivity().pref.edit().putString("roomIDNotify","false").apply()
            //getParticularRoom(getMainActivity().pref.getString("roomID",""))
        }
    }

    override fun onResume() {
        super.onResume()
       /* if (allRoom.size == 0) {
            allRoom.clear()
            updateList(true)
        }
        if (allRoom.size != 0) {
            allRoom.clear()
            updateList(true)
        }*/

        allRoom.clear()
        updateList(true)

        /*LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(
            broadcast!!, IntentFilter(Constants.INTENTACTION)
        );*/
//        val intentFilter = IntentFilter()
//        intentFilter.addAction("com.my.app.onMessageReceived")
//        intentFilter.addAction("gift_Received")
//        activity?.registerReceiver(broadCastReceiver, intentFilter)
        getMainActivity().setDrawerItemCheckedUnchecked(null)

    }

    public fun updateView(state: String) {
//        updateList(true)
    }

    private val broadCastReceiver = object : BroadcastReceiver() {
        override fun onReceive(contxt: Context?, intent: Intent?) {
            val extras = intent?.extras
            val state = extras!!.getString("extra")
            Log.e("TAG_Notification_rece", "onReceive: $state")
            updateView(state.toString())
        }
    }

    override fun onPause() {
        if (this::handler.isInitialized) {
            handler.removeCallbacksAndMessages(null)
        }
        //job.cancel()
        super.onPause()
//        LocalBroadcastManager.getInstance(requireActivity()).unregisterReceiver(broadcast!!)
    }

    override fun setupClickListeners() {
        binding.toolbarHamburger.setOnClickListener { (activity as MainActivity).drawerSwitchState() }
        binding.goToSearchBtn.setOnClickListener { activity?.onBackPressed() }
    }

    override fun onDestroyView() {
        if (this::handler.isInitialized) {
            handler.removeCallbacksAndMessages(null)
        }
        super.onDestroyView()
    }

    private fun makePreviewAnimation() {
        binding.goToSearchBtn.setViewVisible()
        binding.messengerListPreview.setViewVisible()
        binding.messengerList.setVisibleOrInvisible(false)
        binding.messengerListPreview.setViewVisible()
        val display = requireActivity().windowManager!!.defaultDisplay
        val metrics = DisplayMetrics()
        display.getMetrics(metrics)
        binding.subTitle.defaultAnimate(100, 200)
        setupPreviewItem(binding.firstAnimPreview, R.drawable.icon_boy)
        setupPreviewItem(binding.secondAnimPreview, R.drawable.icon_girl)
        setupPreviewItem(binding.thirdAnimPreview, R.drawable.icon_girl_2)
        binding.firstAnimPreview.root.animateFromLeft(200, 300, metrics.widthPixels / 3)
        binding.secondAnimPreview.root.animateFromLeft(200, 500, metrics.widthPixels / 3)
        binding.thirdAnimPreview.root.animateFromLeft(200, 700, metrics.widthPixels / 3)
    }

    private fun setupPreviewItem(
        requestPreviewBinding: ItemRequestPreviewLongBinding,
        preview: Int
    ) {
        requestPreviewBinding.previewImg.setImageResource(preview)
    }

    private fun getParticularRoom(roomID: String?) {
        Timber.d("ROOM_ID= $roomID")
        lifecycleScope.launchWhenResumed {
            val res = try {
                apolloClient(requireContext(), userToken!!).query(GetparticularRoomsQuery(roomID!!))
                    .execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse ${e.message}")
                binding.root.snackbar("${e.message}")
//                binding.root.snackbar("Exception to get room ${e.message}")
                hideProgressView()
                return@launchWhenResumed
            }
            hideProgressView()
            val Rooms = res.data?.room
            val chatBundle = Bundle()
            if (Rooms?.userId!!.id.equals(userId)) {
                chatBundle.putString("otherUserId", Rooms.target.id)
                if (Rooms.target.avatar != null) {
                    chatBundle.putString("otherUserPhoto", Rooms.target.avatar.url ?: "")
                } else {
                    chatBundle.putString("otherUserPhoto", "")
                }
                chatBundle.putString("otherUserName", Rooms.target.fullName)
            } else {
                chatBundle.putString("otherUserId", Rooms.userId.id)
                if (Rooms.userId.avatar != null) {
                    chatBundle.putString("otherUserPhoto", Rooms.userId.avatar.url ?: "")
                } else {
                    chatBundle.putString("otherUserPhoto", "")
                }
                chatBundle.putString("otherUserName", Rooms.userId.fullName)
            }
            chatBundle.putInt("chatId", Rooms.id.toInt())
            findNavController().navigate(R.id.globalUserToNewChatAction, chatBundle)
        }
    }

    private fun getParticularFirstMessageUpdate(position: Int, id: String) {
        lifecycleScope.launchWhenResumed {
            val resFirstMessage = try {
                apolloClient(requireContext(), userToken!!).query(GetFirstMessageQuery())
                    .execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse ${e.message}")
                binding.root.snackbar("${e.message}")
//                binding.root.snackbar("Exception getFirstMessage ${e.message}")
                //hideProgressView()
                return@launchWhenResumed
            }
            if (resFirstMessage.hasErrors()) {
                if (resFirstMessage.errors!![0].nonStandardFields!!["code"].toString() == "InvalidOrExpiredToken"
                ) {
                    // error("User doesn't exist")
                    lifecycleScope.launch(Dispatchers.Main) {
                        userPreferences.clear()
                        //App.userPreferences.saveUserIdToken("","","")
                        val intent = Intent(activity, SplashActivity::class.java)
                        startActivity(intent)
                        requireActivity().finishAffinity()
                    }
                }
            }
            if (resFirstMessage.data?.firstmessage != null) {
                firstMessage = GetAllRoomsQuery.Edge(GetAllRoomsQuery.Node(
                    id = "001",
                    name = resFirstMessage.data?.firstmessage?.firstmessageContent!!,
                    lastModified = resFirstMessage.data?.firstmessage?.firstmessageTimestamp,
                    unread = resFirstMessage.data?.firstmessage?.unread,
                    blocked = 0,
                    messageSet = GetAllRoomsQuery.MessageSet(
                        edges = mutableListOf<GetAllRoomsQuery.Edge1>().apply {
                            add(
                                GetAllRoomsQuery.Edge1(
                                    GetAllRoomsQuery.Node1(
                                        content = "",
                                        id = "001",
                                        roomId = GetAllRoomsQuery.RoomId(id = ""),
                                        timestamp = resFirstMessage.data?.firstmessage?.firstmessageTimestamp!!,
                                        read = "",
                                        messageType = MessageMessageType.C
                                    )
                                )
                            )
                        }
                    ),
                    userId = GetAllRoomsQuery.UserId(
                        null,
                        resFirstMessage.data?.firstmessage?.firstmessageContent!!,
                        null,
                        null,
                        null,
                        null
                    ),
                    target = GetAllRoomsQuery.Target(null, null, null, null, null, null),
                ))
            }
            Log.e("params", "" + firstMessage)
            //hideProgressView()
            if (firstMessage != null) {
                try {
                    allRoom[position] = firstMessage!!
                    Log.e("submitList1", "483")
                    messengerListAdapter.submitList1(allRoom)
                } catch (e: IndexOutOfBoundsException) {
                    e.printStackTrace()
                }
            }
            getMainActivity().updateChatBadge()
        }
    }

    private fun getParticularBraodCastUpdate(position: Int, id: String) {
        lifecycleScope.launchWhenResumed {
            val resBroadcast = try {
                apolloClient(requireContext(), userToken!!).query(GetBroadcastMessageQuery())
                    .execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponsegetBroadcastMessage ${e.message}")
                binding.root.snackbar("${e.message}")
//                binding.root.snackbar("Exception getBroadcastMessage ${e.message}")
                //hideProgressView()
                return@launchWhenResumed
            }
            if (resBroadcast.hasErrors()) {
                if (resBroadcast.errors!![0].nonStandardFields!!["code"].toString() == "InvalidOrExpiredToken"
                ) {
                    // error("User doesn't exist")
                    lifecycleScope.launch(Dispatchers.Main) {
                        userPreferences.clear()
                        //App.userPreferences.saveUserIdToken("","","")
                        val intent = Intent(activity, SplashActivity::class.java)
                        startActivity(intent)
                        requireActivity().finishAffinity()
                    }
                }
            }
            if (resBroadcast.data?.broadcast != null) {
                broadcastMessage = GetAllRoomsQuery.Edge(GetAllRoomsQuery.Node(
                    id = "000",
                    name = resBroadcast.data?.broadcast?.broadcastContent!!,
                    lastModified = resBroadcast.data?.broadcast?.broadcastTimestamp,
                    unread = resBroadcast.data?.broadcast?.unread,
                    blocked = 0,
                    messageSet = GetAllRoomsQuery.MessageSet(
                        edges = mutableListOf<GetAllRoomsQuery.Edge1>().apply {
                            add(
                                GetAllRoomsQuery.Edge1(
                                    GetAllRoomsQuery.Node1(
                                        content = "",
                                        id = "000",
                                        roomId = GetAllRoomsQuery.RoomId(id = ""),
                                        timestamp = resBroadcast.data?.broadcast?.broadcastTimestamp!!,
                                        read = "",
                                        messageType = MessageMessageType.C
                                    )
                                )
                            )
                        }
                    ),
                    userId = GetAllRoomsQuery.UserId(null, "Team i69", null, null, null, null),
                    target = GetAllRoomsQuery.Target(null, null, null, null, null, null),
                ))
            }
            if (broadcastMessage != null && allRoom.size > position) {
                //allRoom.add(0, broadcastMessage)
                allRoom[position] = broadcastMessage!!
                Log.e("submitList1", "548")
                messengerListAdapter.submitList1(allRoom)
            }
            getMainActivity().updateChatBadge()
        }
    }

    private fun getParticularRoomForUpdate(
        position: Int,
        id: String,
        isFromNotification: Boolean = false
    ) {
        lifecycleScope.launchWhenResumed {
            val res = try {
                apolloClient(requireContext(), userToken!!).query(GetAllRoomsQuery(20))
                    .execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse get room ${e.message}")
                binding.root.snackbar("${e.message}")
                // hideProgressView()
                return@launchWhenResumed
            }
            //hideProgressView()
            val room = res.data?.rooms?.edges
            room?.indices?.forEach { i ->
                try {
                    if (id == room[i]!!.node!!.id) {
                        //temp until unread count not fixed
                        allRoom[position] = room[i]!!
                        Log.e("submitList1", "577")
                        messengerListAdapter.submitList1(allRoom)
                        //sharedMomentAdapter.notifyItemChanged(pos)
                        return@forEach
                    }
                } catch (e: IndexOutOfBoundsException) {

                }
            }
            getMainActivity().updateChatBadge()
            if (isFromNotification) {
                Handler(Looper.getMainLooper()).postDelayed({
                    kotlin.run {
                        val notificationManager = requireActivity()
                            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.cancelAll()
                    }
                }, 1000)
            }
        }
    }

    private val formatter =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSZZZ", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

    fun updateList(isProgressShow: Boolean) {
         if (isProgressShow) {
             showProgressView()
         }
        allRoom.clear()



        lifecycleScope.launchWhenResumed {

            ///broadcast message query
            val resBroadcast = try {
                apolloClient(requireContext(), userToken!!).query(GetBroadcastMessageQuery())
                    .execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponsegetBroadcastMessage ${e.message}")
                binding.root.snackbar("${e.message}")
                hideProgressView()
                return@launchWhenResumed
            }
            if (resBroadcast.hasErrors()) {
                if (resBroadcast.errors!![0].nonStandardFields!!["code"].toString() == "InvalidOrExpiredToken"
                ) {
                    // error("User doesn't exist")
                    lifecycleScope.launch(Dispatchers.Main) {
                        userPreferences.clear()
                        //App.userPreferences.saveUserIdToken("","","")
                        val intent = Intent(activity, SplashActivity::class.java)
                        startActivity(intent)
                        requireActivity().finishAffinity()
                    }
                }
            }
            if (resBroadcast.data?.broadcast != null) {
                broadcastMessage = GetAllRoomsQuery.Edge(GetAllRoomsQuery.Node(
                    id = "000",
                    name = resBroadcast.data?.broadcast?.broadcastContent!!,
                    lastModified = resBroadcast.data?.broadcast?.broadcastTimestamp,
                    unread = resBroadcast.data?.broadcast?.unread,
                    blocked = 0,
                    messageSet = GetAllRoomsQuery.MessageSet(
                        edges = mutableListOf<GetAllRoomsQuery.Edge1>().apply {
                            add(
                                GetAllRoomsQuery.Edge1(
                                    GetAllRoomsQuery.Node1(
                                        content = "",
                                        id = "000",
                                        roomId = GetAllRoomsQuery.RoomId(id = ""),
                                        timestamp = resBroadcast.data?.broadcast?.broadcastTimestamp!!,
                                        read = "",
                                        messageType = MessageMessageType.C
                                    )
                                )
                            )
                        }
                    ),
                    userId = GetAllRoomsQuery.UserId(null, "Team i69", null, null, null, null),
                    target = GetAllRoomsQuery.Target(null, null, null, null, null, null),
                ))
            }
            if (broadcastMessage != null) {
                allRoom.add(0, broadcastMessage)

//here
               // messengerListAdapter.submitList1(allRoom)
            }


            val res = try {
                apolloClient(requireContext(), userToken!!).query(GetAllRoomsQuery(20)).execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse all moments ${e.message}")
                binding.root.snackbar("${e.message}")
                hideProgressView()
                return@launchWhenResumed
            }
            if (res.hasErrors() && !res.errors.isNullOrEmpty()) {
                if (!res.errors!![0].nonStandardFields.isNullOrEmpty() &&
                    res.errors!![0].nonStandardFields?.containsKey("code")!! &&
                    res.errors!![0].nonStandardFields?.get("code")
                        .toString() == "InvalidOrExpiredToken"
                ) {
                    Log.e("dddfff", "33333")
                    // error("User doesn't exist")
                    lifecycleScope.launch(Dispatchers.Main) {
                        userPreferences.clear()
                        Log.e("dddfff", "444444")
                        //App.userPreferences.saveUserIdToken("","","")
                        val intent = Intent(activity, SplashActivity::class.java)
                        startActivity(intent)
                        requireActivity().finishAffinity()
                        Log.e("dddfff", "555555")
                    }
                }
            }
            var temp :GetAllRoomsQuery.Edge? = null
            if (allRoom.isNotEmpty())
                temp = allRoom.removeAt(0)
            allRoom.addAll(res.data?.rooms!!.edges)
            allRoom =
                allRoom.filter { it?.node?.messageSet?.edges?.isNotEmpty() == true }.toMutableList()
            allRoom = allRoom.filter { it?.node?.blocked == 0 }.toMutableList()
            //Log.e("listt", "" + allRoom)
            //messengerListAdapter.updateList(allRoom)
            allRoom.sortWith { o1, o2 ->
                try {
                    val date1 = o1?.node?.lastModified?.toString()
                    val date2 = o2?.node?.lastModified?.toString()
                    formatter.parse(date2)?.compareTo(formatter.parse(date1)) ?: 0
                } catch (throwable:Throwable) {
                    0
                }
            }
            if (temp != null)
                allRoom.add(0,temp)
            //here
           // messengerListAdapter.submitList1(allRoom)


            ///first message query
            val resFirstMessage = try {
                apolloClient(requireContext(), userToken!!).query(GetFirstMessageQuery())
                    .execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse getFirstMessage ${e.message}")
                binding.root.snackbar("${e.message}")
                //hideProgressView()
                return@launchWhenResumed
            }
            if (resFirstMessage.hasErrors() && !resFirstMessage.errors.isNullOrEmpty()) {
                if (!resFirstMessage.errors!![0].nonStandardFields.isNullOrEmpty() &&
                    resFirstMessage.errors!![0].nonStandardFields?.containsKey("code") == true &&
                    resFirstMessage.errors!![0].nonStandardFields?.get("code").toString() == "InvalidOrExpiredToken"
                ) {
                    // error("User doesn't exist")
                    lifecycleScope.launch(Dispatchers.Main) {
                        userPreferences.clear()
                        //App.userPreferences.saveUserIdToken("","","")
                        val intent = Intent(activity, SplashActivity::class.java)
                        startActivity(intent)
                        requireActivity().finishAffinity()
                    }
                }
            }
            if (resFirstMessage.data?.firstmessage != null) {
                firstMessage = GetAllRoomsQuery.Edge(GetAllRoomsQuery.Node(
                    id = "001",
                    name = resFirstMessage.data?.firstmessage?.firstmessageContent!!,
                    lastModified = resFirstMessage.data?.firstmessage?.firstmessageTimestamp,
                    unread = resFirstMessage.data?.firstmessage?.unread,
                    blocked = 0,
                    messageSet = GetAllRoomsQuery.MessageSet(
                        edges = mutableListOf<GetAllRoomsQuery.Edge1>().apply {
                            add(
                                GetAllRoomsQuery.Edge1(
                                    GetAllRoomsQuery.Node1(
                                        content = "",
                                        id = "001",
                                        roomId = GetAllRoomsQuery.RoomId(id = ""),
                                        timestamp = resFirstMessage.data?.firstmessage?.firstmessageTimestamp!!,
                                        read = "",
                                        messageType = MessageMessageType.C
                                    )
                                )
                            )
                        }
                    ),
                    userId = GetAllRoomsQuery.UserId(
                        null,
                        resFirstMessage.data?.firstmessage?.firstmessageContent!!,
                        null,
                        null,
                        null,
                        null
                    ),
                    target = GetAllRoomsQuery.Target(null, null, null, null, null, null),
                ))
                allRoom.add(firstMessage)
                var temp :GetAllRoomsQuery.Edge? = null
                if (allRoom.isNotEmpty())
                    temp = allRoom.removeAt(0)
                allRoom.sortWith { o1, o2 ->
                    try {
                        val date1 = o1?.node?.lastModified?.toString()
                        val date2 = o2?.node?.lastModified?.toString()
                        formatter.parse(date2)?.compareTo(formatter.parse(date1)) ?: 0
                    } catch (throwable:Throwable) {
                        0
                    }
                }
                if (temp!= null)
                    allRoom.add(0,temp)
                //here
                //messengerListAdapter.submitList1(allRoom)
            }
            hideProgressView()
            messengerListAdapter.submitList1(allRoom)

            if (allRoom.size != 0) {
                if (res.data?.rooms?.pageInfo?.endCursor != null) {
                    endCursor = res.data?.rooms!!.pageInfo.endCursor!!
                    hasNextPage = res.data?.rooms!!.pageInfo.hasNextPage
                }
            }
            var totoalunread = 0
            allRoom.indices.forEach { i ->
                val data = allRoom[i]
                totoalunread = if (totoalunread == 0 && data!!.node!!.unread!!.toInt()>0) {
                    1
                    //data!!.node!!.unread!!.toInt()
                } else{
                    if(totoalunread > 0 && data!!.node!!.unread!!.toInt()>0) {
                        totoalunread + 1//data!!.node!!.unread!!.toInt()
                    }else
                        totoalunread
                }
            }
            try {
                //getMainActivity().binding.navView.updateMessagesCount(totoalunread)
                getMainActivity().binding.bottomNavigation.addBadge(totoalunread)
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
        Handler(Looper.getMainLooper()).postDelayed({
            kotlin.run {
                val notificationManager = requireActivity()
                    .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancelAll()
            }
        }, 1000)

    }

    override fun onItemClick(AllroomEdge: GetAllRoomsQuery.Edge, position: Int) {
        /*if (AllroomEdge.node?.id == "firstName" || AllroomEdge.node?.id == "broadcast") {
            Toast.makeText(requireContext(),"work in progress",Toast.LENGTH_SHORT).show()
            return
        }*/
        viewModel.setSelectedMessagePreview(AllroomEdge)
        val chatBundle = Bundle()
        LogUtil.debug("NodeId : : ${AllroomEdge.node?.id}")
        when (AllroomEdge.node?.id) {
            "001" -> {
                if (AllroomEdge.node.unread?.toInt()!! > 0) {
                    getMainActivity().pref.edit().putString("readCount", "true")
                        .putString("type", "001").putString("id", AllroomEdge.node.id)
                        .putInt("position", position).apply()
                }
                chatBundle.putString("otherUserId", "")
                chatBundle.putString("otherUserPhoto", "")
                chatBundle.putString("otherUserName", AllroomEdge.node.userId.fullName)
                chatBundle.putInt("otherUserGender", 0)
                chatBundle.putString("ChatType", "001")
                chatBundle.putInt("chatId", 0)
                findNavController().navigate(R.id.globalUserToNewChatAction, chatBundle)
            }
            "000" -> {
                if (AllroomEdge.node.unread?.toInt()!! > 0) {
                    getMainActivity().pref.edit().putString("readCount", "true")
                        .putString("type", "000").putString("id", AllroomEdge.node.id)
                        .putInt("position", position).apply()
                }
                chatBundle.putString("otherUserId", "")
                chatBundle.putString("otherUserPhoto", "")
                chatBundle.putString("otherUserName", AllroomEdge.node.userId.fullName)
                chatBundle.putInt("otherUserGender", 0)
                chatBundle.putString("ChatType", "000")
                chatBundle.putInt("chatId", 0)
                findNavController().navigate(R.id.globalUserToNewChatAction, chatBundle)
            }
            else -> {
                if (AllroomEdge.node?.unread?.toInt()!! > 0) {
                    getMainActivity().pref.edit().putString("readCount", "true")
                        .putString("type", "User").putString("id", AllroomEdge.node.id)
                        .putInt("position", position).apply()
                }
                if (AllroomEdge.node.userId.id.equals(userId)) {
                    chatBundle.putString("otherUserId", AllroomEdge.node.target.id)
                    if (AllroomEdge.node.target.avatar != null) {
                        chatBundle.putString(
                            "otherUserPhoto",
                            AllroomEdge.node.target.avatar.url ?: ""
                        )
                    } else {
                        chatBundle.putString("otherUserPhoto", "")
                    }
                    chatBundle.putString("otherUserName", AllroomEdge.node.target.fullName)
                    chatBundle.putInt("otherUserGender", AllroomEdge.node.target.gender ?: 0)
                    chatBundle.putString("ChatType", "Normal")
                } else {
                    chatBundle.putString("otherUserId", AllroomEdge.node.userId.id)
                    if (AllroomEdge.node.userId.avatar != null) {
                        chatBundle.putString(
                            "otherUserPhoto",
                            AllroomEdge.node.userId.avatar.url ?: ""
                        )
                    } else {
                        chatBundle.putString("otherUserPhoto", "")
                    }
                    chatBundle.putString("otherUserName", AllroomEdge.node.userId.fullName ?: "")
                    chatBundle.putInt("otherUserGender", AllroomEdge.node.userId.gender ?: 0)
                    chatBundle.putString("ChatType", "Normal")
                }
                chatBundle.putInt("chatId", AllroomEdge.node.id.toInt())
                findNavController().navigate(R.id.globalUserToNewChatAction, chatBundle)
            }
        }
    }

    override fun onItemDeleteClicked(roomId: String, position: Int) {
        Log.d("MLF", "onItemDeleteClicked: $roomId")
        showDeleteConfirmationDialog(roomId)
    }

    private fun showDeleteConfirmationDialog(roomId: String) {
        val dialogLayout = layoutInflater.inflate(R.layout.dialog_delete, null)
        val headerTitle = dialogLayout.findViewById<TextView>(R.id.header_title)
        val noButton = dialogLayout.findViewById<TextView>(R.id.no_button)
        val yesButton = dialogLayout.findViewById<TextView>(R.id.yes_button)

        val title = "Are you sure you want to delete?"

        headerTitle.text = title
        noButton.text = AppStringConstant(requireContext()).no
        yesButton.text = AppStringConstant(requireContext()).yes

        val builder = AlertDialog.Builder(MainActivity.getMainActivity(),R.style.DeleteDialogTheme)
        builder.setView(dialogLayout)
        builder.setCancelable(false)
        val dialog = builder.create()

        noButton.setOnClickListener {
            dialog.dismiss()
        }

        yesButton.setOnClickListener {
            dialog.dismiss()
            deleteChatRoom(roomId)
        }

        dialog.show()
    }

    private fun deleteChatRoom(roomId: String) {
        if (roomId.isNotEmpty()) {
            showProgressView()
            viewModel.deleteChatRoom(roomId.toInt(), userToken.toString()) {
                Log.d("MLF", "onItemDeleteClicked: ${it?.message}")
                hideProgressView()
                when(it) {
                    is Resource.Success -> {
                        Log.d("MLF", "onItemDeleteClicked: ${it?.data?.data?.message}")
                        updateList(true)

                    }
                    is Resource.Error -> {

                    }
                    is Resource.Loading -> {

                    }
                }
            }
        }

    }


    fun getMainActivity() = activity as MainActivity

}

package com.i69.ui.viewModels


import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.i69.GetAllUserMomentsQuery
import com.i69.data.models.Moment
import com.i69.data.remote.repository.UserMomentsRepository
import com.i69.data.remote.responses.MomentLikes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.net.URL
import javax.inject.Inject


@HiltViewModel
class UserMomentsModelView @Inject constructor(private val userMomentsRepo: UserMomentsRepository): ViewModel() {
    val offlineUserMomentsList = mutableListOf<Moment>()
    val userMomentsList=ArrayList<GetAllUserMomentsQuery.Edge>()
    //val arrayListLiveData = MutableLiveData<ArrayList<GetAllUserMomentsQuery.Edge>>()
    var endCursorN: String=""
    var hasNextPageN: Boolean= false
    val errorMessage=MutableLiveData<String>()

    val coinPrice = ArrayList<MomentLikes>()
//    private val coinPrice: MutableLiveData<ArrayList<MomentLikes>> = MutableLiveData()

    fun getAllOfflineMoments(callback: (List<Moment>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val moments = userMomentsRepo.getMomentsList()
            callback.invoke(moments)
        }
    }

    fun getAllMoments(context: Context, token: String,width: Int, size: Int, i: Int, endCursors: String, callback: (String?) -> Unit) {
        userMomentsRepo.getUserMoments(context,viewModelScope,
            token = token,
            width = width,
            size = size,
            i=i,
            endCursors = endCursors) {
                allUserMoments,endCursor,hasNextPage, error ->

            //this.userMomentsList.clear()
            Log.d("USMV", "getAllMoments: ")
            this.userMomentsList.addAll(allUserMoments)

            Log.d("OfflineMoments", "getAllMoments: $i ${userMomentsList.size}")
            if (i == 10) {
                viewModelScope.launch(Dispatchers.IO) {
                    userMomentsRepo.deleteAllOfflineMoments()

                    userMomentsList.forEach {
                        offlineUserMomentsList.add(Moment(node = it, image = null))
//                        saveImage(it.node?.file.toString()) { image ->
//                            allUserMomentsList.add(Moment(node = it, image = image))
//                        }
                    }

                    userMomentsRepo.insertMomentsList(offlineUserMomentsList)
                    Log.d("OfflineMoments", "inserted: ${offlineUserMomentsList.size}")
                }
            }

            endCursorN=endCursor
            hasNextPageN=hasNextPage
            //this.arrayListLiveData.postValue(allUserMoments)
            callback.invoke(error)
        }
    }

    private fun saveImage(url: String, imageSaved: (ByteArray) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val imageUrl = URL(url)
                val ucon = imageUrl.openConnection()
                val `is` = ucon.getInputStream()
                val baos = ByteArrayOutputStream()
                val buffer = ByteArray(1024)
                var read: Int
                while (`is`.read(buffer, 0, buffer.size).also { read = it } != -1) {
                    baos.write(buffer, 0, read)
                }
                baos.flush()
                imageSaved.invoke(baos.toByteArray())
            } catch (e: Exception) {
                Log.d("ImageManager", "Error: $e")
            }
        }
    }

     fun getMomentLikes(token: String, momentPk: String, callback: (String?) -> Unit) {
        userMomentsRepo.getMomentLikes(viewModelScope, token = token, momentPk = momentPk) {
                allUserMoments ->

            this.coinPrice.clear()
            this.coinPrice.addAll(allUserMoments)
//            endCursorN=endCursor
//            hasNextPageN=hasNextPage
//            this._coinPrice.postValue(allUserMoments)
            callback.invoke(null)
        }


    }

//    fun getMomentLikes(token: String, momentPk: String): LiveData<ArrayList<MomentLikes>> =
//        userMomentsRepo.getMomentLikes(viewModelScope, momentPk, token)
}
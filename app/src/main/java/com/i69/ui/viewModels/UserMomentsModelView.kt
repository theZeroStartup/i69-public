package com.i69.ui.viewModels


import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.i69.GetAllUserMomentsQuery
import com.i69.data.remote.repository.UserMomentsRepository
import com.i69.data.remote.responses.MomentLikes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class UserMomentsModelView @Inject constructor(private val userMomentsRepo: UserMomentsRepository): ViewModel() {
    val userMomentsList=ArrayList<GetAllUserMomentsQuery.Edge>()
    //val arrayListLiveData = MutableLiveData<ArrayList<GetAllUserMomentsQuery.Edge>>()
    var endCursorN: String=""
    var hasNextPageN: Boolean= false
    val errorMessage=MutableLiveData<String>()

    val coinPrice = ArrayList<MomentLikes>()
//    private val coinPrice: MutableLiveData<ArrayList<MomentLikes>> = MutableLiveData()


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
            endCursorN=endCursor
            hasNextPageN=hasNextPage
            //this.arrayListLiveData.postValue(allUserMoments)
            callback.invoke(error)
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
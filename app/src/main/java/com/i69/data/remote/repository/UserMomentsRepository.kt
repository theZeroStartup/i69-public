package com.i69.data.remote.repository

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.apollographql.apollo3.exception.ApolloException
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.i69.GetAllUserMomentsQuery
import com.i69.data.models.Moment
import com.i69.data.remote.api.GraphqlApi
import com.i69.data.remote.responses.MomentLikes
import com.i69.ui.screens.main.moment.db.MomentDao
import com.i69.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class UserMomentsRepository @Inject constructor(
    private val api: GraphqlApi,
    private val momentsDao: MomentDao
) {
    private var _coinPrice: ArrayList<MomentLikes> = ArrayList()
    private val coinPrice: MutableLiveData<ArrayList<MomentLikes>> = MutableLiveData()

    fun insertMomentsList(moments: List<Moment>) {
        return momentsDao.insertMomentsList(moments)
    }

    fun getMomentsList(): List<Moment> {
        return momentsDao.getMomentsList()
    }

    fun deleteAllOfflineMoments() {
        momentsDao.deleteAllMoments()
    }

    fun getMomentLikes(viewModelScope: CoroutineScope,
                               momentPk: String,
                               token: String, callback: (ArrayList<MomentLikes>) -> Unit) {

        viewModelScope.launch(Dispatchers.IO) {

            _coinPrice.clear()
            val queryName = "getMomentLikes"

            val query = StringBuilder()
                .append("query {")
                .append("$queryName(momentPk: $momentPk){ ")
                .append("user{  ")
                .append("id, ")
                .append("username, ")
                .append("email, ")
                .append("fullName, ")
                .append("avatar{ ")
                .append("url ")
                .append("}")
                .append("}")
                .append("}")
                .append("}")
                .toString()

            try {
                val result = api.callApi(token = "Token $token", body = query.getGraphqlApiBody())
                val jsonObject = Gson().fromJson(result.body(), JsonObject::class.java)
                Timber.i("Query: $query")
                Timber.w("Result: ${result.body()}")

                when {
                    result.isSuccessful -> {
                        val dataJsonObject = jsonObject["data"].asJsonObject
                        val coinSettingsJsonArray = dataJsonObject[queryName].asJsonArray

                        coinSettingsJsonArray.forEach { jsonElement ->
                            val json = Gson().fromJson(jsonElement, MomentLikes::class.java)
                            _coinPrice.add(json)
                        }

                        val error: String? = if (jsonObject.has("errors")) {
                            jsonObject["errors"].asJsonArray[0].asJsonObject["message"].asString
                        } else null


                        if (error.isNullOrEmpty()) coinPrice.postValue(_coinPrice)

                        callback(_coinPrice)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }

    }

//    fun getMomentLikes(
//        viewModelScope: CoroutineScope,
//        momentPk: String,
//        token: String,
//    ): MutableLiveData<ArrayList<MomentLikes>> {
//
//        _coinPrice.clear()
//        if (_coinPrice.isEmpty()) {
//            viewModelScope.launch(Dispatchers.IO) {
//                loadMomentsLikes(token, momentPk)
//
//            }
//        }
//        coinPrice.value = _coinPrice
//        return coinPrice
//    }

    private suspend fun loadMomentsLikes(token: String, momentPk: String) {
//        val queryName = "selfMomentLikes"

        val queryName = "getMomentLikes"

        val query = StringBuilder()
            .append("query {")
            .append("$queryName(momentPk: $momentPk){ ")
            .append("user{  ")
            .append("id, ")
            .append("username, ")
            .append("email, ")
            .append("fullName, ")
            .append("avatar{ ")
            .append("url ")
            .append("}")
            .append("}")
            .append("}")
            .append("}")
            .toString()

        try {
            val result = api.callApi(token = "Token $token", body = query.getGraphqlApiBody())
            val jsonObject = Gson().fromJson(result.body(), JsonObject::class.java)
            Timber.i("Query: $query")
            Timber.w("Result: ${result.body()}")

            when {
                result.isSuccessful -> {
                    val dataJsonObject = jsonObject["data"].asJsonObject
                    val coinSettingsJsonArray = dataJsonObject[queryName].asJsonArray

                    coinSettingsJsonArray.forEach { jsonElement ->
                        val json = Gson().fromJson(jsonElement, MomentLikes::class.java)
                        _coinPrice.add(json)
                    }

                    val error: String? = if (jsonObject.has("errors")) {
                        jsonObject["errors"].asJsonArray[0].asJsonObject["message"].asString
                    } else null


                    if (error.isNullOrEmpty()) coinPrice.postValue(_coinPrice)

//                    callback(_coinPrice)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun getUserMoments(context: Context, viewModelScope: CoroutineScope,
                       token: String, width: Int,
                       size: Int,
                       i: Int,
                       endCursors: String,
                       callback: (ArrayList<GetAllUserMomentsQuery.Edge>,endCursor: String,hasNextPage: Boolean, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = try {
                Log.d("UserMomentsFragment", "getUserMoments: $width $size $i $endCursors")
                apolloClientForContore(context,token).query(GetAllUserMomentsQuery(width = width, characterSize = size,first=i,endCursors,"")).execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse ${e.message}")
                return@launch
            }
            if(res.hasErrors())
            {
//                Log.e("rr2rrr","-->"+res.errors!!.get(0).nonStandardFields!!.get("code").toString())
                if(res.errors!!.get(0).nonStandardFields?.get("code").toString().equals("InvalidOrExpiredToken"))
                {
                    // error("User doesn't exist")
                    viewModelScope.launch(Dispatchers.Main) {
                    }
                }
            }

            val allmoments = res.data?.allUserMoments?.edges
            var endCursor: String=""
            var hasNextPage: Boolean= false
            endCursor = res.data?.allUserMoments!!.pageInfo.endCursor!!
            hasNextPage = res.data?.allUserMoments!!.pageInfo.hasNextPage
            if(allmoments?.size!=0) {

                val allUserMomentsFirst: ArrayList<GetAllUserMomentsQuery.Edge> = ArrayList()


                allmoments?.indices?.forEach { i ->
                    allmoments[i]?.let { allUserMomentsFirst.add(it) }
                }
               callback(allUserMomentsFirst,endCursor,hasNextPage,null)
            }
        }
    }


//    private suspend fun getAllMoments(
//        token: String,
//        query: String,
//        getMomentQuryName: String,
//        callback: (List<User>, List<User>, List<User>, String?) -> Unit
//    ) {
//        try {
//            val result = api.callApi(token = "Token $token", body = query.getGraphqlApiBody())
//            val jsonObject = Gson().fromJson(result.body(), JsonObject::class.java)
//            Timber.i("Query: $query")
//            Timber.w("Result: ${result.body()}")
//
//            when {
//                result.isSuccessful -> {
//                    val allMomentsList = ArrayList<User>()
//
//                    val dataJsonObject = jsonObject["data"].asJsonObject
//                    val allMometsArrays = dataJsonObject[getMomentQuryName].asJsonArray
//
//                    allMometsArrays.forEach { jsonElement ->
//                        val json = Gson().fromJson(jsonElement, User::class.java)
//                        allMomentsList.add(json)
//                    }
//
//
//                    val error: String? = if (jsonObject.has("errors")) {
//                        jsonObject["errors"].asJsonArray[0].asJsonObject["message"].asString
//                    } else {
//                        null
//                    }
//
//                    if (error.isNullOrEmpty())
//                        callback.invoke(allMomentsList, emptyList(), emptyList(),null)
//                    else
//                        callback.invoke(emptyList(), emptyList(), emptyList(), error)
//
//                }
//                else -> callback.invoke(emptyList(), emptyList(), emptyList(), "Something went wrong! Please try again later!")
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//            callback.invoke(emptyList(), emptyList(), emptyList(), "Something went wrong! Please try again later!")
//        }
//    }

}
package com.i69.data.remote.repository

import android.os.Bundle
import android.util.Log
import com.facebook.AccessToken
import com.facebook.GraphRequest
import com.facebook.GraphResponse
import com.facebook.login.LoginResult
import com.i69.data.remote.api.GraphqlApi
import com.i69.data.remote.requests.LoginRequest
import com.i69.data.remote.responses.LoginResponse
import com.i69.data.remote.responses.ResponseBody
import com.i69.utils.Resource
import com.i69.utils.getResponse
import com.i69.utils.getStringOrNull
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton


private const val AVATAR_URL = "https://graph.facebook.com/%s/picture?type=large&width=1024&height=1024"
private const val EXTRA_FIELDS = "fields"
private const val EXTRA_FIELDS_VALUE = "id,name,email,picture.type(large)"
private const val EXTRA_NAME = "name"
private const val EXTRA_ID = "id"

@Singleton
class LoginRepository @Inject constructor(
    private val api: GraphqlApi
) {

    suspend fun login(loginRequest: LoginRequest): Resource<ResponseBody<LoginResponse>> {
        val queryName = "socialAuth"
        val query = StringBuilder()
            .append("mutation {")
            .append("$queryName (")
            .append("accessToken: \"${loginRequest.accessToken}\", ")
            .append("accessVerifier: \"${loginRequest.accessVerifier}\", ")
            .append("provider: \"${loginRequest.provider}\"")
            .append(") {")
            .append("id, email, token, isNew, username")
            .append("}")
            .append("}")
            .toString()

        Log.e("loginSocialAuths", query);
        Timber.d("LOG " + query)
        Timber.d("LOG " + queryName)
        return api.getResponse(query, queryName, "")
    }

    fun getUserDataFromFacebook(loginResult: LoginResult, callback: (String?, ArrayList<String>?) -> Unit) {
        val request = GraphRequest.newMeRequest(AccessToken.getCurrentAccessToken()) { fbRes, _ ->
            val name = fbRes?.getStringOrNull(EXTRA_NAME)

//            val photos = arrayListOf(String.format(AVATAR_URL, fbRes?.getString(EXTRA_ID)))
//            val photos = arrayListOf(String.format(AVATAR_URL, fbRes?.getString(EXTRA_ID)))
            callback(name, null)

        }

        val params = Bundle()
        params.putString(EXTRA_FIELDS, EXTRA_FIELDS_VALUE)
        request.parameters = params
        request.executeAsync()
    }

}
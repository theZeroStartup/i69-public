package com.i69.ui.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facebook.login.LoginResult
import dagger.hilt.android.lifecycle.HiltViewModel
import com.i69.data.models.Id
import com.i69.data.models.User
import com.i69.data.remote.repository.*
import com.i69.data.remote.requests.LoginRequest
import com.i69.data.remote.responses.CoinsResponse
import com.i69.data.remote.responses.DefaultPicker
import com.i69.data.remote.responses.LoginResponse
import com.i69.data.remote.responses.ResponseBody
import com.i69.ui.base.profile.PUBLIC
import com.i69.utils.Resource
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginRepository: LoginRepository,
    private val appRepository: AppRepository,
    val userUpdateRepository: UserUpdateRepository,
    private val coinRepository: CoinRepository,
    private val userDetailsRepository: UserDetailsRepository,

    ) : ViewModel() {

    private var authUser: User? = null
    var token: String? = null


    fun getDefaultPickers(userToken: String): LiveData<DefaultPicker> = appRepository.getDefaultPickers(viewModelScope, userToken)

    suspend fun login(loginRequest: LoginRequest): Resource<ResponseBody<LoginResponse>> =
        loginRepository.login(loginRequest)

    fun getUserDataFromFacebook(loginResult: LoginResult, callback: (String?, ArrayList<String>?) -> Unit) =
        loginRepository.getUserDataFromFacebook(loginResult, callback)

    /// Coin
    suspend fun deductCoin(userId: String, token: String, deductCoinMethod: com.i69.data.enums.DeductCoinMethod): Resource<ResponseBody<CoinsResponse>> =
        coinRepository.deductCoin(userId, token = token, deductCoinMethod)

    fun getAuthUser() = authUser

    fun setAuthUser(updated: User) {
        authUser = updated
    }
    suspend fun getLanguage(userId: String, token: String)=
        userDetailsRepository.getLanguage(userId,token).data
    /// Update
    suspend fun updateProfile(user : User, token : String): Resource<ResponseBody<Id>> = userUpdateRepository.updateProfile(user, token = token)

    suspend fun uploadImage(userId: String, token: String, filePath: String): Resource<ResponseBody<Id>> =
        userUpdateRepository.uploadImage(userId = userId, token = token, filePath, PUBLIC)

    suspend fun uploadImage2(userId: String, token: String, filePath: File): Resource<ResponseBody<Id>> =
        userUpdateRepository.uploadImage2(userId = userId, token = token, filePath, PUBLIC)

    suspend fun updateLanguage(languageCode:String, userid:String ,token:String): Resource<ResponseBody<Id>> = userUpdateRepository.updateLanguage(languageCode =languageCode,userid=userid, token = token )

}
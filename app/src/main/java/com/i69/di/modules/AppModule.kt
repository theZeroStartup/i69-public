package com.i69.di.modules

import android.content.Context
import androidx.room.Room
import com.i69.BuildConfig
import com.i69.applocalization.AppStringConstant
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import com.i69.data.remote.api.Api
import com.i69.data.remote.api.GraphqlApi
import com.i69.data.remote.repository.*
import com.i69.profile.db.dao.UserDao
import com.i69.db.AppDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private fun getRetrofit(converterFactory: Converter.Factory, contentType: String): Retrofit {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY

        val clientBuilder = OkHttpClient.Builder()

        if (contentType.isNotEmpty()) {
            clientBuilder.addNetworkInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Content-Type", contentType)
                    .build()
                return@addNetworkInterceptor chain.proceed(request)
            }
        }

        val client = clientBuilder
            .addInterceptor(logging)
            .connectTimeout(1, TimeUnit.MINUTES)
            .readTimeout(1, TimeUnit.MINUTES)
            .writeTimeout(1, TimeUnit.MINUTES)
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(converterFactory)
            .build()
    }

    @Singleton
    @Provides
    fun provideGraphqlApi(): GraphqlApi = getRetrofit(
        converterFactory = ScalarsConverterFactory.create(),
        "application/json"
    ).create(GraphqlApi::class.java)

    @Singleton
    @Provides
    fun provideApi(): Api = getRetrofit(
        converterFactory = GsonConverterFactory.create(),
        ""
    ).create(Api::class.java)

    @Singleton
    @Provides
    fun provideAppRepository(api: GraphqlApi) = AppRepository(api)

    @Singleton
    @Provides
    fun provideLoginRepository(api: GraphqlApi) = LoginRepository(api)

    @Singleton
    @Provides
    fun provideCoinRepository(api: GraphqlApi) = CoinRepository(api)

    @Singleton
    @Provides
    fun provideUserUpdateRepository(graphqlApi: GraphqlApi, api: Api) = UserUpdateRepository(graphqlApi = graphqlApi, api = api)

    @Singleton
    @Provides
    fun provideUserDetailsRepository(api: GraphqlApi, userUpdateRepository: UserUpdateRepository, dao: UserDao, @ApplicationContext context: Context) = UserDetailsRepository(api, userUpdateRepository, dao, context)

   /* @Singleton
    @Provides
    fun provideChatRepository(api: GraphqlApi, userDetailsRepository: UserDetailsRepository, userDao: UserDao, chatsDao: ChatDialogsDao) = ChatRepository(api, userDetailsRepository, chatsDao, userDao)

    @Singleton
    @Provides
    fun provideChatLoginRepository() = ChatLoginRepository()*/

    @Singleton
    @Provides
    fun provideSearchRepository(api: GraphqlApi) = SearchRepository(api)

    @Singleton
    @Provides
    fun provideContext(
        @ApplicationContext context: Context
    ) = context

    @Singleton
    @Provides
    fun provideDb(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "app_db.db")
            .fallbackToDestructiveMigration().build()

    @Singleton
    @Provides
    fun provideUserDao(db: AppDatabase) = db.userDao()

    @Singleton
    @Provides
    fun provideChatDialogsDao(db: AppDatabase) = db.chatDialogDao()

    @Provides
    fun provideAppStringConstant(context: Context) = AppStringConstant(context)
}
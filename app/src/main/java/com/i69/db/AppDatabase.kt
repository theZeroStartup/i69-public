package com.i69.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.i69.chat.model.ModelQBChatDialogs
import com.i69.chat.dao.ChatDialogsDao
import com.i69.data.models.User
import com.i69.data.remote.responses.DefaultPicker
import com.i69.profile.db.converters.PickerConverters
import com.i69.profile.db.converters.UserConverters
import com.i69.profile.db.dao.UserDao

@Database(entities = [User::class, DefaultPicker::class, ModelQBChatDialogs::class], version = 19, exportSchema = false)
@TypeConverters(UserConverters::class, PickerConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao

    abstract fun chatDialogDao(): ChatDialogsDao

}

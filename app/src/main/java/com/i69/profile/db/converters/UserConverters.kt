package com.i69.profile.db.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.i69.data.models.BlockedUser
import com.i69.data.models.Photo
import com.i69.data.models.UserAttrTranslation
import com.i69.data.models.UserSubscription
import java.util.*

class UserConverters {
    var gson = Gson()
    @TypeConverter
    fun toPhotosString(list: MutableList<Photo>?): String{
        val type = object: TypeToken<MutableList<Photo>>(){}.type
        return gson.toJson(list, type)
    }

    @TypeConverter
    fun stringToPhotosList(info: String): MutableList<Photo>{
        val type = object: TypeToken<MutableList<Photo>>(){}.type
        return gson.fromJson(info, type)
    }


    @TypeConverter
    fun toUserAttrTranslationString(list: MutableList<UserAttrTranslation>?): String{
        val type = object: TypeToken<MutableList<UserAttrTranslation>>(){}.type
        return gson.toJson(list, type)
    }

    @TypeConverter
    fun stringToUserAttrTranslationList(info: String): MutableList<UserAttrTranslation>{
        val type = object: TypeToken<MutableList<UserAttrTranslation>>(){}.type
        return gson.fromJson(info, type)
    }



    @TypeConverter
    fun toDoubleList(list: MutableList<Double>?): String?{
        val type = object: TypeToken<MutableList<Double>>(){}.type
        return gson.toJson(list, type)
    }

    @TypeConverter
    fun stringToDoubleList(info: String): MutableList<Double>{
        val type = object: TypeToken<MutableList<Double>>(){}.type
        return gson.fromJson(info, type)
    }

    @TypeConverter
    fun toIntList(list: MutableList<Int>?): String?{
        val type = object: TypeToken<MutableList<Int>>(){}.type
        return gson.toJson(list, type)
    }

    @TypeConverter
    fun stringToIntList(info: String): MutableList<Int>{
        val type = object: TypeToken<MutableList<Int>>(){}.type
        return gson.fromJson(info, type)
    }

    @TypeConverter
    fun toStringList(list: MutableList<String>?): String?{
        val type = object: TypeToken<MutableList<String>>(){}.type
        return gson.toJson(list, type)
    }

    @TypeConverter
    fun stringToStringList(info: String): MutableList<String>{
        val type = object: TypeToken<MutableList<String>>(){}.type
        return gson.fromJson(info, type)
    }

    @TypeConverter
    fun toBlockedList(list: MutableList<BlockedUser>?): String?{
        val type = object: TypeToken<MutableList<BlockedUser>>(){}.type
        return gson.toJson(list, type)
    }



    @TypeConverter
    fun stringToBlockedUserList(info: String): MutableList<BlockedUser>{
        val type = object: TypeToken<MutableList<BlockedUser>>(){}.type
        return gson.fromJson(info, type)
    }


//    @TypeConverter
//    fun toUserSubscriptionList(list: MutableList<UserSubscription>?): String?{
//        val type = object: TypeToken<MutableList<UserSubscription>>(){}.type
//        return gson.toJson(list, type)
//    }
//
//
//    @TypeConverter
//    fun stringToUserSubscriptionList(info: String): MutableList<UserSubscription>{
//        val type = object: TypeToken<MutableList<UserSubscription>>(){}.type
//        return gson.fromJson(info, type)
//    }


    @TypeConverter
    fun toUserSubscriptionList(list: UserSubscription?): String?{
        val type = object: TypeToken<UserSubscription>(){}.type
        return gson.toJson(list, type)
    }


    @TypeConverter
    fun stringToUserSubscriptionList(info: String): UserSubscription{
        val type = object: TypeToken<UserSubscription>(){}.type
        return gson.fromJson(info, type)
    }


    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time?.toLong()
    }
}
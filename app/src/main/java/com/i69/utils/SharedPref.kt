package com.i69.utils

import android.content.Context
import com.google.gson.Gson
import com.i69.applocalization.AppStringConstant

class SharedPref(context: Context) {

    val sharedpref = context.getSharedPreferences("i69languagechange_pref", Context.MODE_PRIVATE)
    val editor = sharedpref.edit()

    val LANGUAGECHANGE = "LANGUAGECHANGE"
    val ISFROMSETTINGS = "ISFROMSETTINGS"
    val OPERATORCODE = "OPERATORCODE"
    val OPERATIONREFERENCE = "OPERATIONREFERENCE"
    val FIRTTIMELANGUAGESET = "FIRTTIMELANGUAGESET"
    val ATTRTRANSLATER = "ATTRTRANSLATER"


    fun setAttrTranslater(appConst : AppStringConstant){
        val gson = Gson()
        val json: String = gson.toJson(appConst)
        editor.apply {
            putString(ATTRTRANSLATER, json)
            apply()
        }
    }


    fun getAttrTranslater(): AppStringConstant?{
        val gson = Gson()

       var cb= sharedpref.getString(ATTRTRANSLATER, null)

        if(cb==null){
            return null
        }
      return  gson.fromJson(cb, AppStringConstant::class.java)

    }

    fun setLanguage(language: Boolean) {
        editor.apply {
            putBoolean(LANGUAGECHANGE, language)
            apply()
        }
    }

    fun getLanguage() : Boolean {
        return sharedpref.getBoolean(LANGUAGECHANGE,false)
    }

    fun setLanguageFromSettings(isFromSettings : Boolean) {
        editor.apply {
            putBoolean(ISFROMSETTINGS, isFromSettings)
            apply()
        }
    }

    fun getLanguageFromSettings() : Boolean {
        return sharedpref.getBoolean(ISFROMSETTINGS,false)
    }

    fun setOperatorCode(operatorCode : String) {
        editor.apply {
            putString(OPERATORCODE, operatorCode)
            apply()
        }
    }

    fun getOperatorCode() : String {
        return sharedpref.getString(OPERATORCODE,"null") ?: "null"
    }

    fun setOperatorReference(operationReference : String) {
        editor.apply {
            putString(OPERATIONREFERENCE, operationReference)
            apply()
        }
    }

    fun getOperatorReference() : String {
        return sharedpref.getString(OPERATIONREFERENCE,"null") ?: "null"
    }

    fun setFirtsTimeLanguage(langageSet : Boolean){
        editor.apply {
            putBoolean(FIRTTIMELANGUAGESET, langageSet)
            apply()
        }
    }

    fun getFirstTimeLanguage() : Boolean {
        return sharedpref.getBoolean(FIRTTIMELANGUAGESET,false)
    }

}
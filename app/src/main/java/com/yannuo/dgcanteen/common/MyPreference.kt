package com.yannuo.dgcanteen.common

import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import com.yannuo.dgcanteen.util.Constant

class MyPreference(cnt: Context) {

    private val preference :SharedPreferences by lazy{
        cnt.getSharedPreferences(Constant.fileName, Context.MODE_PRIVATE);
    }

    private var mEdit: SharedPreferences.Editor? = null

    init {
        mEdit = preference.edit()
    }


     fun saveStr(key: String, value: String) {
        val str = preference.getString(key, Constant.strDefault)
        if (TextUtils.isEmpty(str)) mEdit!!.putString(key, value).apply()
    }

     fun saveBoolean(key: String, value: Boolean) {
        val swc = preference.getBoolean(key, value)
        if (swc == value) mEdit!!.putBoolean(key, value).apply()
    }

     fun saveInt(key: String, value: Int) {
        val swc = preference.getInt(key, -1)
        if (swc == -1) mEdit!!.putInt(key, value).apply()
    }



     fun getStr(key: String) : String {
         return preference.getString(key, Constant.strDefault).toString()
    }

     fun getBoolean(key: String) :Boolean{
       return preference.getBoolean(key, false)
    }

     fun getInt(key: String) :Int {
        return preference.getInt(key, 0)
    }

    fun register( listener : SharedPreferences.OnSharedPreferenceChangeListener){
        preference.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregister( listener : SharedPreferences.OnSharedPreferenceChangeListener){
        preference.unregisterOnSharedPreferenceChangeListener(listener)
    }
}
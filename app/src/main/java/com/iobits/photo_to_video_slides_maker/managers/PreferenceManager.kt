package com.iobits.photo_to_video_slides_maker.managers

import android.content.Context
import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferenceManager @Inject constructor(context: Context) {
    private val mPref: SharedPreferences
    private var mEditor: SharedPreferences.Editor? = null
    private var mBulkUpdate = false

    enum class Key {
        IS_APP_ADS_FREE,
        IS_APP_PREMIUM,
        IS_SHOW_RATE_US
    }

    init {
        mPref = context.getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE)
    }

    fun put(key: Key, `val`: String?) {
        doEdit()
        mEditor!!.putString(key.name, `val`)
        doCommit()
    }

    fun put(key: Key, `val`: Int) {
        doEdit()
        mEditor!!.putInt(key.name, `val`)
        doCommit()
    }

    fun put(key: Key, `val`: Boolean) {
        doEdit()
        mEditor!!.putBoolean(key.name, `val`)
        doCommit()
    }

    fun put(key: Key, `val`: Float) {
        doEdit()
        mEditor!!.putFloat(key.name, `val`)
        doCommit()
    }

    fun put(key: Key, `val`: Double) {
        doEdit()
        mEditor!!.putString(key.name, `val`.toString())
        doCommit()
    }

    fun put(key: Key, `val`: Long) {
        doEdit()
        mEditor!!.putLong(key.name, `val`)
        doCommit()
    }

    fun getString(key: Key, defaultValue: String?): String? {
        return mPref.getString(key.name, defaultValue)
    }

    fun getString(key: Key): String? {
        return mPref.getString(key.name, null)
    }

    fun getInt(key: Key): Int {
        return mPref.getInt(key.name, 0)
    }

    fun getInt(key: Key, defaultValue: Int): Int {
        return mPref.getInt(key.name, defaultValue)
    }

    fun getLong(key: Key): Long {
        return mPref.getLong(key.name, 0)
    }

    fun getLong(key: Key, defaultValue: Long): Long {
        return mPref.getLong(key.name, defaultValue)
    }

    fun getFloat(key: Key): Float {
        return mPref.getFloat(key.name, 0f)
    }

    fun getFloat(key: Key, defaultValue: Float): Float {
        return mPref.getFloat(key.name, defaultValue)
    }

    fun getDouble(key: Key): Double {
        return getDouble(key, 0.0)
    }

    fun getDouble(key: Key, defaultValue: Double): Double {
        return try {
            java.lang.Double.valueOf(mPref.getString(key.name, defaultValue.toString()))
        } catch (nfe: NumberFormatException) {
            defaultValue
        }
    }

    fun getBoolean(key: Key, defaultValue: Boolean): Boolean {
        return mPref.getBoolean(key.name, defaultValue)
    }

    fun getBoolean(key: Key): Boolean {
        return mPref.getBoolean(key.name, false)
    }

    fun remove(vararg keys: Key) {
        doEdit()
        for (key in keys) {
            mEditor!!.remove(key.name)
        }
        doCommit()
    }

    fun clear() {
        doEdit()
        mEditor!!.clear()
        doCommit()
    }

    fun edit() {
        mBulkUpdate = true
        mEditor = mPref.edit()
    }

    fun commit() {
        mBulkUpdate = false
        mEditor!!.commit()
        mEditor = null
    }

    private fun doEdit() {
        if (!mBulkUpdate && mEditor == null) {
            mEditor = mPref.edit()
        }
    }

    private fun doCommit() {
        if (!mBulkUpdate && mEditor != null) {
            mEditor!!.commit()
            mEditor = null
        }
    }

    companion object {
        private const val SETTINGS_NAME = "default_settings"
        private var sSharedPrefs: PreferenceManager? = null
        fun getInstance(context: Context): PreferenceManager? {
            if (sSharedPrefs == null) {
                sSharedPrefs = PreferenceManager(context.applicationContext)
            }
            return sSharedPrefs
        }

        val instance: PreferenceManager?
            get() {
                if (sSharedPrefs != null) {
                    return sSharedPrefs
                }
                throw IllegalArgumentException("Should use getInstance(Context) at least once before using this method.")
            }
    }
}

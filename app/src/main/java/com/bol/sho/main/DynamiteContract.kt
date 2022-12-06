package com.bol.sho.main

import android.app.Activity

interface DynamiteContract {

    interface View {
        fun goToDynamiteWebView(url: String)
        fun goToDynamiteGame()
    }

    interface Presenter {
        suspend fun setAdb()
        suspend fun fetching(activity: Activity)
        suspend fun setUrl(url: String)
    }
}
package com.bol.sho.data

import androidx.annotation.Keep

@Keep
data class UserInfo(
    var gadid: String,
    var url: String,
    var adb: String
)
package com.bol.sho.repo

import com.bol.sho.data.Resource
import com.bol.sho.data.UserInfo

interface Repo {

    suspend fun getUserInfoData(gadid: String): Resource<UserInfo>
    suspend fun setUserInfoData(userInfo: UserInfo)
}
package com.bol.sho.repo

import com.bol.sho.retrofit.ApiFactory
import com.bol.sho.data.Resource
import com.bol.sho.data.UserInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RepoImpl: BaseRepo(), Repo {

    private var remote = ApiFactory.apiService

    override suspend fun getUserInfoData(gadid: String): Resource<UserInfo> {
        return safeApiCall { remote.getUserInfo(gadid) }
    }

    override suspend fun setUserInfoData(userInfo: UserInfo) {
        withContext(Dispatchers.IO) { remote.postUserInfo(userInfo) }
    }
}
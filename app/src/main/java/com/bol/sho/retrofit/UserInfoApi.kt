package com.bol.sho.retrofit

import com.bol.sho.data.UserInfo
import retrofit2.Response
import retrofit2.http.*

interface UserInfoApi {

    @GET("/userinfo")
    suspend fun getUserInfo(@Query("gadid") gadid: String): Response<UserInfo>

    @POST("/userinfo")
    suspend fun postUserInfo(@Body userInfo: UserInfo)
}
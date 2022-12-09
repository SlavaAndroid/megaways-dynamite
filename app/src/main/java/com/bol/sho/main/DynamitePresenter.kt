package com.bol.sho.main

import android.app.Activity
import android.provider.Settings
import androidx.core.net.toUri
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.AppsFlyerLib
import com.bol.sho.DynamiteApp
import com.bol.sho.data.UserInfo
import com.bol.sho.repo.Repo
import com.bol.sho.repo.RepoImpl
import com.bol.sho.utils.Constant
import com.facebook.applinks.AppLinkData
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.onesignal.OneSignal
import kotlinx.coroutines.*
import java.util.*

class DynamitePresenter(private val view: DynamiteContract.View) : DynamiteContract.Presenter  {

    private val repo: Repo = RepoImpl()
    private var userInfo: UserInfo? = null

    private fun getAdb(): String {
        val adb = Settings.Global.getString(
            DynamiteApp.appContext.contentResolver,
            Settings.Global.ADB_ENABLED
        )
        return if (adb == "1") "1" else "0"
    }

    override suspend fun setAdb() {
        coroutineScope {
            val adb = getAdb()
            val gadid = getGadid()
            val remoteUserInfo = repo.getUserInfoData(gadid)
            val requestUserInfo: UserInfo

            if (remoteUserInfo.data == null) {
                requestUserInfo = UserInfo(
                    gadid = gadid,
                    url = "",
                    adb = adb
                )
            } else {
                requestUserInfo = UserInfo(
                    gadid = gadid,
                    url = remoteUserInfo.data.url,
                    adb = adb
                )
            }.also { repo.setUserInfoData(requestUserInfo) }
        }
    }

    override suspend fun fetching(activity: Activity) {
        coroutineScope {
            val gadid = getGadid()
            userInfo = repo.getUserInfoData(gadid).data ?: return@coroutineScope

            if (userInfo!!.adb == "1") {
                view.goToDynamiteGame()
            } else {
                var url = userInfo!!.url
                if (url.isEmpty()) {
                    val deep = withContext(Dispatchers.IO) { getDeepLink() }
                    var apps: MutableMap<String, Any>? = null
                    when(deep) {
                        "null" -> {
                            apps =
                                withContext(Dispatchers.IO) { getAppsFlyer(activity) }
                            url = urlBuilder(apps, deep, gadid)
                        }
                        else -> {
                            url = urlBuilder(apps, deep, gadid)
                        }
                    }.also {
                        OneSignal.initWithContext(DynamiteApp.appContext)
                        OneSignal.setAppId(Constant.ONE_APP_ID)
                        OneSignal.setExternalUserId(gadid)
                        sendOneSignalTag(deep, apps)
                    }
                }
                withContext(Dispatchers.Main.immediate) {
                    view.goToDynamiteWebView(url)
                }
            }
        }
    }

    override suspend fun setUrl(url: String) {
        val gadid = getGadid()
        userInfo = repo.getUserInfoData(gadid).data ?: return
        if (userInfo!!.url.isEmpty()) {
            val requestUserInfo = UserInfo(
                gadid = userInfo!!.gadid,
                url = url,
                adb = userInfo!!.adb
            )
            repo.setUserInfoData(requestUserInfo)
        } else {
            return
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun getGadid() = withContext(Dispatchers.IO) {
        AdvertisingIdClient.getAdvertisingIdInfo(DynamiteApp.appContext).id.toString()
    }

    private suspend fun getAppsFlyer(activity: Activity): MutableMap<String, Any>? = suspendCancellableCoroutine {
        val conversionDataListener = object : AppsFlyerConversionListener {
            override fun onConversionDataSuccess(data: MutableMap<String, Any>?) {
                it.resumeWith(Result.success(data))
            }

            override fun onConversionDataFail(message: String?) {
                it.resumeWith(Result.success(null))
            }

            override fun onAppOpenAttribution(data: MutableMap<String, String>?) {}
            override fun onAttributionFailure(error: String?) {}
        }
        AppsFlyerLib.getInstance().init(Constant.APPS_DEV_KEY, conversionDataListener, activity)
        AppsFlyerLib.getInstance().start(activity)
    }

    private suspend fun getDeepLink(): String = suspendCancellableCoroutine {
        AppLinkData.fetchDeferredAppLinkData(DynamiteApp.appContext) { data ->
            it.resumeWith(Result.success(data?.targetUri.toString()))
        }
    }

    private fun urlBuilder(
        apps: MutableMap<String, Any>?,
        deep: String,
        gadid: String
    ): String {
        return Constant.START_URL.toUri().buildUpon().apply {
            appendQueryParameter(Constant.SECURE_GET_PARAMETR, Constant.SECURE_KEY)
            appendQueryParameter(Constant.DEV_TMZ_KEY, TimeZone.getDefault().id)
            appendQueryParameter(Constant.GADID_KEY, gadid)
            appendQueryParameter(Constant.DEEPLINK_KEY, deep)
            appendQueryParameter(
                Constant.SOURCE_KEY,
                if (deep != "null") "deeplink" else apps?.get("media_source").toString()
            )
            appendQueryParameter(
                Constant.AF_ID_KEY,
                when (deep) {
                    "null" -> AppsFlyerLib.getInstance().getAppsFlyerUID(DynamiteApp.appContext)
                    else -> "null"
                }
            )
            appendQueryParameter(Constant.ADSET_ID_KEY, apps?.get("adset_id").toString())
            appendQueryParameter(Constant.CAMPAIGN_ID_KEY, apps?.get("campaign_id").toString())
            appendQueryParameter(Constant.APP_CAMPAIGN_KEY, apps?.get("campaign").toString())
            appendQueryParameter(Constant.ADSET_KEY, apps?.get("adset").toString())
            appendQueryParameter(Constant.ADGROUP_KEY, apps?.get("adgroup").toString())
            appendQueryParameter(Constant.ORIG_COST_KEY, apps?.get("orig_cost").toString())
            appendQueryParameter(Constant.AF_SITEID_KEY, apps?.get("af_siteid").toString())
        }.toString()
    }

    private fun sendOneSignalTag(deep: String, afData: MutableMap<String, Any>?) {
        if (deep == "null" && afData?.get("campaign").toString() == "null") {
            OneSignal.sendTag("key2", "organic")
        }
        else if (deep != "null") {
            OneSignal.sendTag("key2", deep.replace("myapp://", "").substringBefore("/"))
        }
        else if (afData?.get("campaign").toString() != "null") {
            OneSignal.sendTag("key2", afData?.get("campaign").toString().substringBefore("_"))
        }
    }
}
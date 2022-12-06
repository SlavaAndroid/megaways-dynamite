package com.bol.sho.main

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.lifecycle.lifecycleScope
import com.bol.sho.R
import com.bol.sho.game.DynamiteStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DynamiteActivity : AppCompatActivity(), DynamiteContract.View {

    private val presenter: DynamiteContract.Presenter = DynamitePresenter(this)

    private var animator: Animator? = null

    private lateinit var dynamiteWebView: WebView
    private lateinit var callback: ValueCallback<Array<Uri?>>

    val launcher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) {
        callback.onReceiveValue(it.toTypedArray())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.megaways_dynamite_activity)

        progressBar(true)
        dynamiteWebView = findViewById(R.id.webView)

        lifecycleScope.launch {
            withContext(Dispatchers.Default) { presenter.setAdb() }
            withContext(Dispatchers.Default) { presenter.fetching(this@DynamiteActivity) }
        }
    }

    override fun goToDynamiteWebView(url: String) {

        dynamiteWebView.loadUrl(url)
        progressBar(false)
        dynamiteWebView.visibility = View.VISIBLE

        val ua = WebView(this@DynamiteActivity).settings.userAgentString.replace(" wv", "")
        dynamiteWebView.webViewClient = Client()
        dynamiteWebView.settings.javaScriptEnabled = true
        dynamiteWebView.settings.domStorageEnabled = true
        dynamiteWebView.settings.loadWithOverviewMode = false
        dynamiteWebView.settings.userAgentString = ua

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(dynamiteWebView, true)

        onBackPressedDispatcher.addCallback(this@DynamiteActivity,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (dynamiteWebView.canGoBack()) {
                        dynamiteWebView.goBack()
                    }
                }
            })

        dynamiteWebView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri?>>,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                callback = filePathCallback
                launcher.launch(IMAGE_TYPE)
                return true
            }

            override fun onCreateWindow(
                view: WebView?, isDialog: Boolean,
                isUserGesture: Boolean, resultMsg: Message
            ): Boolean {
                val newWebView = WebView(this@DynamiteActivity.applicationContext)
                newWebView.webChromeClient = this
                newWebView.settings.javaScriptEnabled = true
                newWebView.settings.javaScriptCanOpenWindowsAutomatically = true
                newWebView.settings.domStorageEnabled = true
                newWebView.settings.setSupportMultipleWindows(true)
                val transport = resultMsg.obj as WebView.WebViewTransport
                transport.webView = newWebView
                resultMsg.sendToTarget()
                return true
            }
        }
    }

    private inner class Client : WebViewClient() {
        @Deprecated("Deprecated in Java")
        override fun onReceivedError(
            view: WebView?,
            errorCode: Int,
            description: String?,
            failingUrl: String?
        ) {
            super.onReceivedError(view, errorCode, description, failingUrl)
            if (errorCode == -2) {
                Toast.makeText(this@DynamiteActivity, "Error", Toast.LENGTH_LONG).show()
            }
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            CookieManager.getInstance().flush()
            url?.let {
                if (it == "https://megawaysdynamite.buzz/") {
                    val intent = Intent(this@DynamiteActivity, DynamiteStart::class.java)
                    startActivity(intent)
                    finish()
                } else if (it.contains("404")) {
                    Log.i("ON_PAGE_FINISHED", "current url with 404: $url")
                } else if (!it.contains("megawaysdynamite.buzz")) {
                    lifecycleScope.launch {
                        presenter.setUrl(it)
                    }
                } else {
                    Log.i("ON_PAGE_FINISHED", "current url: $url")
                }
            }
        }
    }

    override fun goToDynamiteGame() {
        val i = Intent(this, DynamiteStart::class.java)
        startActivity(i)
        finish()
    }

    private fun progressBar(loading: Boolean) {
        if (loading) {
            animator = AnimatorSet().apply {
                playSequentially(
                    ObjectAnimator.ofFloat(findViewById(R.id.progress_bar), "alpha", 1f, 0f),
                    ObjectAnimator.ofFloat(findViewById(R.id.progress_bar), "alpha", 0f, 1f)
                )
                duration = 350
                start()
                doOnEnd { progressBar(loading) }
            }
        } else return
    }

    companion object {
        private const val IMAGE_TYPE = "image/*"
    }
}
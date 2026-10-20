package com.altuncontrol.app

import android.annotation.SuppressLint
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File
import java.net.URL
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private lateinit var web: WebView

    inner class Bridge {
        @JavascriptInterface
        fun wol(mac: String) {
            thread { Net.wol(mac) }
        }

        @JavascriptInterface
        fun version(): String = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: ""
        } catch (e: Exception) {
            ""
        }

        // GitHub surumundeki APK'yi indirir ve sistem kurulum ekranini acar
        @JavascriptInterface
        fun installUpdate(url: String): String {
            if (!url.startsWith("https://github.com/")) return "hata"
            if (!packageManager.canRequestPackageInstalls()) {
                startActivity(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                return "izin"
            }
            thread {
                try {
                    val f = File(cacheDir, "update.apk")
                    URL(url).openStream().use { i -> f.outputStream().use { o -> i.copyTo(o) } }
                    val uri = FileProvider.getUriForFile(this@MainActivity, "$packageName.fileprovider", f)
                    startActivity(
                        Intent(Intent.ACTION_VIEW)
                            .setDataAndType(uri, "application/vnd.android.package-archive")
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                } catch (_: Exception) {
                }
            }
            return "ok"
        }

        // Widget icin baglanti bilgisini kaydeder
        @JavascriptInterface
        fun saveConfig(host: String, host2: String, pin: String, mac: String, scenes: String) {
            getSharedPreferences("cfg", 0).edit()
                .putString("host", host).putString("host2", host2).putString("pin", pin)
                .putString("mac", mac).putString("scenes", scenes).apply()
            val m = AppWidgetManager.getInstance(this@MainActivity)
            for (id in m.getAppWidgetIds(ComponentName(this@MainActivity, PcWidget::class.java))) {
                m.updateAppWidget(id, PcWidget.build(this@MainActivity))
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Suppress("DEPRECATION")
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        web = WebView(this)
        setContentView(web)
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowUniversalAccessFromFileURLs = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
        web.setBackgroundColor(0xFF0E0E10.toInt())
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest): Boolean {
                val u = r.url
                if (u.scheme == "http" || u.scheme == "https") {
                    startActivity(Intent(Intent.ACTION_VIEW, u))
                    return true
                }
                return false
            }
        }
        web.addJavascriptInterface(Bridge(), "Android")
        load(intent)
    }

    private fun load(i: Intent?) {
        val d = i?.data
        var q = ""
        if (d != null && d.scheme == "altuncontrol") {
            q = "?host=" + Uri.encode(d.getQueryParameter("host") ?: "") +
                "&host2=" + Uri.encode(d.getQueryParameter("host2") ?: "") +
                "&pin=" + Uri.encode(d.getQueryParameter("pin") ?: "") +
                "&mac=" + Uri.encode(d.getQueryParameter("mac") ?: "")
        }
        web.loadUrl("file:///android_asset/index.html$q")
    }

    override fun onNewIntent(i: Intent) {
        super.onNewIntent(i)
        load(i)
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        moveTaskToBack(true)
    }
}

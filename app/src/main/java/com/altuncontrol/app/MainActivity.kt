package com.altuncontrol.app

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private lateinit var web: WebView

    inner class Bridge {
        // PC kapaliyken Wake-on-LAN paketini dogrudan telefon gonderir
        @JavascriptInterface
        fun wol(mac: String) {
            thread {
                try {
                    val hex = mac.replace(":", "").replace("-", "")
                    val m = ByteArray(6) { i -> hex.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
                    val pkt = ByteArray(6 + 16 * 6)
                    for (i in 0 until 6) pkt[i] = 0xFF.toByte()
                    for (i in 0 until 16) System.arraycopy(m, 0, pkt, 6 + i * 6, 6)
                    DatagramSocket().use { s ->
                        s.broadcast = true
                        for (port in intArrayOf(9, 7)) {
                            s.send(DatagramPacket(pkt, pkt.size, InetAddress.getByName("255.255.255.255"), port))
                        }
                    }
                } catch (_: Exception) {
                }
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

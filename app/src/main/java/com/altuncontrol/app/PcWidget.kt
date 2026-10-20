package com.altuncontrol.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.concurrent.thread

class PcWidget : AppWidgetProvider() {
    override fun onUpdate(c: Context, m: AppWidgetManager, ids: IntArray) {
        for (id in ids) m.updateAppWidget(id, build(c))
    }

    override fun onReceive(c: Context, i: Intent) {
        super.onReceive(c, i)
        val p = c.getSharedPreferences("cfg", 0)
        when (i.action) {
            "wake" -> {
                val pr = goAsync()
                thread {
                    Net.wol(p.getString("mac", "") ?: "")
                    toast(c, "Uyandırma paketi gönderildi")
                    pr.finish()
                }
            }
            "scene" -> {
                val name = i.getStringExtra("name") ?: ""
                val pr = goAsync()
                thread {
                    toast(c, if (run(p, name)) "$name başladı" else "PC'ye ulaşılamadı")
                    pr.finish()
                }
            }
        }
    }

    private fun run(p: android.content.SharedPreferences, name: String): Boolean {
        val pin = URLEncoder.encode(p.getString("pin", "") ?: "", "UTF-8")
        for (h in listOf(p.getString("host", ""), p.getString("host2", ""))) {
            if (h.isNullOrBlank()) continue
            try {
                val u = URL("http://$h/api/run?scene=${URLEncoder.encode(name, "UTF-8")}&pin=$pin")
                val cn = u.openConnection() as HttpURLConnection
                cn.connectTimeout = 2500
                cn.readTimeout = 5000
                if (cn.responseCode == 200) return true
            } catch (_: Exception) {
            }
        }
        return false
    }

    private fun toast(c: Context, t: String) {
        Handler(Looper.getMainLooper()).post { Toast.makeText(c, t, Toast.LENGTH_SHORT).show() }
    }

    companion object {
        private fun pi(c: Context, code: Int, act: String, name: String) = PendingIntent.getBroadcast(
            c, code, Intent(c, PcWidget::class.java).setAction(act).putExtra("name", name),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        fun build(c: Context): RemoteViews {
            val v = RemoteViews(c.packageName, R.layout.widget)
            val sc = (c.getSharedPreferences("cfg", 0).getString("scenes", "") ?: "").split("|").filter { it.isNotBlank() }
            v.setOnClickPendingIntent(R.id.w_wake, pi(c, 0, "wake", ""))
            val ids = intArrayOf(R.id.w_s1, R.id.w_s2)
            for ((i, id) in ids.withIndex()) {
                if (i < sc.size) {
                    v.setTextViewText(id, sc[i])
                    v.setViewVisibility(id, View.VISIBLE)
                    v.setOnClickPendingIntent(id, pi(c, i + 1, "scene", sc[i]))
                } else {
                    v.setViewVisibility(id, View.GONE)
                }
            }
            return v
        }
    }
}

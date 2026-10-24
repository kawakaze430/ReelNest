package com.reelnest.catalogpreview

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import java.util.concurrent.Executors
import org.json.JSONObject

/**
 * Real read-only Android/Android TV catalog UI.
 * No JWT storage, player URL, account pairing or DRM implementation.
 */
class CatalogActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var layout: LinearLayout
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30,40,30,40)
        }
        val scroller=ScrollView(this).apply { addView(layout) }
        setContentView(scroller)
        val heading=TextView(this).apply { text="ReelNest • Public catalog";textSize=28f }
        layout.addView(heading)
        status=TextView(this).apply {
            text="Checking the public catalog...";textSize=17f
        }
        layout.addView(status)
        val origin=BuildConfig.API_ORIGIN
        if(origin=="https://api.example.org") {
            status.text="Configure an authorized HTTPS API origin at build time. Playback is disabled."
            return
        }
        executor.execute {
            var message:String?=null
            val titles=mutableListOf<Triple<String,String,String>>()
            try {
                val url=URL(origin+"/api/v1/catalog?page=0&size=24")
                val conn=url.openConnection() as HttpsURLConnection
                conn.connectTimeout=8000;conn.readTimeout=8000
                conn.requestMethod="GET";conn.instanceFollowRedirects=false
                conn.setRequestProperty("Accept","application/json")
                try {
                    if(conn.responseCode!=200) throw IllegalStateException("Catalog unavailable")
                    val body=conn.inputStream.bufferedReader().use { it.readText() }
                    val entries=JSONObject(body).getJSONArray("content")
                    for(i in 0 until minOf(entries.length(),24)) {
                        val item=entries.getJSONObject(i)
                        val label=item.optString("name").take(255)
                        if(label.isNotBlank())titles.add(Triple(label,
                            item.optString("synopsis").take(500),item.optString("genre").take(80)))
                    }
                }finally{conn.disconnect()}
            }catch(_:Exception){message="Catalog unavailable. Verify network and API origin."}
            runOnUiThread {
                if(isFinishing||isDestroyed)return@runOnUiThread
                status.text=message?:if(titles.isEmpty())"No published titles." else
                    "Browse titles using touch or your TV remote. Protected video is unavailable."
                titles.forEach { (name,synopsis,genre) ->
                    val card=Button(this).apply {
                        text="$name · $genre";textSize=19f
                        isFocusable=true
                        setOnClickListener {
                            AlertDialog.Builder(this@CatalogActivity)
                                .setTitle(name).setMessage(synopsis+
                                    "\n\nPlayback requires device OIDC, DRM and current entitlements.")
                                .setPositiveButton("Close",null).show()
                        }
                    }
                    layout.addView(card,ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT))
                }
            }
        }
    }
    override fun onDestroy(){executor.shutdownNow();super.onDestroy()}
}

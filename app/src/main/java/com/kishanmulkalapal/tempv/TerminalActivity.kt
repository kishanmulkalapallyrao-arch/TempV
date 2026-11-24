package com.kishanmulkalapal.tempv

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class TerminalActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var keysRow: LinearLayout
    private var session: TerminalSession? = null
    private var rows = 24
    private var cols = 80
    private var ctrlActive = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        keysRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xFF1A1A1A.toInt())
        }

        webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.allowFileAccess = true
        webView.setBackgroundColor(0xFF000000.toInt())
        webView.addJavascriptInterface(JsBridge(), "AndroidTerm")
        webView.loadUrl("file:///android_asset/terminal/index.html")

        root.addView(keysRow, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(40)))
        root.addView(webView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        setContentView(root)
        buildKeys()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun buildKeys() {
        val keys = listOf(
            "ESC" to "\u001b", "CTRL" to "", "TAB" to "\t",
            "←" to "\u001b[D", "↓" to "\u001b[B",
            "↑" to "\u001b[A", "→" to "\u001b[C"
        )
        for ((label, seq) in keys) {
            val b = Button(this).apply {
                text = label
                textSize = 12f
                setTextColor(0xFFE0E0E0.toInt())
                setBackgroundColor(0xFF262626.toInt())
                setPadding(0, 0, 0, 0)
                setOnClickListener {
                    if (label == "CTRL") {
                        ctrlActive = !ctrlActive
                        alpha = if (ctrlActive) 1f else 0.5f
                    } else session?.write(seq)
                }
            }
            keysRow.addView(b, LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
        }
    }

    private fun startSession() {
        if (session != null) return
        val home = filesDir.resolve("home").apply { mkdirs() }
        session = TerminalSession(
            shellPath = "/system/bin/sh",
            workDir = home.absolutePath,
            onOutput = { bytes ->
                val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                runOnUiThread {
                    webView.evaluateJavascript("window.writeOutput('$b64')", null)
                }
            },
            onExit = { runOnUiThread { finish() } }
        )
        session?.start(rows, cols)
    }

    inner class JsBridge {
        @JavascriptInterface
        fun onInput(data: String) {
            val out = if (ctrlActive && data.length == 1) {
                ctrlActive = false
                runOnUiThread { keysRow.getChildAt(1).alpha = 0.5f }
                val c = data[0].uppercaseChar()
                val code = c.code - 'A'.code + 1
                if (code in 1..26) code.toChar().toString() else data
            } else data
            session?.write(out)
        }

        @JavascriptInterface
        fun onResize(r: Int, c: Int) {
            rows = r; cols = c
            session?.resize(r, c)
        }

        @JavascriptInterface
        fun ready() { runOnUiThread { startSession() } }
    }

    override fun onDestroy() {
        session?.stop()
        super.onDestroy()
    }
}

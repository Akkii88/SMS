package com.example.sms

import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.widget.ScrollView
import android.widget.TextView
import kotlin.random.Random

class LogcatSimulator(private val logTextView: TextView, private val scrollView: ScrollView) {

    private val handler = Handler(Looper.getMainLooper())
    private var isRedMode = false
    private var logRunnable: Runnable? = null

    fun startNormalLogs() {
        isRedMode = false
        startLogging()
    }

    fun startRedMode() {
        if (!isRedMode) {
            isRedMode = true
            startLogging()
        }
    }

    fun stopRedMode() {
        isRedMode = false
        logTextView.setTextColor(Color.GREEN)
        logTextView.append("\n✅ Access granted. Secure connection established...\n")
    }

    private fun startLogging() {
        logRunnable?.let { handler.removeCallbacks(it) }

        logRunnable = object : Runnable {
            override fun run() {
                val newLine = if (isRedMode) getCrazyRedLog() else getNormalLog()
                logTextView.append("$newLine\n")
                scrollToBottom()
                handler.postDelayed(this, 300)
            }
        }

        logRunnable?.let { handler.post(it) }
    }

    private fun scrollToBottom() {
        scrollView.post {
            scrollView.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }

    private fun getNormalLog(): String {
        val logs = listOf(
            ">> AUTH handshake successful",
            "[SYS] Background service running",
            "📡 ping @ agent.server [OK]",
            "User status: ACTIVE",
            "[SYNC] Data sync initiated...",
            "[✓] Heartbeat response received",
            ">> Secure socket opened...",
            "Packet header validated"
        )
        return logs.random()
    }

    private fun getCrazyRedLog(): String {
        logTextView.setTextColor(Color.RED)
        val warnings = listOf(
            "❌ UNAUTHORIZED ACCESS DETECTED",
            "⚠️ LOCATION TRACE INITIATED...",
            "🚨 BREACH ALERT - CODE RED",
            "!!! SYSTEM INTRUSION WARNING !!!",
            "🔴 YOUR LOCATION IS COMPROMISED",
            "🧠 BRAINWAVE INTERCEPTED!",
            "👀 WE SEE YOU NOW...",
            "💣 INITIATING PROTOCOL-66",
            "🌐 Global surveillance triggered..."
        )
        return warnings.random()
    }
}

package com.example.sms

import android.content.Intent
import android.graphics.Color
import android.os.*
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var editTextUniqueID: EditText
    private lateinit var buttonJoinChat: Button
    private lateinit var textViewWelcome: TextView
    private lateinit var textViewLogcat: TextView
    private lateinit var scrollViewLogcat: ScrollView

    private val db = FirebaseFirestore.getInstance()
    private val handler = Handler(Looper.getMainLooper())

    private var backPressedTime: Long = 0
    private var logRunnable: Runnable? = null
    private var logRunning = true
    private var logDelay: Long = 500L // Initial normal delay
    private var errorLogMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Hide status/navigation bars for immersive effect
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        supportActionBar?.hide()

        // Initialize views
        textViewWelcome = findViewById(R.id.textViewWelcome)
        editTextUniqueID = findViewById(R.id.editTextUniqueID)
        buttonJoinChat = findViewById(R.id.buttonJoinChat)
        textViewLogcat = findViewById(R.id.textViewLogcat)
        scrollViewLogcat = findViewById(R.id.logScrollView)

        animateWelcomeText()
        startFakeLogcat()

        buttonJoinChat.setOnClickListener {
            val chatID = editTextUniqueID.text.toString().trim()

            if (chatID.length < 4) {
                Toast.makeText(this, "Access code must be at least 4 characters!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(editTextUniqueID.windowToken, 0)

            validateUniqueID(chatID)
        }
    }

    private fun animateWelcomeText() {
        val text = "Welcome, Agent..."
        textViewWelcome.text = ""
        var index = 0

        val typeWriter = object : Runnable {
            override fun run() {
                if (index <= text.length) {
                    textViewWelcome.text = text.substring(0, index)
                    index++
                    handler.postDelayed(this, 100)
                }
            }
        }
        handler.post(typeWriter)
    }

    private fun validateUniqueID(chatID: String) {
        db.collection("chatIDs").document(chatID).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Valid ID
                    logRunning = false
                    errorLogMode = false
                    val intent = Intent(this, ChatActivity::class.java)
                    intent.putExtra("CHAT_ID", chatID)
                    startActivity(intent)
                } else {
                    // Invalid ID
                    errorLogMode = true
                    logDelay = 50L
                    appendLog("ALERT: Invalid Access Code [$chatID]!", Color.RED)
                    appendLog("SECURITY BREACH DETECTED", Color.RED)
                    appendLog("WARNING: Your location is being traced...", Color.RED)
                    appendLog("⚠️ SYSTEM ALERT: Immediate validation required", Color.RED)
                    accelerateLogSpeed()
                }
            }
            .addOnFailureListener {
                appendLog("ERROR: Cannot connect to server", Color.RED)
                Toast.makeText(this, "Error checking access code", Toast.LENGTH_SHORT).show()
            }
    }

    private fun startFakeLogcat() {
        logRunnable = object : Runnable {
            override fun run() {
                if (logRunning) {
                    val log = if (errorLogMode) getRandomErrorLine() else getRandomLogLine()
                    val color = if (errorLogMode) Color.RED else getRandomColor()
                    appendLog(log, color)
                    handler.postDelayed(this, logDelay)
                }
            }
        }
        handler.post(logRunnable!!)
    }

    private fun accelerateLogSpeed() {
        handler.post(object : Runnable {
            override fun run() {
                if (errorLogMode && logDelay > 5L) {
                    logDelay -= 5L
                    handler.postDelayed(this, 300)
                }
            }
        })
    }

    private fun appendLog(text: String, color: Int) {
        val spannable = SpannableStringBuilder()
        spannable.append(text).setSpan(
            ForegroundColorSpan(color),
            0,
            text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        textViewLogcat.append(spannable)
        textViewLogcat.append("\n")

        scrollViewLogcat.post {
            scrollViewLogcat.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun getRandomLogLine(): String {
        val prefixes = listOf("[INFO]", "[DEBUG]", "[SYSTEM]", "[NET]", "[AUTH]", "[FIREBASE]")
        val suffixes = listOf(
            "System modules loading...",
            "Syncing Firebase data...",
            "Listening on port 8888",
            "Monitoring input stream...",
            "Session active, waiting for user...",
            "All systems operational."
        )
        return "${prefixes.random()} ${suffixes.random()}"
    }

    private fun getRandomErrorLine(): String {
        val messages = listOf(
            "[SECURITY] Unauthorized access attempt detected.",
            "[ALERT] IP logging initiated.",
            "[ADMIN] This activity is being monitored.",
            "[FIREWALL] Multiple invalid attempts blocked.",
            "[TRACE] User location triangulated.",
            "[WARNING] Chatroom breach alert!",
            "[AGENT] Validate identity immediately.",
            "[SERVER] Suspicious activity from client side.",
            "[ERROR] Invalid credentials detected."
        )
        return messages.random()
    }

    private fun getRandomColor(): Int {
        val colors = listOf(Color.GREEN, Color.YELLOW, Color.CYAN, Color.LTGRAY)
        return colors.random()
    }

    override fun onBackPressed() {
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            super.onBackPressed()
        } else {
            Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show()
            backPressedTime = System.currentTimeMillis()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        logRunning = false
    }
}

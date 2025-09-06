package com.example.sms

import ChatAdapter
import ChatMessage
import MessageSpacingDecoration
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaRecorder
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.provider.Settings
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class ChatActivity : AppCompatActivity() {

    private lateinit var recyclerViewChat: RecyclerView
    private lateinit var editTextMessage: EditText
    private lateinit var buttonSend: Button
    private lateinit var buttonRecord: ImageButton
    private lateinit var buttonImage: ImageButton
    private lateinit var textViewChatID: TextView
    private lateinit var statusText: TextView
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var chatID: String

    private val db = FirebaseFirestore.getInstance()
    private var recorder: MediaRecorder? = null
    private var audioFilePath: String? = null
    private lateinit var currentImagePath: String

    private val handler = Handler(Looper.getMainLooper())
    private val random = kotlin.random.Random(System.currentTimeMillis())

    private val deviceId: String by lazy {
        Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        recyclerViewChat = findViewById(R.id.recyclerViewChat)
        editTextMessage = findViewById(R.id.editTextMessage)
        buttonSend = findViewById(R.id.buttonSend)
        buttonRecord = findViewById(R.id.buttonRecord)
        buttonImage = findViewById(R.id.buttonExtra)
        textViewChatID = findViewById(R.id.textViewChatID)
        statusText = findViewById(R.id.statusText)

        chatID = intent.getStringExtra("CHAT_ID") ?: ""
        textViewChatID.text = "Chat ID: ${maskChatID(chatID)}"

        chatAdapter = ChatAdapter(deviceId)
        recyclerViewChat.layoutManager = LinearLayoutManager(this)
        recyclerViewChat.adapter = chatAdapter
        recyclerViewChat.addItemDecoration(MessageSpacingDecoration((4 * resources.displayMetrics.density).toInt()))

        buttonSend.setOnClickListener { sendMessage() }
        buttonRecord.setOnClickListener { checkMicPermissionAndRecord() }
        buttonImage.setOnClickListener { checkCameraPermissionAndCapture() }

        loadMessages()
        startRotatingStatus()
    }

    private fun maskChatID(chatID: String): String {
        return if (chatID.length >= 8) {
            "${chatID.take(3)}****${chatID.takeLast(2)}"
        } else "*".repeat(chatID.length)
    }

    private fun sendMessage() {
        val messageText = editTextMessage.text.toString().trim()
        if (messageText.isEmpty()) return

        val message = hashMapOf(
            "text" to messageText,
            "timestamp" to System.currentTimeMillis(),
            "senderId" to deviceId
        )

        db.collection("chats").document(chatID)
            .collection("messages").add(message)
            .addOnSuccessListener {
                editTextMessage.text.clear()
                hideKeyboard()
                scrollToBottom()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to send", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadMessages() {
        db.collection("chats").document(chatID)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Toast.makeText(this, "Error loading messages", Toast.LENGTH_SHORT).show()
                    Log.e("Firestore", "Snapshot error", error)
                    return@addSnapshotListener
                }

                val messages = snapshots?.toObjects(ChatMessage::class.java)
                messages?.let {
                    chatAdapter.submitList(it.toList()) // Avoid mutation issues
                    scrollToBottom()
                }
            }
    }

    private fun scrollToBottom() {
        recyclerViewChat.post {
            recyclerViewChat.scrollToPosition(chatAdapter.itemCount - 1)
        }
    }

    private fun hideKeyboard() {
        val view = currentFocus
        if (view != null) {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    // ===== VOICE RECORDING =====

    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) showRecordingDialog()
        else Toast.makeText(this, "Microphone permission denied", Toast.LENGTH_SHORT).show()
    }

    private fun checkMicPermissionAndRecord() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            showRecordingDialog()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun showRecordingDialog(): AlertDialog {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Recording")
            .setMessage("Recording voice message...")
            .setCancelable(false)
            .setPositiveButton("Stop") { d, _ ->
                stopRecording()
                askToSendVoice()
                d.dismiss()
            }
            .setNegativeButton("Cancel") { d, _ ->
                stopRecording()
                d.dismiss()
            }
            .create()

        startRecording()
        dialog.show()
        return dialog
    }

    private fun startRecording() {
        try {
            val outputFile = File.createTempFile("voice_", ".3gp", externalCacheDir ?: cacheDir)
            audioFilePath = outputFile.absolutePath

            recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setOutputFile(audioFilePath)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                prepare()
                start()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Recording failed: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("Recorder", "Start error", e)
        }
    }

    private fun stopRecording() {
        try {
            recorder?.stop()
        } catch (e: Exception) {
            Log.e("Recorder", "Stop error", e)
        } finally {
            recorder?.release()
            recorder = null
        }
    }

    private fun askToSendVoice() {
        AlertDialog.Builder(this)
            .setTitle("Send Voice Message?")
            .setPositiveButton("Send") { _, _ -> encodeAndSendVoice() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun encodeAndSendVoice() {
        try {
            val file = File(audioFilePath ?: return)
            val base64Audio = Base64.encodeToString(file.readBytes(), Base64.DEFAULT)

            val voiceMessage = hashMapOf(
                "audioBase64" to base64Audio,
                "timestamp" to System.currentTimeMillis(),
                "senderId" to deviceId
            )

            db.collection("chats").document(chatID)
                .collection("messages").add(voiceMessage)
                .addOnSuccessListener {
                    Snackbar.make(recyclerViewChat, "Voice message sent", Snackbar.LENGTH_SHORT).show()
                    scrollToBottom()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to send voice message", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Toast.makeText(this, "Encoding failed: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("Base64", "Voice encode error", e)
        }
    }

    // ===== IMAGE CAPTURE =====

    private val imageCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, Uri.fromFile(File(currentImagePath)))
            encodeAndSendImage(bitmap)
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera()
        else Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
    }

    private fun checkCameraPermissionAndCapture() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            launchCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        val photoFile = createImageFile()
        currentImagePath = photoFile.absolutePath
        val photoURI: Uri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.provider",
            photoFile
        )
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
        }
        imageCaptureLauncher.launch(intent)
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File.createTempFile("IMG_$timeStamp", ".jpg", cacheDir)
    }

    private fun encodeAndSendImage(bitmap: Bitmap) {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, stream)
        val base64Image = Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT)

        val imageMessage = hashMapOf(
            "imageUrl" to base64Image,  // Important: This should match ChatAdapter field
            "timestamp" to System.currentTimeMillis(),
            "senderId" to deviceId
        )

        db.collection("chats").document(chatID)
            .collection("messages").add(imageMessage)
            .addOnSuccessListener {
                Snackbar.make(recyclerViewChat, "Image sent", Snackbar.LENGTH_SHORT).show()
                scrollToBottom()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to send image", Toast.LENGTH_SHORT).show()
            }
    }

    // ===== STATUS =====

    private fun startRotatingStatus() {
        val nodes = listOf("Berlin", "Tokyo", "Amsterdam", "Toronto", "Seoul", "Helsinki", "Singapore")
        var currentNode = ""

        val updateStatus = object : Runnable {
            override fun run() {
                val node = nodes.random()
                val latency = random.nextInt(10, 160)

                val fadeOut = AlphaAnimation(1f, 0f).apply { duration = 300; fillAfter = true }
                val fadeIn = AlphaAnimation(0f, 1f).apply { duration = 300; startOffset = 300; fillAfter = true }

                fadeOut.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                    override fun onAnimationStart(animation: android.view.animation.Animation) {}
                    override fun onAnimationRepeat(animation: android.view.animation.Animation) {}
                    override fun onAnimationEnd(animation: android.view.animation.Animation) {
                        val status = "Connected to Node: $node | Latency: ${latency}ms"
                        val spannable = SpannableString(status)

                        val green = ContextCompat.getColor(this@ChatActivity, R.color.green_hacker)
                        val yellow = ContextCompat.getColor(this@ChatActivity, R.color.yellow)
                        val red = ContextCompat.getColor(this@ChatActivity, R.color.soft_red)

                        val nodeStart = status.indexOf("Node: ") + 6
                        val nodeEnd = status.indexOf(" |")
                        if (nodeStart in 0 until nodeEnd && nodeEnd <= status.length) {
                            spannable.setSpan(
                                ForegroundColorSpan(green),
                                nodeStart, nodeEnd,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        }

                        val latencyStart = status.indexOf("Latency:") + 9
                        val latencyEnd = status.indexOf("ms") + 2
                        if (latencyStart in 0 until latencyEnd && latencyEnd <= status.length) {
                            val color = when {
                                latency <= 50 -> green
                                latency <= 100 -> yellow
                                else -> red
                            }

                            spannable.setSpan(
                                ForegroundColorSpan(color),
                                latencyStart, latencyEnd,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        }

                        if (node != currentNode) {
                            val blinkAnim = AlphaAnimation(0.1f, 1f).apply {
                                duration = 100
                                repeatMode = AlphaAnimation.REVERSE
                                repeatCount = 1
                            }
                            statusText.startAnimation(blinkAnim)
                        }

                        statusText.text = spannable
                        statusText.startAnimation(fadeIn)
                        currentNode = node
                    }
                })

                statusText.startAnimation(fadeOut)
                handler.postDelayed(this, 5000)
            }
        }

        handler.post(updateStatus)
    }
}

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.MediaPlayer
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.sms.R
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(private val deviceId: String) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    private var messages: List<ChatMessage> = emptyList()
    private var mediaPlayer: MediaPlayer? = null
    private var currentlyPlayingPosition: Int = -1
    private var pausedPosition: Int = -1

    inner class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textMessage: TextView = view.findViewById(R.id.textViewMessage)
        val timestamp: TextView = view.findViewById(R.id.textViewTimestamp)
        val imageMessage: ImageView = view.findViewById(R.id.imageMessage)
        val playHint: TextView = view.findViewById(R.id.playHint)
        val voiceLayout: LinearLayout = view.findViewById(R.id.voiceMessageLayout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.activity_chat_message, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val message = messages[position]
        val context = holder.itemView.context

        holder.textMessage.isVisible = false
        holder.imageMessage.isVisible = false
        holder.voiceLayout.isVisible = false
        holder.playHint.isVisible = false
        holder.timestamp.isVisible = false // Hide the old timestamp TextView

        // TEXT MESSAGE
        if (!message.text.isNullOrEmpty()) {
            val fullText = "${message.text}  ${timestampFormatted(message.timestamp)}"
            val spannable = SpannableString(fullText)
            val start = fullText.indexOf(timestampFormatted(message.timestamp))

            spannable.setSpan(RelativeSizeSpan(0.8f), start, fullText.length, 0)
            spannable.setSpan(ForegroundColorSpan(Color.GRAY), start, fullText.length, 0)

            holder.textMessage.text = spannable
            holder.textMessage.isVisible = true

            // Set text color based on sender
            val color = if (message.senderId == deviceId) {
                context.getColor(R.color.green_hacker)
            } else {
                context.getColor(R.color.soft_red)
            }
            holder.textMessage.setTextColor(color)
        }

        // IMAGE MESSAGE
        if (!message.imageBase64.isNullOrEmpty()) {
            val decodedBytes = Base64.decode(message.imageBase64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            holder.imageMessage.setImageBitmap(bitmap)
            holder.imageMessage.isVisible = true
        }

        // VOICE MESSAGE
        if (!message.audioBase64.isNullOrEmpty()) {
            holder.voiceLayout.isVisible = true
            holder.playHint.isVisible = true

            holder.playHint.text = when {
                position == currentlyPlayingPosition -> "Playing..."
                position == pausedPosition -> "Paused"
                else -> "Tap to Play"
            }

            holder.voiceLayout.setOnClickListener {
                if (position == currentlyPlayingPosition) {
                    mediaPlayer?.pause()
                    pausedPosition = position
                    currentlyPlayingPosition = -1
                    notifyItemChanged(position)
                } else {
                    if (currentlyPlayingPosition != -1) {
                        mediaPlayer?.stop()
                        notifyItemChanged(currentlyPlayingPosition)
                    }

                    val audioFile = File.createTempFile("temp_audio", ".3gp", context.cacheDir)
                    val bytes = Base64.decode(message.audioBase64, Base64.DEFAULT)
                    FileOutputStream(audioFile).use { it.write(bytes) }

                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(audioFile.absolutePath)
                        prepare()
                        start()
                        setOnCompletionListener {
                            currentlyPlayingPosition = -1
                            notifyItemChanged(position)
                        }
                    }

                    currentlyPlayingPosition = position
                    pausedPosition = -1
                    notifyItemChanged(position)
                }
            }
        }
    }

    override fun getItemCount(): Int = messages.size

    fun submitList(newMessages: List<ChatMessage>?) {
        messages = newMessages ?: emptyList()
        notifyDataSetChanged()
    }

    private fun timestampFormatted(timestamp: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}

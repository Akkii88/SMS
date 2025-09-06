data class ChatMessage(
    val text: String? = null,
    val audioBase64: String? = null,
    val imageBase64: String? = null, // Added for image base64
    val timestamp: Long = System.currentTimeMillis(),
    val senderId: String? = null
)

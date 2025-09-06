package com.example.sms

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private val disclaimerText = """
        This app is created purely for fun and entertainment purposes. It is not intended for serious use, government missions, or summoning extraterrestrial life forms. We know someone will try to hack NASA or contact aliens using it, but please don’t. We already tried and, spoiler alert, it didn’t work.

        Any resemblance to real-world intelligence agencies, secret societies, or ancient prophecy scrolls is purely coincidental. The app was made during a highly caffeinated evening and probably should not be trusted for anything beyond having a good time and possibly confusing your friends in harmless ways.

        We are not responsible for any strange looks you get while using this app, any laughter it may cause in public, or any questionable decisions made under the influence of boredom. If your cat starts replying to messages or your microwave begins speaking back, please consult a professional, not us. We just make fun apps.

        By using this app, you agree that it’s all in good humor. You won’t use it to break the law, start an uprising, or annoy people to the point of being banned from family gatherings. This app does not come with a user manual, a license to be annoying, or the ability to launch rockets. Please enjoy it as it was meant to be — a harmless little distraction in a world full of seriousness.

        Thanks for downloading. We hope it makes your day a little more interesting, your conversations a little weirder, and your life just a bit more fun.
    """.trimIndent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val disclaimerView = findViewById<TextView>(R.id.disclaimerText)
        disclaimerView.text = disclaimerText

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 1000) // Show for 6 seconds (adjust if needed)
    }
}

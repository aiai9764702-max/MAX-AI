package com.maxai.assistant

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Button
import android.graphics.Color
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private lateinit var status: TextView
    private lateinit var voiceButton: Button

    private val requestCode = 101
    private val permissionCode = 102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this, this)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER
        layout.setPadding(24, 24, 24, 24)
        layout.setBackgroundColor(Color.rgb(5, 10, 25))

        val title = TextView(this)
        title.text = "MAX AI"
        title.textSize = 38f
        title.setTextColor(Color.CYAN)
        title.gravity = Gravity.CENTER

        val subtitle = TextView(this)
        subtitle.text = "YOUR PERSONAL AI ASSISTANT"
        subtitle.textSize = 16f
        subtitle.setTextColor(Color.WHITE)
        subtitle.gravity = Gravity.CENTER

        status = TextView(this)
        status.text = "\nMAX VOICE SYSTEM\n\nREADY TO LISTEN"
        status.textSize = 18f
        status.setTextColor(Color.GREEN)
        status.gravity = Gravity.CENTER

        voiceButton = Button(this)
        voiceButton.text = "ACTIVATE MAX"

        voiceButton.setOnClickListener {
            if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
                startListening()
            } else {
                requestPermissions(
                    arrayOf(android.Manifest.permission.RECORD_AUDIO),
                    permissionCode
                )
            }
        }

        layout.addView(title)
        layout.addView(subtitle)
        layout.addView(status)
        layout.addView(voiceButton)

        setContentView(layout)
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE,
            "hi-IN"
        )
        intent.putExtra(
            RecognizerIntent.EXTRA_PROMPT,
            "Speak to MAX"
        )

        try {
            status.text = "MAX IS LISTENING..."
            startActivityForResult(intent, requestCode)
        } catch (e: Exception) {
            status.text = "Voice recognition unavailable"
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == this.requestCode &&
            resultCode == RESULT_OK) {

            val results = data?.getStringArrayListExtra(
                RecognizerIntent.EXTRA_RESULTS
            )

            val spokenText = results?.firstOrNull()

            if (spokenText != null) {
                status.text = "YOU SAID:\n\n$spokenText"
                tts?.speak(
                    "आपने कहा $spokenText",
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "MAX_RESPONSE"
                )
            } else {
                status.text = "No voice detected. Try again."
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == permissionCode &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startListening()
        } else if (requestCode == permissionCode) {
            status.text = "Microphone permission required"
        }
    }

    override fun onInit(statusCode: Int) {
        if (statusCode == TextToSpeech.SUCCESS) {
            tts?.language = Locale("hi", "IN")
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

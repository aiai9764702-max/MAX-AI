
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
import android.util.Log

import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeBackend
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private lateinit var status: TextView
    private lateinit var voiceButton: Button

    private val requestCode = 101
    private val permissionCode = 102

    private val activityScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val model by lazy {
        Firebase.ai(
            backend = GenerativeBackend.googleAI()
        ).generativeModel("gemini-3.8-flash")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            Firebase.initialize(context = this)

            Firebase.appCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        } catch (e: Exception) {
            Log.e("MAX_AI", "Firebase setup error", e)
        }

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
        status.text = "\nMAX AI SYSTEM\n\nREADY TO LISTEN"
        status.textSize = 18f
        status.setTextColor(Color.GREEN)
        status.gravity = Gravity.CENTER

        voiceButton = Button(this)
        voiceButton.text = "ACTIVATE MAX"

        voiceButton.setOnClickListener {
            if (
                checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED
            ) {
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

        if (
            requestCode == this.requestCode &&
            resultCode == RESULT_OK
        ) {
            val results = data?.getStringArrayListExtra(
                RecognizerIntent.EXTRA_RESULTS
            )

            val spokenText = results?.firstOrNull()

            if (!spokenText.isNullOrBlank()) {
                askGemini(spokenText)
            } else {
                status.text = "No voice detected. Try again."
            }
        }
    }

    private fun askGemini(question: String) {
        voiceButton.isEnabled = false
        status.text = "MAX IS THINKING..."

        activityScope.launch {
            try {
                val prompt = """
                    You are MAX, a helpful Android AI assistant.
                    Reply naturally and clearly.
                    Reply in the same language as the user's question.
                    You can use Hindi, English, or Hinglish.
                    Keep answers concise unless more detail is requested.
                    Do not claim to perform phone actions that you have not performed.

                    User: $question
                """.trimIndent()

                val response = withContext(Dispatchers.IO) {
                    model.generateContent(prompt)
                }

                val answer = response.text
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?: "मुझे जवाब नहीं मिल पाया। दोबारा पूछिए।"

                status.text = "MAX SAYS:\n\n$answer"

                tts?.speak(
                    answer,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "MAX_RESPONSE"
                )

            } catch (e: Exception) {
                Log.e("MAX_AI", "Gemini error", e)

                status.text =
                    "AI CONNECTION FAILED\nCheck internet and Firebase setup."

                tts?.speak(
                    "माफ कीजिए, अभी AI से कनेक्शन नहीं हो पाया।",
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "MAX_ERROR"
                )
            } finally {
                voiceButton.isEnabled = true
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

        if (
            requestCode == permissionCode &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
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
        activityScope.cancel()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}


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
import com.google.firebase.initialize
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.appcheck.appCheck
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
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val model by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel("gemini-3.8-flash")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Firebase.initialize(context = this)
        Firebase.appCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )
        tts = TextToSpeech(this, this)
        createInterface()
    }

    private fun createInterface() {
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
        status.text = "MAX SYSTEM READY"
        status.textSize = 18f
        status.setTextColor(Color.GREEN)
        status.gravity = Gravity.CENTER
        voiceButton = Button(this)
        voiceButton.text = "ACTIVATE MAX"
        voiceButton.setOnClickListener { requestMicrophone() }
        layout.addView(title)
        layout.addView(subtitle)
        layout.addView(status)
        layout.addView(voiceButton)
        setContentView(layout)
    }

    private fun requestMicrophone() {
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startListening()
        } else {
            requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), permissionCode)
        }
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, " बोलिए, MAX सुन रहा है")
        try {
            startActivityForResult(intent, requestCode)
            status.text = "LISTENING..."
        } catch (e: Exception) {
            status.text = "VOICE INPUT NOT AVAILABLE"
            Log.e("MAX", "Speech recognition error", e)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == this.requestCode && resultCode == RESULT_OK) {
            val spokenText = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                status.text = "YOU: $spokenText"
                askGemini(spokenText)
            }
        }
    }

    override fun onInit(statusCode: Int) {
        if (statusCode == TextToSpeech.SUCCESS) {
            tts?.language = Locale("hi", "IN")
            status.text = "MAX VOICE READY"
        } else {
            status.text = "VOICE ENGINE ERROR"
        }
    }
    private fun speak(message: String) {
        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "MAX_REPLY")
    }

    private fun askGemini(prompt: String) {
        status.text = "MAX IS THINKING..."
        activityScope.launch {
            try {
                val response = model.generateContent(prompt)
                val answer = response.text ?: "Sorry, no response received."
                status.text = "MAX: $answer"
                speak(answer)
            } catch (e: Exception) {
                status.text = "ERROR: " + e.javaClass.simpleName + ": " + (e.message ?: "No details")
                Log.e("MAX", "Gemini request failed", e)
            }
        }
    }
}

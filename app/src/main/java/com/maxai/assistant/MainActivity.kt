package com.maxai.assistant

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Button

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER
        layout.setPadding(24, 24, 24, 24)
        layout.setBackgroundColor(Color.rgb(5, 10, 25))

        val title = TextView(this)
        title.text = "MAX AI"
        title.textSize = 36f
        title.setTextColor(Color.CYAN)
        title.gravity = Gravity.CENTER

        val subtitle = TextView(this)
        subtitle.text = "YOUR PERSONAL AI ASSISTANT"
        subtitle.textSize = 14f
        subtitle.setTextColor(Color.WHITE)
        subtitle.gravity = Gravity.CENTER

        val status = TextView(this)
        status.text = "\nSYSTEM STATUS: ONLINE\n\nAI BRAIN: GEMINI\n\nVOICE SYSTEM: READY"
        status.textSize = 16f
        status.setTextColor(Color.GREEN)
        status.gravity = Gravity.CENTER

        val voiceButton = Button(this)
        voiceButton.text = "ACTIVATE MAX"
        voiceButton.setOnClickListener {
            status.text = "MAX AI\n\nVoice feature will be added soon."
        }

        layout.addView(title)
        layout.addView(subtitle)
        layout.addView(status)
        layout.addView(voiceButton)

        setContentView(layout)
    }
}

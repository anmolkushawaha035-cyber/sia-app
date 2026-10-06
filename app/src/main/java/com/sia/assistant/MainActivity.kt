package com.sia.assistant

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var status: TextView
    private lateinit var input: EditText
    private val speechCode = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.parseColor("#0A0F2A"))
        root.setPadding(40, 120, 40, 40)

        val title = TextView(this)
        title.text = "SIA"
        title.textSize = 34f
        title.setTextColor(Color.WHITE)
        title.gravity = Gravity.CENTER

        val tagline = TextView(this)
        tagline.text = "Your Personal AI Companion"
        tagline.textSize = 14f
        tagline.setTextColor(Color.WHITE)
        tagline.gravity = Gravity.CENTER

        val circle = TextView(this)
        circle.text = "S"
        circle.textSize = 70f
        circle.setTextColor(Color.WHITE)
        circle.gravity = Gravity.CENTER
        val circleBg = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.parseColor("#4B6BFF"), Color.parseColor("#B05CFF"))
        )
        circleBg.shape = GradientDrawable.OVAL
        circle.background = circleBg
        val circleParams = LinearLayout.LayoutParams(420, 420)
        circleParams.gravity = Gravity.CENTER_HORIZONTAL
        circleParams.topMargin = 80
        circle.layoutParams = circleParams

        status = TextView(this)
        status.text = "Anmol Sir, main taiyaar hoon."
        status.textSize = 20f
        status.setTextColor(Color.WHITE)
        status.gravity = Gravity.CENTER
        val statusParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        statusParams.topMargin = 60
        status.layoutParams = statusParams

        val spacer = LinearLayout(this)
        spacer.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        )

        val bottom = LinearLayout(this)
        bottom.orientation = LinearLayout.HORIZONTAL
        bottom.gravity = Gravity.CENTER_VERTICAL

        input = EditText(this)
        input.hint = "Sia se kuch poochiye..."
        input.setTextColor(Color.WHITE)
        input.setHintTextColor(Color.parseColor("#AAAAAA"))
        input.setBackgroundColor(Color.parseColor("#1B2250"))
        input.setPadding(40, 30, 40, 30)
        input.layoutParams = LinearLayout.LayoutParams(0, 140, 1f)

        val micButton = Button(this)
        micButton.text = "MIC"
        micButton.setTextColor(Color.WHITE)
        micButton.setBackgroundColor(Color.parseColor("#B964FF"))
        micButton.layoutParams = LinearLayout.LayoutParams(220, 140)
        micButton.setOnClickListener { startListening() }

        val sendButton = Button(this)
        sendButton.text = "BHEJO"
        sendButton.setTextColor(Color.WHITE)
        sendButton.setBackgroundColor(Color.parseColor("#4B6BFF"))
        sendButton.layoutParams = LinearLayout.LayoutParams(220, 140)
        sendButton.setOnClickListener {
            val text = input.text.toString().trim()
            if (text.isEmpty()) {
                status.text = "Pehle kuch likhiye ya boliye."
            } else {
                status.text = "Aapne likha: $text"
            }
        }

        bottom.addView(input)
        bottom.addView(micButton)
        bottom.addView(sendButton)

        root.addView(title)
        root.addView(tagline)
        root.addView(circle)
        root.addView(status)
        root.addView(spacer)
        root.addView(bottom)

        setContentView(root)
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Boliye...")
        try {
            startActivityForResult(intent, speechCode)
        } catch (e: ActivityNotFoundException) {
            status.text = "Is phone mein bolne wali service nahi mili."
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == speechCode) {
            if (resultCode == RESULT_OK) {
                val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                val spoken = results?.firstOrNull()
                if (spoken != null) {
                    input.setText(spoken)
                    status.text = "Aapne kaha: $spoken"
                } else {
                    status.text = "Kuch sunai nahi diya, dobara try kijiye."
                }
            } else {
                status.text = "Kuch sunai nahi diya, dobara try kijiye."
            }
        }
    }
}
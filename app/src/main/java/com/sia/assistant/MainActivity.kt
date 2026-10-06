package com.sia.assistant

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var status: TextView
    private lateinit var input: EditText
    private lateinit var tts: TextToSpeech
    private var ttsReady = false
    private val speechCode = 101

    private val persona =
        "Tum Sia ho, Anmol Sir ki personal AI saathi. " +
        "Hamesha Hindi (Devanagari lipi) mein jawab do. " +
        "Jawab chhota rakho, sirf 2-3 vaakya. " +
        "Anmol Sir ko pyaar se Anmol Sir kehkar bulao."

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this, this)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.parseColor("#0A0F2A"))
        root.setPadding(40, 100, 40, 40)

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
        circle.textSize = 60f
        circle.setTextColor(Color.WHITE)
        circle.gravity = Gravity.CENTER
        val circleBg = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.parseColor("#4B6BFF"), Color.parseColor("#B05CFF"))
        )
        circleBg.shape = GradientDrawable.OVAL
        circle.background = circleBg
        val circleParams = LinearLayout.LayoutParams(340, 340)
        circleParams.gravity = Gravity.CENTER_HORIZONTAL
        circleParams.topMargin = 40
        circle.layoutParams = circleParams

        status = TextView(this)
        status.text = "Anmol Sir, main taiyaar hoon."
        status.textSize = 20f
        status.setTextColor(Color.WHITE)
        status.gravity = Gravity.CENTER
        status.setPadding(0, 40, 0, 40)

        val scroll = ScrollView(this)
        scroll.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        )
        scroll.addView(status)

        val keyButton = Button(this)
        keyButton.text = "API KEY LAGAYEIN"
        keyButton.setTextColor(Color.WHITE)
        keyButton.setBackgroundColor(Color.parseColor("#2A3170"))
        keyButton.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 110
        )
        keyButton.setOnClickListener { showKeyDialog() }

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
                speak("पहले कुछ लिखिए या बोलिए।")
            } else {
                askAI(text)
            }
        }

        bottom.addView(input)
        bottom.addView(micButton)
        bottom.addView(sendButton)

        root.addView(title)
        root.addView(tagline)
        root.addView(circle)
        root.addView(scroll)
        root.addView(keyButton)
        root.addView(bottom)

        setContentView(root)
    }

    private fun prefs() = getSharedPreferences("sia_prefs", MODE_PRIVATE)

    private fun showKeyDialog() {
        val box = EditText(this)
        box.hint = "AIza..."
        box.setSingleLine(true)
        AlertDialog.Builder(this)
            .setTitle("Gemini API key")
            .setView(box)
            .setPositiveButton("Save") { _, _ ->
                val k = box.text.toString().trim()
                if (k.isNotEmpty()) {
                    prefs().edit().putString("gemini_key", k).apply()
                    status.text = "Key save ho gayi."
                    speak("की सेव हो गई।")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun askAI(question: String) {
        val key = prefs().getString("gemini_key", "") ?: ""
        if (key.isEmpty()) {
            status.text = "Pehle API key lagayiye."
            speak("पहले API key लगाइए।")
            return
        }
        status.text = "Sia soch rahi hai..."
        Thread {
            var answer: String
            try {
                val part = JSONObject().put("text", question)
                val content = JSONObject().put("parts", JSONArray().put(part))
                val sysPart = JSONObject().put("text", persona)
                val sys = JSONObject().put("parts", JSONArray().put(sysPart))
                val body = JSONObject()
                    .put("contents", JSONArray().put(content))
                    .put("systemInstruction", sys)

                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("x-goog-api-key", key)
                conn.doOutput = true
                conn.connectTimeout = 15000
                conn.readTimeout = 30000
                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val text = stream?.bufferedReader()?.use { it.readText() } ?: ""

                if (code in 200..299) {
                    val json = JSONObject(text)
                    answer = json.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")
                        .trim()
                } else if (code == 429) {
                    answer = "Abhi ki seema khatam ho gayi hai, thodi der baad koshish kijiye."
                } else if (code == 400 || code == 403) {
                    answer = "API key sahi nahi lagti, dobara lagayiye."
                } else {
                    answer = "Dikkat aayi, code $code."
                }
            } catch (e: Exception) {
                answer = "Jawab nahi mil paaya, internet check karke dobara koshish kijiye."
            }
            runOnUiThread {
                status.text = answer
                speak(answer)
            }
        }.start()
    }

    override fun onInit(initStatus: Int) {
        if (initStatus == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale("hi", "IN"))
            if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                status.text = "Hindi awaaz phone mein nahi hai."
            } else {
                ttsReady = true
                speak("अनमोल सर, मैं तैयार हूँ।")
            }
        } else {
            status.text = "Sia ki awaaz shuru nahi ho paayi."
        }
    }

    private fun speak(text: String) {
        if (ttsReady) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "sia_speak")
        }
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
            val results = if (resultCode == RESULT_OK) {
                data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            } else null
            val spoken = results?.firstOrNull()
            if (spoken != null) {
                input.setText(spoken)
                askAI(spoken)
            } else {
                status.text = "Kuch sunai nahi diya, dobara try kijiye."
                speak("कुछ सुनाई नहीं दिया, दोबारा कोशिश कीजिए।")
            }
        }
    }

    override fun onDestroy() {
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }
}
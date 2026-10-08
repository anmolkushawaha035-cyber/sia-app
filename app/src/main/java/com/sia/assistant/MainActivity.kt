package com.sia.assistant

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class ApiException(val code: Int, message: String) : Exception(message)

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var status: TextView
    private lateinit var input: EditText
    private lateinit var tts: TextToSpeech
    private var ttsReady = false
    private val speechCode = 101

    // Agar kabhi 404 aaye to bas yahan model ke naam badalne hain
    private val models = listOf(
        "gemini-3.6-flash",
        "gemini-3.5-flash",
        "gemini-2.5-flash"
    )

    private val persona =
        "Tum Sia ho, Anmol Sir ki personal AI assistant. " +
        "User ko hamesha 'Anmol Sir' kehkar bulao. " +
        "Hamesha Hindi (Devanagari lipi) mein jawab do, jab tak Anmol Sir English mein baat na karein. " +
        "Jawab chhote, saaf, narm aur madadgaar rakho (2 se 4 vakya). " +
        "Taaza khabar ya naye jaankari ke liye internet search ka istemaal karo. " +
        "Jawab mein star, hash ya koi markdown symbol mat use karo."

    private val history = mutableListOf<Pair<String, String>>()

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun historyFile(): File = File(filesDir, "sia_history.json")

    private fun saveHistory() {
        try {
            val arr = JSONArray()
            for (h in history) {
                arr.put(JSONObject().put("role", h.first).put("text", h.second))
            }
            historyFile().writeText(arr.toString(), Charsets.UTF_8)
        } catch (e: Exception) {
        }
    }

    private fun loadHistory() {
        try {
            val f = historyFile()
            if (!f.exists()) return
            val arr = JSONArray(f.readText(Charsets.UTF_8))
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                history.add(Pair(o.getString("role"), o.getString("text")))
            }
        } catch (e: Exception) {
        }
    }

    private fun makeButton(label: String, bg: Int): Button {
        val b = Button(this)
        b.text = label
        b.setTextColor(Color.WHITE)
        b.setTypeface(null, Typeface.BOLD)
        b.setBackgroundColor(bg)
        return b
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.parseColor("#0A0F2A"))
        root.setPadding(dp(16), dp(32), dp(16), dp(16))
        root.gravity = Gravity.CENTER_HORIZONTAL

        val title = TextView(this)
        title.text = "SIA"
        title.textSize = 34f
        title.setTextColor(Color.WHITE)
        title.setTypeface(null, Typeface.BOLD)
        title.gravity = Gravity.CENTER
        root.addView(title)

        val sub = TextView(this)
        sub.text = "Your Personal AI Companion"
        sub.textSize = 15f
        sub.setTextColor(Color.WHITE)
        sub.gravity = Gravity.CENTER
        root.addView(sub)

        val circle = TextView(this)
        circle.text = "S"
        circle.textSize = 64f
        circle.setTextColor(Color.WHITE)
        circle.setTypeface(null, Typeface.BOLD)
        circle.gravity = Gravity.CENTER
        val bg = GradientDrawable()
        bg.shape = GradientDrawable.OVAL
        bg.orientation = GradientDrawable.Orientation.TOP_BOTTOM
        bg.colors = intArrayOf(Color.parseColor("#4B6BFF"), Color.parseColor("#B866FF"))
        circle.background = bg
        val cp = LinearLayout.LayoutParams(dp(160), dp(160))
        cp.topMargin = dp(20)
        cp.bottomMargin = dp(20)
        root.addView(circle, cp)

        val scroll = ScrollView(this)
        status = TextView(this)
        status.text = "Anmol Sir, main taiyaar hoon."
        status.textSize = 19f
        status.setTextColor(Color.WHITE)
        status.setTypeface(null, Typeface.BOLD)
        status.gravity = Gravity.CENTER
        status.setPadding(dp(8), dp(8), dp(8), dp(8))
        scroll.addView(status)
        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        val keyBtn = makeButton("API KEY LAGAYEIN", Color.parseColor("#2A3170"))
        root.addView(keyBtn, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        input = EditText(this)
        input.hint = "Yahan likhiye..."
        input.setHintTextColor(Color.parseColor("#8890B5"))
        input.setTextColor(Color.WHITE)
        input.setBackgroundColor(Color.parseColor("#1B2150"))
        input.setPadding(dp(12), dp(12), dp(12), dp(12))
        input.maxLines = 3
        val ip = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        ip.topMargin = dp(8)
        root.addView(input, ip)

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        val micBtn = makeButton("MIC", Color.parseColor("#B866FF"))
        val sendBtn = makeButton("BHEJO", Color.parseColor("#4B6BFF"))
        row.addView(micBtn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(sendBtn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(row, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        setContentView(root)

        loadHistory()

        tts = TextToSpeech(this, this)

        keyBtn.setOnClickListener { showKeyDialog() }
        micBtn.setOnClickListener { startListening() }
        sendBtn.setOnClickListener {
            val t = input.text.toString().trim()
            if (t.isNotEmpty()) {
                input.setText("")
                askGemini(t)
            }
        }
    }

    override fun onInit(s: Int) {
        if (s == TextToSpeech.SUCCESS) {
            tts.language = Locale("hi", "IN")
            ttsReady = true
            speak("Anmol Sir, main taiyaar hoon.")
        }
    }

    private fun speak(text: String) {
        if (ttsReady) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "sia")
        }
    }

    private fun setStatus(text: String) {
        status.text = text
    }

    private fun showKeyDialog() {
        val prefs = getSharedPreferences("sia_prefs", MODE_PRIVATE)
        val box = EditText(this)
        box.hint = "Yahan API key paste kijiye"
        box.inputType = InputType.TYPE_CLASS_TEXT
        box.setText(prefs.getString("api_key", ""))
        AlertDialog.Builder(this)
            .setTitle("API Key")
            .setView(box)
            .setPositiveButton("Save") { _, _ ->
                prefs.edit().putString("api_key", box.text.toString().trim()).apply()
                setStatus("API key save ho gayi, Anmol Sir.")
                speak("API key save ho gayi, Anmol Sir.")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Boliye, Anmol Sir...")
        try {
            startActivityForResult(intent, speechCode)
        } catch (e: ActivityNotFoundException) {
            setStatus("Is phone mein voice input nahi mila.")
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == speechCode && resultCode == RESULT_OK && data != null) {
            val list = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val said = list?.firstOrNull()
            if (!said.isNullOrBlank()) {
                askGemini(said)
            }
        }
    }

    private fun askGemini(userText: String) {
        val prefs = getSharedPreferences("sia_prefs", MODE_PRIVATE)
        val key = prefs.getString("api_key", "") ?: ""
        if (key.isBlank()) {
            val m = "Anmol Sir, pehle API KEY LAGAYEIN button dabaiye."
            setStatus(m)
            speak(m)
            return
        }
        setStatus("Aapne kaha: $userText\n\nSia soch rahi hai...")
        history.add(Pair("user", userText))
        val snapshot = history.toList()

        Thread {
            var reply: String? = null
            var lastError = ""
            for (model in models) {
                try {
                    reply = try {
                        callModel(model, key, snapshot, true)
                    } catch (e: ApiException) {
                        if (e.code == 400) callModel(model, key, snapshot, false) else throw e
                    }
                    break
                } catch (e: ApiException) {
                    lastError = "code ${e.code}: ${e.message}"
                    if (e.code != 404 && e.code != 503 && e.code != 429) break
                } catch (e: Exception) {
                    lastError = "internet ya network dikkat: ${e.message}"
                    break
                }
            }
            val finalReply = reply
            val finalError = lastError
            runOnUiThread {
                if (finalReply != null) {
                    history.add(Pair("model", finalReply))
                    saveHistory()
                    setStatus(finalReply)
                    speak(finalReply)
                } else {
                    if (history.isNotEmpty()) history.removeAt(history.size - 1)
                    val short = if (finalError.length > 300) finalError.substring(0, 300) else finalError
                    setStatus("Dikkat aayi, $short")
                    speak("Anmol Sir, kuch dikkat aayi.")
                }
            }
        }.start()
    }

    private fun callModel(
        model: String,
        key: String,
        snapshot: List<Pair<String, String>>,
        useSearch: Boolean
    ): String {
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 20000
        conn.readTimeout = 90000
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("x-goog-api-key", key)
        conn.doOutput = true

        val body = JSONObject()
        val sys = JSONObject()
        sys.put("parts", JSONArray().put(JSONObject().put("text", persona)))
        body.put("systemInstruction", sys)

        if (useSearch) {
            body.put("tools", JSONArray().put(JSONObject().put("google_search", JSONObject())))
        }

        // Purani baatein: peeche se lekar lagbhag 100000 akshar tak bhejte hain
        val recentList = mutableListOf<Pair<String, String>>()
        var total = 0
        for (i in snapshot.indices.reversed()) {
            total += snapshot[i].second.length
            if (total > 100000) break
            recentList.add(0, snapshot[i])
        }
        var recent: List<Pair<String, String>> = recentList
        while (recent.isNotEmpty() && recent[0].first != "user") {
            recent = recent.drop(1)
        }
        val contents = JSONArray()
        for (item in recent) {
            val part = JSONObject().put("text", item.second)
            val msg = JSONObject()
            msg.put("role", item.first)
            msg.put("parts", JSONArray().put(part))
            contents.put(msg)
        }
        body.put("contents", contents)

        conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""

        if (code !in 200..299) {
            var msg = text
            try {
                msg = JSONObject(text).getJSONObject("error").getString("message")
            } catch (e: Exception) {
            }
            throw ApiException(code, msg)
        }

        val root = JSONObject(text)
        val cands = root.optJSONArray("candidates")
        if (cands == null || cands.length() == 0) {
            throw ApiException(0, "Jawab khali aaya")
        }
        val parts = cands.getJSONObject(0).getJSONObject("content").getJSONArray("parts")
        val sb = StringBuilder()
        for (i in 0 until parts.length()) {
            sb.append(parts.getJSONObject(i).optString("text", ""))
        }
        return sb.toString().replace("*", "").replace("#", "").trim()
    }

    override fun onDestroy() {
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }
}
package com.sia.assistant

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.parseColor("#0B0F2A"))
        root.setPadding(40, 120, 40, 40)

        val title = TextView(this)
        title.text = "SIA"
        title.textSize = 34f
        title.setTextColor(Color.parseColor("#00D1FF"))
        title.gravity = Gravity.CENTER

        val tagline = TextView(this)
        tagline.text = "Your Personal AI Companion"
        tagline.textSize = 14f
        tagline.setTextColor(Color.parseColor("#B866FF"))
        tagline.gravity = Gravity.CENTER

        val avatar = TextView(this)
        avatar.text = "S"
        avatar.textSize = 70f
        avatar.setTextColor(Color.WHITE)
        avatar.gravity = Gravity.CENTER
        val circle = GradientDrawable()
        circle.shape = GradientDrawable.OVAL
        circle.colors = intArrayOf(Color.parseColor("#4B6BFF"), Color.parseColor("#B866FF"))
        avatar.background = circle
        val avatarParams = LinearLayout.LayoutParams(420, 420)
        avatarParams.gravity = Gravity.CENTER_HORIZONTAL
        avatarParams.setMargins(0, 80, 0, 40)
        avatar.layoutParams = avatarParams

        val status = TextView(this)
        status.text = "Anmol Sir, main taiyaar hoon."
        status.textSize = 18f
        status.setTextColor(Color.WHITE)
        status.gravity = Gravity.CENTER

        val spacer = View(this)
        spacer.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        )

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL

        val input = EditText(this)
        input.hint = "Sia se kuch poochiye..."
        input.setHintTextColor(Color.parseColor("#8890B5"))
        input.setTextColor(Color.WHITE)
        input.setPadding(40, 30, 40, 30)
        val box = GradientDrawable()
        box.setColor(Color.parseColor("#1A2150"))
        box.cornerRadius = 60f
        input.background = box
        input.layoutParams = LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        )

        val mic = Button(this)
        mic.text = "Mic"
        mic.setTextColor(Color.WHITE)
        mic.setBackgroundColor(Color.parseColor("#B866FF"))

        val send = Button(this)
        send.text = "Bhejo"
        send.setTextColor(Color.WHITE)
        send.setBackgroundColor(Color.parseColor("#4B6BFF"))

        mic.setOnClickListener {
            status.text = "Mic aage ke step mein jodenge."
        }

        send.setOnClickListener {
            val text = input.text.toString().trim()
            if (text.isNotEmpty()) {
                status.text = "Anmol Sir, aapne likha: $text"
                input.setText("")
            }
        }

        row.addView(input)
        row.addView(mic)
        row.addView(send)

        root.addView(title)
        root.addView(tagline)
        root.addView(avatar)
        root.addView(status)
        root.addView(spacer)
        root.addView(row)

        setContentView(root)
    }
}
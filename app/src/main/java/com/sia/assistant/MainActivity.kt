package com.sia.assistant

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val textView = TextView(this)
        textView.text = "Namaste Anmol Sir,\nmain Sia hoon."
        textView.textSize = 26f
        textView.setTextColor(Color.parseColor("#00D1FF"))
        textView.setBackgroundColor(Color.parseColor("#0B0F2A"))
        textView.gravity = Gravity.CENTER

        setContentView(textView)
    }
}
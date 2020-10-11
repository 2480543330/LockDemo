package com.example.dotunlockdemo

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main2.*

class MainActivity2 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        //获取传递过来的密码
        val psd = intent.getIntExtra("psd", 0)

        //查看密码
        button1.setOnClickListener {
            button1.text = psd.toString()
        }
    }
}
package com.bol.sho.game

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bol.sho.databinding.DynamiteStartGameBinding

class DynamiteStart: AppCompatActivity() {

    private lateinit var binding: DynamiteStartGameBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DynamiteStartGameBinding.inflate(layoutInflater)
        setContentView(binding.root)
        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        binding.btn100.setOnClickListener {
            goToGame(100)
        }

        binding.btn200.setOnClickListener {
            goToGame(200)
        }

        binding.btn300.setOnClickListener {
            goToGame(300)
        }
    }

    private fun goToGame(credits: Int) {
        val intent = Intent(this, DynamiteGame::class.java)
        intent.putExtra("credits", credits)
        startActivity(intent)
    }

    override fun onBackPressed() {
        val setIntent = Intent(Intent.ACTION_MAIN)
        setIntent.addCategory(Intent.CATEGORY_HOME)
        setIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(setIntent)
    }
}
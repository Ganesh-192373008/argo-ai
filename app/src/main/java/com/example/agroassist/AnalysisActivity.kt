package com.example.agroassist

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity

class AnalysisActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analysis)

        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val statusIcon1 = findViewById<ImageView>(R.id.statusIcon1)
        val statusIcon2 = findViewById<ImageView>(R.id.statusIcon2)
        val statusIcon3 = findViewById<ImageView>(R.id.statusIcon3)

        progressBar.progress = 0

        val animator = ValueAnimator.ofInt(0, 100)
        animator.duration = 3000
        animator.addUpdateListener { animation ->
            val progress = animation.animatedValue as Int
            progressBar.progress = progress

            if (progress >= 33) {
                statusIcon1.setImageResource(R.drawable.ic_check_circle)
            }
            if (progress >= 66) {
                statusIcon2.setImageResource(R.drawable.ic_check_circle)
            }
            if (progress >= 95) {
                statusIcon3.setImageResource(R.drawable.ic_check_circle)
            }
        }

        animator.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                val intent = Intent(this@AnalysisActivity, ResultsActivity::class.java)
                intent.putExtra("image_path", this@AnalysisActivity.intent.getStringExtra("image_path"))
                intent.putExtra("image_uri", this@AnalysisActivity.intent.getStringExtra("image_uri"))
                startActivity(intent)
                finish()
            }
        })

        animator.start()
    }
}

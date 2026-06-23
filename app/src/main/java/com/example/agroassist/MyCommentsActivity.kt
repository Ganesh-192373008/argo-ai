package com.example.agroassist

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MyCommentsActivity : AppCompatActivity() {

    private lateinit var dbHelper: AgroDatabaseHelper
    private lateinit var commentsContainer: LinearLayout
    private lateinit var emptyStateLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_comments)

        dbHelper = AgroDatabaseHelper(this)

        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener { finish() }

        commentsContainer = findViewById(R.id.commentsContainer)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
    }

    override fun onResume() {
        super.onResume()
        loadCommentsInbox()
    }

    private fun loadCommentsInbox() {
        commentsContainer.removeAllViews()
        val commentNotifications = dbHelper.getCommentsOnMyPosts()

        if (commentNotifications.isEmpty()) {
            emptyStateLayout.visibility = View.VISIBLE
            return
        } else {
            emptyStateLayout.visibility = View.GONE
        }

        for (item in commentNotifications) {
            val postId = item["post_id"]?.toIntOrNull() ?: -1
            val avatar = item["avatar"] ?: "👨‍🌾"
            val commenterName = item["name"] ?: "Farmer Friend"
            val commentContent = item["content"] ?: ""
            val timeAgo = item["time"] ?: "Just now"
            val postSnippet = item["post_content"] ?: ""

            val itemView = LayoutInflater.from(this).inflate(R.layout.item_comment_received, commentsContainer, false)

            itemView.findViewById<TextView>(R.id.textAvatar).text = avatar
            itemView.findViewById<TextView>(R.id.textCommentHeader).text = "$commenterName replied to your post:"
            itemView.findViewById<TextView>(R.id.textPostSnippet).text = "\"$postSnippet\""
            itemView.findViewById<TextView>(R.id.textCommentContent).text = commentContent
            itemView.findViewById<TextView>(R.id.textTime).text = timeAgo

            // Clicking on the notification card takes you to the full Comments screen for that post!
            itemView.setOnClickListener {
                if (postId != -1) {
                    val intent = Intent(this, CommentsActivity::class.java).apply {
                        putExtra("post_id", postId)
                    }
                    startActivity(intent)
                }
            }

            commentsContainer.addView(itemView)
        }
    }
}

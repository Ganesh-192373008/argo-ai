package com.example.agroassist

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class CommentsActivity : AppCompatActivity() {

    private lateinit var dbHelper: AgroDatabaseHelper
    private var postId: Int = -1
    private lateinit var postContentContainer: LinearLayout
    private lateinit var commentsContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comments)

        dbHelper = AgroDatabaseHelper(this)
        postId = intent.getIntExtra("post_id", -1)

        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener { finish() }

        postContentContainer = findViewById(R.id.postContentContainer)
        commentsContainer = findViewById(R.id.commentsContainer)

        val editCommentInput = findViewById<EditText>(R.id.editCommentInput)
        val btnSubmitComment = findViewById<Button>(R.id.btnSubmitComment)

        if (postId == -1) {
            Toast.makeText(this, "Error loading post", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadPostDetails()
        loadComments()

        btnSubmitComment.setOnClickListener {
            val commentText = editCommentInput.text.toString().trim()
            if (commentText.isEmpty()) {
                Toast.makeText(this, "Reply cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            var userAvatar = "👨‍🌾"
            var userName = "Farmer Friend"
            try {
                val profile = dbHelper.getProfile()
                val profileName = profile["name"]
                if (!profileName.isNullOrEmpty()) {
                    userName = profileName
                }
                if (userName.lowercase().contains("sunita") || userName.lowercase().contains("lakshmi") || userName.lowercase().contains("devi") || userName.lowercase().contains("reddy")) {
                    userAvatar = "👩‍🌾"
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Save comment to database
            dbHelper.addComment(postId, userAvatar, userName, commentText, "Just now", 1)

            // Clear text input and refresh list
            editCommentInput.text.clear()
            loadComments()

            Toast.makeText(this, "Reply posted!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadPostDetails() {
        val posts = dbHelper.getPosts()
        val post = posts.find { it["id"] == postId }
        if (post == null) {
            Toast.makeText(this, "Post not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        postContentContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)
        val postView = inflater.inflate(R.layout.item_community_post, postContentContainer, false)

        val textAvatar = postView.findViewById<TextView>(R.id.textAvatar)
        val textFarmerName = postView.findViewById<TextView>(R.id.textFarmerName)
        val textMeta = postView.findViewById<TextView>(R.id.textMeta)
        val textContent = postView.findViewById<TextView>(R.id.textContent)
        
        val btnLike = postView.findViewById<LinearLayout>(R.id.btnLike)
        val imageLike = postView.findViewById<ImageView>(R.id.imageLike)
        val textLikeCount = postView.findViewById<TextView>(R.id.textLikeCount)
        
        val btnComment = postView.findViewById<LinearLayout>(R.id.btnComment)
        val textCommentCount = postView.findViewById<TextView>(R.id.textCommentCount)
        
        val btnShare = postView.findViewById<LinearLayout>(R.id.btnShare)

        val likes = post["likes"] as Int
        val isLiked = (post["is_liked"] as Int) == 1
        val commentsList = dbHelper.getComments(postId)

        textAvatar.text = post["avatar"] as String
        textFarmerName.text = post["name"] as String
        textMeta.text = "${post["state"] as String} • ${post["time"] as String}"
        textContent.text = post["content"] as String
        textLikeCount.text = likes.toString()
        textCommentCount.text = commentsList.size.toString()

        // Sync like button display
        updateLikeUI(isLiked, imageLike, textLikeCount, likes)

        // Handle Like click
        btnLike.setOnClickListener {
            val newIsLiked = !isLiked
            val newLikes = if (newIsLiked) likes + 1 else likes - 1
            dbHelper.updatePostLikes(postId, newLikes, if (newIsLiked) 1 else 0)
            loadPostDetails() // Refresh UI
        }

        // Disable comments button on details screen to avoid infinite activity nesting
        btnComment.isEnabled = false

        // Handle Share click
        btnShare.setOnClickListener {
            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_SUBJECT, "AgroAssist AI Community Post")
                putExtra(android.content.Intent.EXTRA_TEXT, "${post["name"] as String} posted on AgroAssist AI: ${post["content"] as String}")
            }
            startActivity(android.content.Intent.createChooser(shareIntent, "Share post via"))
        }

        postContentContainer.addView(postView)
    }

    private fun loadComments() {
        commentsContainer.removeAllViews()
        val comments = dbHelper.getComments(postId)
        for (comment in comments) {
            val commentView = LayoutInflater.from(this).inflate(R.layout.item_comment, commentsContainer, false)
            commentView.findViewById<TextView>(R.id.commentAvatar).text = comment["avatar"]
            commentView.findViewById<TextView>(R.id.commentName).text = comment["name"]
            commentView.findViewById<TextView>(R.id.commentTime).text = comment["time"]
            commentView.findViewById<TextView>(R.id.commentContent).text = comment["content"]
            commentsContainer.addView(commentView)
        }
    }

    private fun updateLikeUI(isLiked: Boolean, imageLike: ImageView, textLikeCount: TextView, count: Int) {
        textLikeCount.text = count.toString()
        if (isLiked) {
            imageLike.setImageResource(android.R.drawable.btn_star_big_on)
            imageLike.setColorFilter(Color.parseColor("#FFD54F"))
            textLikeCount.setTextColor(Color.parseColor("#FFB300"))
        } else {
            imageLike.setImageResource(android.R.drawable.btn_star_big_off)
            imageLike.clearColorFilter()
            textLikeCount.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
        }
    }
}

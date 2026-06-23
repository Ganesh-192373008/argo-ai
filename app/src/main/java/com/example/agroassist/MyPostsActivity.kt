package com.example.agroassist

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MyPostsActivity : AppCompatActivity() {

    private lateinit var dbHelper: AgroDatabaseHelper
    private lateinit var postsContainer: LinearLayout
    private lateinit var emptyStateLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_posts)

        dbHelper = AgroDatabaseHelper(this)

        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener { finish() }

        postsContainer = findViewById(R.id.postsContainer)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
    }

    override fun onResume() {
        super.onResume()
        loadMyPosts()
    }

    private fun loadMyPosts() {
        postsContainer.removeAllViews()
        val posts = dbHelper.getPosts(onlyMyPosts = true)

        if (posts.isEmpty()) {
            emptyStateLayout.visibility = View.VISIBLE
            return
        } else {
            emptyStateLayout.visibility = View.GONE
        }

        for (post in posts) {
            val postId = post["id"] as Int
            val avatar = post["avatar"] as String
            val name = post["name"] as String
            val state = post["state"] as String
            val time = post["time"] as String
            val content = post["content"] as String
            val likes = post["likes"] as Int
            val isLiked = (post["is_liked"] as Int) == 1
            val commentsCount = dbHelper.getComments(postId).size
            val imageUri = post["image_uri"] as? String ?: ""

            val postView = LayoutInflater.from(this).inflate(R.layout.item_community_post, postsContainer, false)

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
            val imagePost = postView.findViewById<ImageView>(R.id.imagePost)

            textAvatar.text = avatar
            textFarmerName.text = name
            textMeta.text = "$state • $time"
            textContent.text = content
            textLikeCount.text = likes.toString()
            textCommentCount.text = commentsCount.toString()

            if (imagePost != null) {
                if (imageUri.isNotEmpty()) {
                    try {
                        imagePost.setImageURI(Uri.parse(imageUri))
                        imagePost.visibility = View.VISIBLE
                    } catch (e: Exception) {
                        e.printStackTrace()
                        imagePost.visibility = View.GONE
                    }
                } else {
                    imagePost.visibility = View.GONE
                }
            }

            // Sync like
            updateLikeUI(isLiked, imageLike, textLikeCount, likes)

            // Like action
            btnLike.setOnClickListener {
                val newIsLiked = !isLiked
                val newLikes = if (newIsLiked) likes + 1 else likes - 1
                dbHelper.updatePostLikes(postId, newLikes, if (newIsLiked) 1 else 0)
                loadMyPosts() // Refresh
            }

            // Comment action opens dedicated CommentsActivity
            btnComment.setOnClickListener {
                val intent = Intent(this, CommentsActivity::class.java).apply {
                    putExtra("post_id", postId)
                }
                startActivity(intent)
            }

            // Share action
            btnShare.setOnClickListener {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "AgroAssist AI Community Post")
                    putExtra(Intent.EXTRA_TEXT, "$name posted on AgroAssist: $content")
                }
                startActivity(Intent.createChooser(shareIntent, "Share post via"))
            }

            postsContainer.addView(postView)
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

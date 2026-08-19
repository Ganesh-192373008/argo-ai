package com.example.agroassist

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.app.Activity
import androidx.activity.result.contract.ActivityResultContracts

class CommunityActivity : BaseProtectedActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    private lateinit var postsContainer: LinearLayout
    private val dbHelper by lazy { AgroDatabaseHelper(this) }

    private val createPostLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            if (data != null) {
                val avatar = data.getStringExtra("avatar") ?: "👨‍🌾"
                val name = data.getStringExtra("name") ?: "Farmer Friend"
                val state = data.getStringExtra("state") ?: "India"
                val content = data.getStringExtra("content") ?: ""
                val imageUri = data.getStringExtra("image_uri")
                
                // Add user post directly to SQLite database as user post (isMyPost = 1)
                dbHelper.addPost(avatar, name, state, "Just now", content, 1, imageUri)
                
                // Refresh posts list
                loadPostsFeed()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_community)

        postsContainer = findViewById(R.id.postsContainer)
        val btnCreatePost = findViewById<LinearLayout>(R.id.btnCreatePost)

        val btnMyPosts = findViewById<TextView>(R.id.btnMyPosts)
        val btnMyComments = findViewById<TextView>(R.id.btnMyComments)

        // Setup bottom nav listeners
        setupBottomNav()

        // Seed initial posts/comments to SQLite database if empty
        seedCommunityIfEmpty()

        // Handle Create Post click
        btnCreatePost.setOnClickListener {
            val intent = Intent(this, CreatePostActivity::class.java)
            createPostLauncher.launch(intent)
        }

        // Handle navigation to My Posts screen
        btnMyPosts.setOnClickListener {
            val intent = Intent(this, MyPostsActivity::class.java)
            startActivity(intent)
        }

        // Handle navigation to Comments Received inbox screen
        btnMyComments.setOnClickListener {
            val intent = Intent(this, MyCommentsActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        loadPostsFeed()
    }

    private fun seedCommunityIfEmpty() {
        // Real-time user community feed - No hardcoded placeholders
    }

    private fun loadPostsFeed() {
        postsContainer.removeAllViews()
        val posts = dbHelper.getPosts()

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

            // Populate content
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

            // Sync like button display
            updateLikeUI(isLiked, imageLike, textLikeCount, likes)

            // Handle Like click
            btnLike.setOnClickListener {
                val newIsLiked = !isLiked
                val newLikes = if (newIsLiked) likes + 1 else likes - 1
                dbHelper.updatePostLikes(postId, newLikes, if (newIsLiked) 1 else 0)
                loadPostsFeed() // Refresh feed UI
            }

            // Handle Comment click - Open full screen CommentsActivity
            btnComment.setOnClickListener {
                val intent = Intent(this, CommentsActivity::class.java).apply {
                    putExtra("post_id", postId)
                }
                startActivity(intent)
            }

            // Handle Share click
            btnShare.setOnClickListener {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "AgroAssist AI Community Post")
                    putExtra(Intent.EXTRA_TEXT, "$name posted on AgroAssist AI: $content")
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
            imageLike.setColorFilter(Color.parseColor("#FFD54F")) // Gold color
            textLikeCount.setTextColor(Color.parseColor("#FFB300"))
        } else {
            imageLike.setImageResource(android.R.drawable.btn_star_big_off)
            imageLike.clearColorFilter()
            textLikeCount.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
        }
    }

    private fun setupBottomNav() {
        val navHome = findViewById<LinearLayout>(R.id.navHome)
        val navDetection = findViewById<LinearLayout>(R.id.navDetection)
        val navCommunity = findViewById<LinearLayout>(R.id.navCommunity)
        val navAssistant = findViewById<LinearLayout>(R.id.navAssistant)
        val navProfile = findViewById<LinearLayout>(R.id.navProfile)

        navHome.setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        navDetection.setOnClickListener {
            val intent = Intent(this, DetectionActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        navCommunity.setOnClickListener {
            // Already on Community
            Toast.makeText(this, "You are already on Community", Toast.LENGTH_SHORT).show()
        }

        navAssistant.setOnClickListener {
            val intent = Intent(this, ChatAssistantActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        navProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }
}

package com.example.agroassist

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class AgroDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "agroassist.db"
        private const val DATABASE_VERSION = 3 // Incremented to support community posts & comments & tracked crops

        // Profile Table
        const val TABLE_PROFILE = "profile"
        const val COLUMN_PROFILE_ID = "id"
        const val COLUMN_PROFILE_NAME = "name"
        const val COLUMN_PROFILE_AGE = "age"
        const val COLUMN_PROFILE_CROPS = "crops"
        const val COLUMN_PROFILE_LOCATION = "location"
        const val COLUMN_PROFILE_GPS = "gps"

        // Schedule Table
        const val TABLE_SCHEDULE = "schedule"
        const val COLUMN_SCHED_ID = "id"
        const val COLUMN_SCHED_CROP = "crop_name"
        const val COLUMN_SCHED_TYPE = "schedule_type" // Water or Fertilizer
        const val COLUMN_SCHED_DETAIL = "detail" // Fertilizer name or task
        const val COLUMN_SCHED_DATE = "date"
        const val COLUMN_SCHED_TIME = "time"

        // History Table
        const val TABLE_HISTORY = "history"
        const val COLUMN_HIST_ID = "id"
        const val COLUMN_HIST_IMAGE = "image_path"
        const val COLUMN_HIST_CROP = "crop_name"
        const val COLUMN_HIST_DISEASE = "disease"
        const val COLUMN_HIST_CONFIDENCE = "confidence"
        const val COLUMN_HIST_TIMESTAMP = "timestamp"

        // Tracked Crops Table
        const val TABLE_TRACKED_CROPS = "tracked_crops"
        const val COLUMN_TC_ID = "id"
        const val COLUMN_TC_NAME = "crop_name"
        const val COLUMN_TC_PLANTED_DATE = "planted_date"
        const val COLUMN_TC_HARVEST_DATE = "expected_harvest_date"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createProfileTable = """
            CREATE TABLE $TABLE_PROFILE (
                $COLUMN_PROFILE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_PROFILE_NAME TEXT,
                $COLUMN_PROFILE_AGE TEXT,
                $COLUMN_PROFILE_CROPS TEXT,
                $COLUMN_PROFILE_LOCATION TEXT,
                $COLUMN_PROFILE_GPS INTEGER DEFAULT 0
            )
        """.trimIndent()

        val createScheduleTable = """
            CREATE TABLE $TABLE_SCHEDULE (
                $COLUMN_SCHED_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_SCHED_CROP TEXT,
                $COLUMN_SCHED_TYPE TEXT,
                $COLUMN_SCHED_DETAIL TEXT,
                $COLUMN_SCHED_DATE TEXT,
                $COLUMN_SCHED_TIME TEXT
            )
        """.trimIndent()

        val createHistoryTable = """
            CREATE TABLE $TABLE_HISTORY (
                $COLUMN_HIST_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_HIST_IMAGE TEXT,
                $COLUMN_HIST_CROP TEXT,
                $COLUMN_HIST_DISEASE TEXT,
                $COLUMN_HIST_CONFIDENCE TEXT,
                $COLUMN_HIST_TIMESTAMP TEXT
            )
        """.trimIndent()

        db.execSQL(createProfileTable)
        db.execSQL(createScheduleTable)
        db.execSQL(createHistoryTable)

        // Seed some sample history items
        val seedHistory = """
            INSERT INTO $TABLE_HISTORY ($COLUMN_HIST_CROP, $COLUMN_HIST_DISEASE, $COLUMN_HIST_CONFIDENCE, $COLUMN_HIST_TIMESTAMP)
            VALUES 
            ('Rice', 'Leaf Blast', '94% Confidence', 'June 09, 2026 10:30 AM'),
            ('Wheat', 'Rust', '88% Confidence', 'June 08, 2026 02:15 PM')
        """.trimIndent()
        db.execSQL(seedHistory)

        // Create community tables
        createCommunityTables(db)

        // Create tracked crops table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS ${TABLE_TRACKED_CROPS} (
                ${COLUMN_TC_ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                ${COLUMN_TC_NAME} TEXT,
                ${COLUMN_TC_PLANTED_DATE} TEXT,
                ${COLUMN_TC_HARVEST_DATE} TEXT
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            createCommunityTables(db)
        }
        if (oldVersion < 3) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS ${TABLE_TRACKED_CROPS} (
                    ${COLUMN_TC_ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                    ${COLUMN_TC_NAME} TEXT,
                    ${COLUMN_TC_PLANTED_DATE} TEXT,
                    ${COLUMN_TC_HARVEST_DATE} TEXT
                )
            """.trimIndent())
        }
    }

    private fun createCommunityTables(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS community_posts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                avatar TEXT,
                name TEXT,
                state TEXT,
                time TEXT,
                content TEXT,
                likes INTEGER DEFAULT 0,
                is_liked INTEGER DEFAULT 0,
                is_my_post INTEGER DEFAULT 0
            )
        """.trimIndent())
        
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS community_comments (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                post_id INTEGER,
                avatar TEXT,
                name TEXT,
                content TEXT,
                time TEXT,
                is_my_comment INTEGER DEFAULT 0
            )
        """.trimIndent())
    }

    // Helper to ensure tables are always created
    fun ensureCommunityTables() {
        val db = writableDatabase
        createCommunityTables(db)
        try {
            db.execSQL("ALTER TABLE community_posts ADD COLUMN image_uri TEXT")
        } catch (e: Exception) {
            // Already exists or table not created yet
        }
    }

    // --- Profile Operations ---
    fun saveProfile(name: String, age: String, crops: String) {
        val db = writableDatabase
        val cursor = db.query(TABLE_PROFILE, null, "$COLUMN_PROFILE_ID = 1", null, null, null, null)
        val exists = cursor.count > 0
        cursor.close()

        val values = ContentValues().apply {
            put(COLUMN_PROFILE_NAME, name)
            put(COLUMN_PROFILE_AGE, age)
            put(COLUMN_PROFILE_CROPS, crops)
        }

        if (exists) {
            db.update(TABLE_PROFILE, values, "$COLUMN_PROFILE_ID = 1", null)
        } else {
            values.put(COLUMN_PROFILE_ID, 1)
            db.insert(TABLE_PROFILE, null, values)
        }
    }

    fun saveLocation(location: String, isGps: Boolean) {
        val db = writableDatabase
        val cursor = db.query(TABLE_PROFILE, null, "$COLUMN_PROFILE_ID = 1", null, null, null, null)
        val exists = cursor.count > 0
        cursor.close()

        val values = ContentValues().apply {
            put(COLUMN_PROFILE_LOCATION, location)
            put(COLUMN_PROFILE_GPS, if (isGps) 1 else 0)
        }

        if (exists) {
            db.update(TABLE_PROFILE, values, "$COLUMN_PROFILE_ID = 1", null)
        } else {
            values.put(COLUMN_PROFILE_ID, 1)
            values.put(COLUMN_PROFILE_NAME, "Rajesh Kumar")
            values.put(COLUMN_PROFILE_AGE, "35")
            values.put(COLUMN_PROFILE_CROPS, "Tomato, Rice, Wheat")
            db.insert(TABLE_PROFILE, null, values)
        }
    }

    fun getProfile(): Map<String, String> {
        val db = readableDatabase
        val cursor = db.query(TABLE_PROFILE, null, "$COLUMN_PROFILE_ID = 1", null, null, null, null)
        val profile = mutableMapOf<String, String>()
        if (cursor.moveToFirst()) {
            profile["name"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PROFILE_NAME)) ?: ""
            profile["age"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PROFILE_AGE)) ?: ""
            profile["crops"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PROFILE_CROPS)) ?: ""
            profile["location"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PROFILE_LOCATION)) ?: ""
            profile["gps"] = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PROFILE_GPS)).toString()
        }
        cursor.close()
        return profile
    }

    // --- Schedule Operations ---
    fun addSchedule(crop: String, type: String, detail: String, date: String, time: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_SCHED_CROP, crop)
            put(COLUMN_SCHED_TYPE, type)
            put(COLUMN_SCHED_DETAIL, detail)
            put(COLUMN_SCHED_DATE, date)
            put(COLUMN_SCHED_TIME, time)
        }
        db.insert(TABLE_SCHEDULE, null, values)
    }

    fun getSchedules(): List<Map<String, String>> {
        val db = readableDatabase
        val cursor = db.query(TABLE_SCHEDULE, null, null, null, null, null, "$COLUMN_SCHED_ID DESC")
        val list = mutableListOf<Map<String, String>>()
        while (cursor.moveToNext()) {
            val item = mapOf(
                "id" to cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SCHED_ID)).toString(),
                "crop" to (cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SCHED_CROP)) ?: ""),
                "type" to (cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SCHED_TYPE)) ?: ""),
                "detail" to (cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SCHED_DETAIL)) ?: ""),
                "date" to (cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SCHED_DATE)) ?: ""),
                "time" to (cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SCHED_TIME)) ?: "")
            )
            list.add(item)
        }
        cursor.close()
        return list
    }

    fun deleteSchedule(id: Int): Int {
        val db = writableDatabase
        return db.delete(TABLE_SCHEDULE, "$COLUMN_SCHED_ID = ?", arrayOf(id.toString()))
    }

    // --- History Operations ---
    fun addHistory(imagePath: String, crop: String, disease: String, confidence: String) {
        val db = writableDatabase
        val dateFormat = java.text.SimpleDateFormat("MMMM dd, yyyy hh:mm a", java.util.Locale.getDefault())
        val timestamp = dateFormat.format(java.util.Date())

        val values = ContentValues().apply {
            put(COLUMN_HIST_IMAGE, imagePath)
            put(COLUMN_HIST_CROP, crop)
            put(COLUMN_HIST_DISEASE, disease)
            put(COLUMN_HIST_CONFIDENCE, confidence)
            put(COLUMN_HIST_TIMESTAMP, timestamp)
        }
        db.insert(TABLE_HISTORY, null, values)
    }

    fun getHistory(): List<Map<String, String>> {
        val db = readableDatabase
        val cursor = db.query(TABLE_HISTORY, null, null, null, null, null, "$COLUMN_HIST_ID DESC")
        val list = mutableListOf<Map<String, String>>()
        while (cursor.moveToNext()) {
            val item = mapOf(
                "id" to cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_HIST_ID)).toString(),
                "image" to (cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_HIST_IMAGE)) ?: ""),
                "crop" to (cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_HIST_CROP)) ?: ""),
                "disease" to (cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_HIST_DISEASE)) ?: ""),
                "confidence" to (cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_HIST_CONFIDENCE)) ?: ""),
                "timestamp" to (cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_HIST_TIMESTAMP)) ?: "")
            )
            list.add(item)
        }
        cursor.close()
        return list
    }

    fun clearHistory() {
        val db = writableDatabase
        db.delete(TABLE_HISTORY, null, null)
    }

    fun seedMockHistory() {
        val db = writableDatabase
        val dateFormat = java.text.SimpleDateFormat("MMMM dd, yyyy hh:mm a", java.util.Locale.US)
        
        // 1. Today (within last 24h)
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.HOUR_OF_DAY, -2) // 2 hours ago
        val dateToday = dateFormat.format(calendar.time)
        
        // 2. 2 days ago (within 7 days)
        calendar.time = java.util.Date()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -2)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 10)
        val date2DaysAgo = dateFormat.format(calendar.time)
        
        // 3. 5 days ago (within 7 days)
        calendar.time = java.util.Date()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -5)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 14)
        val date5DaysAgo = dateFormat.format(calendar.time)
        
        // 4. 12 days ago (within 30 days)
        calendar.time = java.util.Date()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -12)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 9)
        val date12DaysAgo = dateFormat.format(calendar.time)
        
        // 5. 20 days ago (within 30 days)
        calendar.time = java.util.Date()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -20)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 16)
        val date20DaysAgo = dateFormat.format(calendar.time)

        // Seed items
        addHistoryDirect(db, "", "Tomato", "healthy", "98% Confidence", dateToday)
        addHistoryDirect(db, "", "Wheat", "Early Blight", "94% Confidence", date2DaysAgo)
        addHistoryDirect(db, "", "Rice", "healthy", "96% Confidence", date5DaysAgo)
        addHistoryDirect(db, "", "Tomato", "Late Blight", "91% Confidence", date12DaysAgo)
        addHistoryDirect(db, "", "Rice", "Bacterial Spot", "89% Confidence", date20DaysAgo)
    }

    private fun addHistoryDirect(db: SQLiteDatabase, imagePath: String, crop: String, disease: String, confidence: String, timestamp: String) {
        val values = ContentValues().apply {
            put(COLUMN_HIST_IMAGE, imagePath)
            put(COLUMN_HIST_CROP, crop)
            put(COLUMN_HIST_DISEASE, disease)
            put(COLUMN_HIST_CONFIDENCE, confidence)
            put(COLUMN_HIST_TIMESTAMP, timestamp)
        }
        db.insert(TABLE_HISTORY, null, values)
    }

    fun parseTimestamp(timestampStr: String): java.util.Date? {
        val formats = arrayOf(
            java.text.SimpleDateFormat("MMMM dd, yyyy hh:mm a", java.util.Locale.US),
            java.text.SimpleDateFormat("MMMM dd, yyyy hh:mm a", java.util.Locale.ENGLISH),
            java.text.SimpleDateFormat("MMMM dd, yyyy hh:mm a", java.util.Locale.getDefault()),
            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        )
        for (format in formats) {
            try {
                return format.parse(timestampStr)
            } catch (e: Exception) {
                // continue
            }
        }
        return null
    }


    // --- Community Posts & Comments Operations ---
    fun addPost(avatar: String, name: String, state: String, time: String, content: String, isMyPost: Int, imageUri: String? = null): Long {
        ensureCommunityTables()
        val db = writableDatabase
        val values = ContentValues().apply {
            put("avatar", avatar)
            put("name", name)
            put("state", state)
            put("time", time)
            put("content", content)
            put("is_my_post", isMyPost)
            put("image_uri", imageUri)
        }
        return db.insert("community_posts", null, values)
    }

    fun getPosts(onlyMyPosts: Boolean = false): List<Map<String, Any>> {
        ensureCommunityTables()
        val db = readableDatabase
        val selection = if (onlyMyPosts) "is_my_post = 1" else null
        val cursor = db.query("community_posts", null, selection, null, null, null, "id DESC")
        val list = mutableListOf<Map<String, Any>>()
        while (cursor.moveToNext()) {
            val postId = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
            val item = mutableMapOf<String, Any>(
                "id" to postId,
                "avatar" to (cursor.getString(cursor.getColumnIndexOrThrow("avatar")) ?: ""),
                "name" to (cursor.getString(cursor.getColumnIndexOrThrow("name")) ?: ""),
                "state" to (cursor.getString(cursor.getColumnIndexOrThrow("state")) ?: ""),
                "time" to (cursor.getString(cursor.getColumnIndexOrThrow("time")) ?: ""),
                "content" to (cursor.getString(cursor.getColumnIndexOrThrow("content")) ?: ""),
                "likes" to cursor.getInt(cursor.getColumnIndexOrThrow("likes")),
                "is_liked" to cursor.getInt(cursor.getColumnIndexOrThrow("is_liked")),
                "is_my_post" to cursor.getInt(cursor.getColumnIndexOrThrow("is_my_post"))
            )
            try {
                val imgIndex = cursor.getColumnIndexOrThrow("image_uri")
                item["image_uri"] = cursor.getString(imgIndex) ?: ""
            } catch (e: Exception) {
                item["image_uri"] = ""
            }
            list.add(item)
        }
        cursor.close()
        return list
    }

    fun updatePostLikes(postId: Int, likes: Int, isLiked: Int) {
        ensureCommunityTables()
        val db = writableDatabase
        val values = ContentValues().apply {
            put("likes", likes)
            put("is_liked", isLiked)
        }
        db.update("community_posts", values, "id = ?", arrayOf(postId.toString()))
    }

    fun addComment(postId: Int, avatar: String, name: String, content: String, time: String, isMyComment: Int): Long {
        ensureCommunityTables()
        val db = writableDatabase
        val values = ContentValues().apply {
            put("post_id", postId)
            put("avatar", avatar)
            put("name", name)
            put("content", content)
            put("time", time)
            put("is_my_comment", isMyComment)
        }
        return db.insert("community_comments", null, values)
    }

    fun getComments(postId: Int): List<Map<String, String>> {
        ensureCommunityTables()
        val db = readableDatabase
        val cursor = db.query("community_comments", null, "post_id = ?", arrayOf(postId.toString()), null, null, "id ASC")
        val list = mutableListOf<Map<String, String>>()
        while (cursor.moveToNext()) {
            val item = mapOf(
                "id" to cursor.getInt(cursor.getColumnIndexOrThrow("id")).toString(),
                "post_id" to cursor.getInt(cursor.getColumnIndexOrThrow("post_id")).toString(),
                "avatar" to (cursor.getString(cursor.getColumnIndexOrThrow("avatar")) ?: ""),
                "name" to (cursor.getString(cursor.getColumnIndexOrThrow("name")) ?: ""),
                "content" to (cursor.getString(cursor.getColumnIndexOrThrow("content")) ?: ""),
                "time" to (cursor.getString(cursor.getColumnIndexOrThrow("time")) ?: ""),
                "is_my_comment" to cursor.getInt(cursor.getColumnIndexOrThrow("is_my_comment")).toString()
            )
            list.add(item)
        }
        cursor.close()
        return list
    }

    fun getCommentsOnMyPosts(): List<Map<String, String>> {
        ensureCommunityTables()
        val db = readableDatabase
        val query = """
            SELECT c.id, c.post_id, c.avatar, c.name, c.content, c.time, c.is_my_comment, p.content as post_content, p.name as post_author
            FROM community_comments c 
            JOIN community_posts p ON c.post_id = p.id 
            WHERE p.is_my_post = 1 AND c.is_my_comment = 0
            ORDER BY c.id DESC
        """.trimIndent()
        val cursor = db.rawQuery(query, null)
        val list = mutableListOf<Map<String, String>>()
        while (cursor.moveToNext()) {
            val item = mapOf(
                "id" to cursor.getInt(cursor.getColumnIndexOrThrow("id")).toString(),
                "post_id" to cursor.getInt(cursor.getColumnIndexOrThrow("post_id")).toString(),
                "avatar" to (cursor.getString(cursor.getColumnIndexOrThrow("avatar")) ?: ""),
                "name" to (cursor.getString(cursor.getColumnIndexOrThrow("name")) ?: ""),
                "content" to (cursor.getString(cursor.getColumnIndexOrThrow("content")) ?: ""),
                "time" to (cursor.getString(cursor.getColumnIndexOrThrow("time")) ?: ""),
                "is_my_comment" to cursor.getInt(cursor.getColumnIndexOrThrow("is_my_comment")).toString(),
                "post_content" to (cursor.getString(cursor.getColumnIndexOrThrow("post_content")) ?: ""),
                "post_author" to (cursor.getString(cursor.getColumnIndexOrThrow("post_author")) ?: "")
            )
            list.add(item)
        }
        cursor.close()
        return list
    }

    // --- Tracked Crops Operations ---
    fun ensureTrackedCropsTable() {
        val db = writableDatabase
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_TRACKED_CROPS (
                $COLUMN_TC_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TC_NAME TEXT,
                $COLUMN_TC_PLANTED_DATE TEXT,
                $COLUMN_TC_HARVEST_DATE TEXT
            )
        """.trimIndent())
    }

    fun addTrackedCrop(cropName: String, plantedDate: String, expectedHarvestDate: String): Long {
        ensureTrackedCropsTable()
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TC_NAME, cropName)
            put(COLUMN_TC_PLANTED_DATE, plantedDate)
            put(COLUMN_TC_HARVEST_DATE, expectedHarvestDate)
        }
        return db.insert(TABLE_TRACKED_CROPS, null, values)
    }

    fun getTrackedCrops(): List<Map<String, String>> {
        ensureTrackedCropsTable()
        val db = readableDatabase
        val cursor = db.query(TABLE_TRACKED_CROPS, null, null, null, null, null, "$COLUMN_TC_ID DESC")
        val list = mutableListOf<Map<String, String>>()
        while (cursor.moveToNext()) {
            val item = mapOf(
                "id" to cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TC_ID)).toString(),
                "crop_name" to (cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TC_NAME)) ?: ""),
                "planted_date" to (cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TC_PLANTED_DATE)) ?: ""),
                "expected_harvest_date" to (cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TC_HARVEST_DATE)) ?: "")
            )
            list.add(item)
        }
        cursor.close()
        return list
    }

    fun deleteTrackedCrop(id: Int): Int {
        ensureTrackedCropsTable()
        val db = writableDatabase
        return db.delete(TABLE_TRACKED_CROPS, "$COLUMN_TC_ID = ?", arrayOf(id.toString()))
    }

    // --- Notifications Operations ---
    fun ensureNotificationsTable() {
        val db = writableDatabase
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS user_notifications (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT,
                message TEXT,
                category TEXT,
                timestamp TEXT
            )
        """.trimIndent())
    }

    fun addNotification(title: String, message: String, category: String, timestamp: String): Long {
        ensureNotificationsTable()
        val db = writableDatabase
        val values = ContentValues().apply {
            put("title", title)
            put("message", message)
            put("category", category)
            put("timestamp", timestamp)
        }
        return db.insert("user_notifications", null, values)
    }

    fun getUserNotifications(): List<Map<String, String>> {
        ensureNotificationsTable()
        val db = readableDatabase
        val cursor = db.query("user_notifications", null, null, null, null, null, "id DESC")
        val list = mutableListOf<Map<String, String>>()
        while (cursor.moveToNext()) {
            val item = mapOf(
                "id" to cursor.getInt(cursor.getColumnIndexOrThrow("id")).toString(),
                "title" to (cursor.getString(cursor.getColumnIndexOrThrow("title")) ?: ""),
                "message" to (cursor.getString(cursor.getColumnIndexOrThrow("message")) ?: ""),
                "category" to (cursor.getString(cursor.getColumnIndexOrThrow("category")) ?: ""),
                "timestamp" to (cursor.getString(cursor.getColumnIndexOrThrow("timestamp")) ?: "")
            )
            list.add(item)
        }
        cursor.close()
        return list
    }
}

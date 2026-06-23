package com.example.agroassist

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class NotificationAdapter(
    private val items: List<NotificationItem>
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardNotification: CardView = view.findViewById(R.id.cardNotification)
        val ivNotificationIcon: ImageView = view.findViewById(R.id.ivNotificationIcon)
        val tvNotificationTitle: TextView = view.findViewById(R.id.tvNotificationTitle)
        val tvNotificationMessage: TextView = view.findViewById(R.id.tvNotificationMessage)
        val tvNotificationTime: TextView = view.findViewById(R.id.tvNotificationTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvNotificationTitle.text = item.title
        holder.tvNotificationMessage.text = item.message
        holder.tvNotificationTime.text = item.time

        val context = holder.itemView.context

        // Style elements based on category
        when (item.category.lowercase(Locale.ROOT)) {
            "market" -> {
                holder.cardNotification.setCardBackgroundColor(Color.parseColor("#FFF3E0")) // Orange tint
                holder.ivNotificationIcon.setImageResource(android.R.drawable.ic_dialog_map)
                holder.ivNotificationIcon.setColorFilter(Color.parseColor("#F57C00"))
            }
            "scheme" -> {
                holder.cardNotification.setCardBackgroundColor(Color.parseColor("#E8EAF6")) // Indigo/purple tint
                holder.ivNotificationIcon.setImageResource(android.R.drawable.ic_menu_agenda)
                holder.ivNotificationIcon.setColorFilter(Color.parseColor("#3F51B5"))
            }
            "weather" -> {
                holder.cardNotification.setCardBackgroundColor(Color.parseColor("#E3F2FD")) // Blue tint
                holder.ivNotificationIcon.setImageResource(android.R.drawable.ic_dialog_alert)
                holder.ivNotificationIcon.setColorFilter(Color.parseColor("#1976D2"))
            }
            else -> { // operation
                holder.cardNotification.setCardBackgroundColor(Color.parseColor("#E8F5E9")) // Green tint
                holder.ivNotificationIcon.setImageResource(android.R.drawable.ic_dialog_info)
                holder.ivNotificationIcon.setColorFilter(Color.parseColor("#388E3C"))
            }
        }

        // Tap redirection mapping
        holder.cardNotification.setOnClickListener {
            val intent = when (item.category.lowercase(Locale.ROOT)) {
                "market" -> Intent(context, MarketPricesActivity::class.java)
                "scheme" -> Intent(context, GovSchemesActivity::class.java)
                "weather" -> Intent(context, WeatherDashboardActivity::class.java)
                else -> { // operation
                    if (item.title.lowercase(Locale.ROOT).contains("water")) {
                        Intent(context, WaterManagementActivity::class.java)
                    } else {
                        Intent(context, FertilizerScheduleActivity::class.java)
                    }
                }
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = items.size
}

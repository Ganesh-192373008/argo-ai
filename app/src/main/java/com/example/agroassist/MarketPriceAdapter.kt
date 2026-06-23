package com.example.agroassist

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class MarketPriceAdapter(
    private var allItems: List<CropPriceItem>,
    private val onItemClick: (CropPriceItem) -> Unit
) : RecyclerView.Adapter<MarketPriceAdapter.ViewHolder>() {

    private var filteredItems: List<CropPriceItem> = allItems

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardCrop: CardView = view.findViewById(R.id.cardCrop)
        val tvCropEmoji: TextView = view.findViewById(R.id.tvCropEmoji)
        val tvCropName: TextView = view.findViewById(R.id.tvCropName)
        val tvCropPrice: TextView = view.findViewById(R.id.tvCropPrice)
        val tvCropChange: TextView = view.findViewById(R.id.tvCropChange)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_market_price, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = filteredItems[position]
        holder.tvCropEmoji.text = item.emoji
        holder.tvCropEmoji.backgroundTintList = android.content.res.ColorStateList.valueOf(
            Color.parseColor(item.bgTint)
        )
        holder.tvCropName.text = item.name
        holder.tvCropPrice.text = item.price
        holder.tvCropChange.text = item.change

        when (item.trend.lowercase(Locale.ROOT)) {
            "up" -> {
                holder.tvCropChange.setTextColor(Color.parseColor("#2E7D32"))
                holder.tvCropChange.setBackgroundColor(Color.parseColor("#E8F5E9"))
            }
            "down" -> {
                holder.tvCropChange.setTextColor(Color.parseColor("#C62828"))
                holder.tvCropChange.setBackgroundColor(Color.parseColor("#FFEBEE"))
            }
            else -> {
                holder.tvCropChange.setTextColor(Color.parseColor("#757575"))
                holder.tvCropChange.setBackgroundColor(Color.parseColor("#EEEEEE"))
            }
        }

        holder.cardCrop.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = filteredItems.size

    fun filter(query: String) {
        val q = query.lowercase(Locale.ROOT)
        filteredItems = if (q.isEmpty()) {
            allItems
        } else {
            allItems.filter { 
                it.name.lowercase(Locale.ROOT).contains(q)
            }
        }
        notifyDataSetChanged()
    }

    fun updateData(newItems: List<CropPriceItem>) {
        this.allItems = newItems
        this.filteredItems = newItems
        notifyDataSetChanged()
    }
}

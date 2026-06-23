package com.example.agroassist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class RecommendedProductAdapter(
    private val products: List<RecommendedProduct>,
    private val onCardClick: (RecommendedProduct) -> Unit,
    private val onAddClick: (RecommendedProduct) -> Unit
) : RecyclerView.Adapter<RecommendedProductAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardProduct: CardView = view.findViewById(R.id.cardProduct)
        val ivProductImage: ImageView = view.findViewById(R.id.ivProductImage)
        val tvProductName: TextView = view.findViewById(R.id.tvProductName)
        val tvProductBrand: TextView = view.findViewById(R.id.tvProductBrand)
        val tvProductRating: TextView = view.findViewById(R.id.tvProductRating)
        val tvProductPrice: TextView = view.findViewById(R.id.tvProductPrice)
        val btnProductAdd: Button = view.findViewById(R.id.btnProductAdd)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recommended_product, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = products[position]
        holder.tvProductName.text = product.name
        holder.tvProductBrand.text = product.brand
        holder.tvProductRating.text = product.rating
        holder.tvProductPrice.text = "₹${product.price}"
        holder.ivProductImage.setImageResource(product.imageResId)

        holder.cardProduct.setOnClickListener {
            onCardClick(product)
        }

        holder.btnProductAdd.setOnClickListener {
            onAddClick(product)
        }
    }

    override fun getItemCount(): Int = products.size
}

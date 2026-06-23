package com.example.agroassist

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CartActivity : AppCompatActivity() {

    private var quantity = 1
    private var productName = "Copper Oxychloride 50% WP"
    private var pricePerUnit = 450
    private var productImageResId = R.drawable.copper_oxychloride
    private val deliveryCharge = 50

    private lateinit var tvItemsCount: TextView
    private lateinit var cardCartItem: View
    private lateinit var btnMinus: TextView
    private lateinit var tvQuantity: TextView
    private lateinit var btnPlus: TextView
    private lateinit var tvItemSubtotal: TextView
    private lateinit var cardOrderSummary: View
    private lateinit var tvOrderSubtotalLabel: TextView
    private lateinit var tvOrderSubtotal: TextView
    private lateinit var tvOrderTotal: TextView
    private lateinit var tvEmptyCart: TextView
    private lateinit var btnCheckout: Button
    private lateinit var bottomBar: View

    private lateinit var tvCartItemName: TextView
    private lateinit var tvCartItemPrice: TextView
    private lateinit var ivCartItemImage: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        // Read intent data with default fallbacks
        productName = intent.getStringExtra("product_name") ?: "Copper Oxychloride 50% WP"
        pricePerUnit = intent.getIntExtra("product_price", 450)
        productImageResId = intent.getIntExtra("product_image", R.drawable.copper_oxychloride)

        // Bind views
        val backButton = findViewById<ImageView>(R.id.backButton)
        val btnClearAll = findViewById<TextView>(R.id.btnClearAll)
        btnCheckout = findViewById(R.id.btnCheckout)
        tvItemsCount = findViewById(R.id.tvItemsCount)
        cardCartItem = findViewById(R.id.cardCartItem)
        btnMinus = findViewById(R.id.btnMinus)
        tvQuantity = findViewById(R.id.tvQuantity)
        btnPlus = findViewById(R.id.btnPlus)
        tvItemSubtotal = findViewById(R.id.tvItemSubtotal)
        cardOrderSummary = findViewById(R.id.cardOrderSummary)
        tvOrderSubtotalLabel = findViewById(R.id.tvOrderSubtotalLabel)
        tvOrderSubtotal = findViewById(R.id.tvOrderSubtotal)
        tvOrderTotal = findViewById(R.id.tvOrderTotal)
        tvEmptyCart = findViewById(R.id.tvEmptyCart)
        bottomBar = findViewById(R.id.bottomBar)

        tvCartItemName = findViewById(R.id.tvCartItemName)
        tvCartItemPrice = findViewById(R.id.tvCartItemPrice)
        ivCartItemImage = findViewById(R.id.ivCartItemImage)

        backButton.setOnClickListener { finish() }
        
        btnPlus.setOnClickListener {
            quantity++
            updateCartUI()
        }

        btnMinus.setOnClickListener {
            if (quantity > 0) {
                quantity--
                updateCartUI()
            }
        }

        btnClearAll.setOnClickListener {
            if (quantity > 0) {
                quantity = 0
                updateCartUI()
                Toast.makeText(this, "Cart cleared!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Cart is already empty", Toast.LENGTH_SHORT).show()
            }
        }

        btnCheckout.setOnClickListener {
            if (quantity > 0) {
                val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
                val view = layoutInflater.inflate(R.layout.dialog_checkout_chooser, null)
                
                val tvCheckoutProductName = view.findViewById<TextView>(R.id.tvCheckoutProductName)
                val btnCloseCheckout = view.findViewById<ImageView>(R.id.btnCloseCheckout)
                val btnCheckoutAmazon = view.findViewById<LinearLayout>(R.id.btnCheckoutAmazon)
                val btnCheckoutFlipkart = view.findViewById<LinearLayout>(R.id.btnCheckoutFlipkart)
                val btnCheckoutGoogle = view.findViewById<LinearLayout>(R.id.btnCheckoutGoogle)
                
                tvCheckoutProductName.text = "Choose where you want to view and purchase '$productName'."
                
                btnCloseCheckout.setOnClickListener { bottomSheetDialog.dismiss() }
                
                btnCheckoutAmazon.setOnClickListener {
                    bottomSheetDialog.dismiss()
                    try {
                        val searchUrl = "https://www.amazon.in/s?k=" + java.net.URLEncoder.encode(productName, "UTF-8")
                        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(searchUrl)).apply {
                            setPackage("com.amazon.mShop.android.shopping")
                        }
                        startActivity(intent)
                    } catch (e: Exception) {
                        try {
                            val searchUrl = "https://www.amazon.in/s?k=" + java.net.URLEncoder.encode(productName, "UTF-8")
                            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(searchUrl))
                            startActivity(intent)
                        } catch (ex: Exception) {
                            Toast.makeText(this@CartActivity, "Error opening browser", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                
                btnCheckoutFlipkart.setOnClickListener {
                    bottomSheetDialog.dismiss()
                    try {
                        val searchUrl = "https://www.flipkart.com/search?q=" + java.net.URLEncoder.encode(productName, "UTF-8")
                        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(searchUrl)).apply {
                            setPackage("com.flipkart.android")
                        }
                        startActivity(intent)
                    } catch (e: Exception) {
                        try {
                            val searchUrl = "https://www.flipkart.com/search?q=" + java.net.URLEncoder.encode(productName, "UTF-8")
                            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(searchUrl))
                            startActivity(intent)
                        } catch (ex: Exception) {
                            Toast.makeText(this@CartActivity, "Error opening browser", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                
                btnCheckoutGoogle.setOnClickListener {
                    bottomSheetDialog.dismiss()
                    try {
                        val searchUrl = "https://www.google.com/search?tbm=shop&q=" + java.net.URLEncoder.encode(productName, "UTF-8")
                        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(searchUrl))
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(this@CartActivity, "Error opening browser", Toast.LENGTH_SHORT).show()
                    }
                }
                
                bottomSheetDialog.setContentView(view)
                bottomSheetDialog.show()
            } else {
                Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show()
            }
        }

        // Initialize UI
        updateCartUI()
    }

    private fun updateCartUI() {
        val itemSubtotal = quantity * pricePerUnit
        val totalAmount = itemSubtotal + deliveryCharge

        tvCartItemName.text = productName
        tvCartItemPrice.text = "₹$pricePerUnit per unit"
        ivCartItemImage.setImageResource(productImageResId)

        tvQuantity.text = quantity.toString()
        tvItemSubtotal.text = "₹$itemSubtotal"
        tvOrderSubtotalLabel.text = "Subtotal ($quantity items)"
        tvOrderSubtotal.text = "₹$itemSubtotal"

        if (quantity > 0) {
            tvItemsCount.text = "$quantity items"
            tvOrderTotal.text = "₹$totalAmount"
            cardCartItem.visibility = View.VISIBLE
            cardOrderSummary.visibility = View.VISIBLE
            tvEmptyCart.visibility = View.GONE
            bottomBar.visibility = View.VISIBLE
            btnCheckout.isEnabled = true
        } else {
            tvItemsCount.text = "0 items"
            tvOrderTotal.text = "₹0"
            cardCartItem.visibility = View.GONE
            cardOrderSummary.visibility = View.GONE
            tvEmptyCart.visibility = View.VISIBLE
            bottomBar.visibility = View.GONE
            btnCheckout.isEnabled = false
        }
    }
}


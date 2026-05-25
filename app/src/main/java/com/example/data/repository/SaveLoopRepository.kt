package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.local.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SaveLoopRepository(private val context: Context) {

    private val db = SaveLoopDatabase.getDatabase(context)
    private val dao = db.saveLoopDao()

    // --- Exposures ---
    val allCoupons: Flow<List<CouponEntity>> = dao.getAllCoupons()
    val favoriteCoupons: Flow<List<CouponEntity>> = dao.getFavoriteCoupons()
    val savingsRecords: Flow<List<SavingsRecordEntity>> = dao.getAllSavingsRecords()
    val activityLogs: Flow<List<ActivityLogEntity>> = dao.getAllActivityLogs()
    val chatMessages: Flow<List<ChatMessageEntity>> = dao.getAllChatMessages()
    val couponCount: Flow<Int> = dao.getCouponCount()

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // --- Prepopulate Initial Realistic Data ---
    suspend fun checkAndPrepopulate() {
        val count = dao.getCouponCount().firstOrNull() ?: 0
        if (count == 0) {
            Log.d("SaveLoopRepository", "Prepopulating initial SaveLoop fintech data...")
            
            // 1. Initial Coupons
            val initialCoupons = listOf(
                CouponEntity(
                    merchantName = "Zomato",
                    code = "ZOMATO150",
                    discountValue = "₹150 OFF",
                    minOrderValue = "On orders above ₹499",
                    expiryDate = "Expires in 2 hours",
                    category = "Food",
                    cashbackInfo = "Up to ₹50 Cashback",
                    isFavorite = true,
                    termsAndConditions = "Valid on orders placed through partner restaurants. Pay using any credit/debit card."
                ),
                CouponEntity(
                    merchantName = "Swiggy",
                    code = "SWIGGY100",
                    discountValue = "₹100 OFF",
                    minOrderValue = "On orders above ₹399",
                    expiryDate = "Expires in 5 hours",
                    category = "Food",
                    cashbackInfo = "₹25 wallet return",
                    isFavorite = false,
                    termsAndConditions = "Min order value is ₹399. Applicable once per user."
                ),
                CouponEntity(
                    merchantName = "Amazon",
                    code = "AMZNSAVE10",
                    discountValue = "10% OFF",
                    minOrderValue = "On min order ₹1999",
                    expiryDate = "Expires in 2 days",
                    category = "Shopping",
                    cashbackInfo = "Up to ₹300 Reward",
                    isFavorite = true,
                    termsAndConditions = "Applicable across Electronics and Fashion categories. Minimum order value ₹1999."
                ),
                CouponEntity(
                    merchantName = "Myntra",
                    code = "MYNTRA300",
                    discountValue = "₹300 OFF",
                    minOrderValue = "On orders above ₹1699",
                    expiryDate = "Expires soon",
                    category = "Shopping",
                    cashbackInfo = "Free shipping",
                    isFavorite = false,
                    termsAndConditions = "Valid on apparel items. Excludes innerwear and gold/silver products."
                ),
                CouponEntity(
                    merchantName = "Irctc",
                    code = "TRAINDELIGHT",
                    discountValue = "₹120 OFF",
                    minOrderValue = "No minimum purchase",
                    expiryDate = "Expires in 3 days",
                    category = "Travel",
                    cashbackInfo = "5% extra Paytm loyalty",
                    isFavorite = false
                ),
                CouponEntity(
                    merchantName = "Netflix",
                    code = "CINESTREAM",
                    discountValue = "15% Cashback",
                    minOrderValue = "On monthly subscription",
                    expiryDate = "Expires in 1 week",
                    category = "Entertainment",
                    cashbackInfo = "Credted as cashback",
                    isFavorite = false
                )
            )
            for (coupon in initialCoupons) {
                dao.insertCoupon(coupon)
            }

            // 2. Initial Savings Records
            val initialSavings = listOf(
                SavingsRecordEntity(
                    merchantName = "Zomato",
                    amountSaved = 150.0,
                    cashbackEarned = 50.0,
                    couponApplied = "ZOMATO150",
                    timestamp = System.currentTimeMillis() - 86400000 * 3, // 3 days ago
                    category = "Food"
                ),
                SavingsRecordEntity(
                    merchantName = "Swiggy",
                    amountSaved = 100.0,
                    cashbackEarned = 25.0,
                    couponApplied = "SWIGGY100",
                    timestamp = System.currentTimeMillis() - 86400000 * 10,
                    category = "Food"
                ),
                SavingsRecordEntity(
                    merchantName = "Amazon",
                    amountSaved = 450.0,
                    cashbackEarned = 120.0,
                    couponApplied = "AMZNSAVE10",
                    timestamp = System.currentTimeMillis() - 86400000 * 15,
                    category = "Shopping"
                ),
                SavingsRecordEntity(
                    merchantName = "Myntra",
                    amountSaved = 300.0,
                    cashbackEarned = 0.0,
                    couponApplied = "MYNTRA300",
                    timestamp = System.currentTimeMillis() - 86400000 * 25,
                    category = "Shopping"
                ),
                SavingsRecordEntity(
                    merchantName = "Uber",
                    amountSaved = 80.0,
                    cashbackEarned = 15.0,
                    couponApplied = "UBERGO",
                    timestamp = System.currentTimeMillis() - 86400000 * 2,
                    category = "Travel"
                )
            )
            for (record in initialSavings) {
                dao.insertSavingsRecord(record)
            }

            // 3. Initial Activity Logs
            val initialLogs = listOf(
                ActivityLogEntity(
                    title = "Zomato coupon copied",
                    description = "Copied code ZOMATO150 successfully.",
                    timestamp = System.currentTimeMillis() - 3600000 * 2, // 2 hours ago
                    type = "copied"
                ),
                ActivityLogEntity(
                    title = "Savings of ₹200 Recorded",
                    description = "Applied Uber ride coupon successfully.",
                    timestamp = System.currentTimeMillis() - 3600000 * 24, // 1 day ago
                    type = "applied"
                ),
                ActivityLogEntity(
                    title = "New screenshot scanned",
                    description = "Extracted 2 active codes from food screenshot.",
                    timestamp = System.currentTimeMillis() - 3600000 * 48, // 2 days ago
                    type = "scanned"
                )
            )
            for (log in initialLogs) {
                dao.insertActivityLog(log)
            }

            // 4. Initial Greeting from AI Chat
            dao.insertChatMessage(
                ChatMessageEntity(
                    message = "Hi Terry! I'm your SaveLoop AI Assistant. Ask me anything like:\n\n• \"Best coupon for pizza?\"\n• \"Any food offers today?\"\n• \"Best cashback for Amazon?\"",
                    isUser = false
                )
            )
        }
    }

    // --- Core Database DB Actions ---
    suspend fun addCoupon(coupon: CouponEntity) {
        dao.insertCoupon(coupon)
        logActivity("New Coupon added", "Added coupon code ${coupon.code} for ${coupon.merchantName}.", "copied")
    }

    suspend fun updateCoupon(coupon: CouponEntity) {
        dao.updateCoupon(coupon)
    }

    suspend fun toggleFavorite(id: Int, isFavorite: Boolean) {
        dao.toggleFavorite(id, isFavorite)
    }

    suspend fun archiveCoupon(id: Int) {
        dao.archiveCoupon(id)
    }

    suspend fun deleteCoupon(id: Int) {
        dao.deleteCouponById(id)
    }

    suspend fun logActivity(title: String, description: String, type: String) {
        dao.insertActivityLog(ActivityLogEntity(title = title, description = description, type = type))
    }

    suspend fun logSavings(merchant: String, amount: Double, cashback: Double, coupon: String, category: String) {
        dao.insertSavingsRecord(
            SavingsRecordEntity(
                merchantName = merchant,
                amountSaved = amount,
                cashbackEarned = cashback,
                couponApplied = coupon,
                category = category
            )
        )
        logActivity(
            "Saved ₹${amount.toInt()} with $merchant!",
            "Applied coupon $coupon during checkout.",
            "applied"
        )
    }

    suspend fun addChatMessage(message: String, isUser: Boolean, recommendedCouponCode: String? = null, recommendedMerchant: String? = null) {
        dao.insertChatMessage(ChatMessageEntity(message = message, isUser = isUser, recommendedCouponCode = recommendedCouponCode, recommendedMerchant = recommendedMerchant))
    }

    suspend fun clearChat() {
        dao.clearChatHistory()
        addChatMessage("Hi Terry! I'm your SaveLoop AI Assistant. How can I save with you today?", false)
    }

    // --- Advanced AI Assistant Integration with Offline Fallback ---
    suspend fun askAssistant(userPrompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.d("SaveLoopRepository", "API key missing or placeholder. Running advanced offline NLP matching...")
            return@withContext offlineAILogic(userPrompt)
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            
            val systemInstructions = "You are SaveLoop AI Assistant. Give premium, concise advice about coupons, shopping, and cashback options. " +
                    "Pretend you know local Indian apps like Swiggy, Zomato, PhonePe, Myntra, Paytm, and Amazon. " +
                    "Try to recommend specific imaginary or common codes (e.g. PIZZA50 for Swiggy/Zomato, AMZNSAVE10 for Amazon, TRAVELDEAL for MakeMyTrip) and explain how much the user will save. Keep replies short (under 4 lines) with clear emojis."

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", userPrompt) })
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", systemInstructions) })
                    })
                })
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext offlineAILogic(userPrompt)
                }
                val rawJson = response.body?.string() ?: return@withContext offlineAILogic(userPrompt)
                val jsonObject = JSONObject(rawJson)
                val candidates = jsonObject.getJSONArray("candidates")
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.getJSONObject("content")
                val parts = content.getJSONArray("parts")
                val textResponse = parts.getJSONObject(0).getString("text")
                return@withContext textResponse.trim()
            }
        } catch (e: Exception) {
            Log.e("SaveLoopRepository", "Error contacting Gemini API", e)
            return@withContext offlineAILogic(userPrompt)
        }
    }

    private fun offlineAILogic(prompt: String): String {
        val query = prompt.lowercase()
        return when {
            query.contains("pizza") || query.contains("food") || query.contains("zomato") || query.contains("swiggy") -> {
                "🍕 Swiggy & Zomato have active codes!\n• **ZOMATO150**: ₹150 OFF (orders above ₹499).\n• **SWIGGY100**: ₹100 OFF (orders above ₹399).\n\n*Pro-tip*: ZOMATO150 gets you up to ₹50 cashback extra via Amazon Pay!"
            }
            query.contains("amazon") || query.contains("shopping") || query.contains("myntra") -> {
                "🛍️ Best Shopping Deals Found:\n• **AMZNSAVE10** on Amazon: Save 10% on minimum purchases of ₹1,999.\n• **MYNTRA300** on Myntra: Save ₹300 on orders above ₹1,699.\n\nEnjoy guaranteed additional 2% cashback via integrated UPI!"
            }
            query.contains("cashback") || query.contains("pay") || query.contains("wallet") -> {
                "💳 Payment Rewards Activated:\n• Use Swiggy with PhonePe wallet to get ₹25 flat reward.\n• Airtel Payments Bank gets you 10% cash back on utility bills up to ₹150 this weekend!"
            }
            query.contains("travel") || query.contains("flight") || query.contains("uber") -> {
                "✈️ Travel Smart & Save:\n• Apply code **TRAINDELIGHT** on IRCTC to grab flat ₹120 off.\n• Use **UBERGO** to fetch flat ₹80 ride savings immediately."
            }
            else -> {
                "🤖 Hey Terry! I analyzed your wallet. Currently you have **6 active coupons** across Food and Shopping. \n\nI recommend using **ZOMATO150** today to claim ₹150 savings before its expiry."
            }
        }
    }

    // --- Mock OCR Screen Scanner Processing ---
    suspend fun performOcrScanningSimulate(): CouponEntity = withContext(Dispatchers.Default) {
        // Simulate a delay for OCR engine
        Thread.sleep(2000)
        
        // Randomly select one merchant to scan
        val scannedMerchants = listOf(
            CouponEntity(
                merchantName = "Uber",
                code = "UBERRIDE50",
                discountValue = "₹50 OFF",
                minOrderValue = "On bookings above ₹199",
                expiryDate = "Expires in 5 days",
                category = "Travel",
                cashbackInfo = "5% cashback"
            ),
            CouponEntity(
                merchantName = "Starbucks",
                code = "COFFEEFREE",
                discountValue = "Buy 1 Get 1",
                minOrderValue = "On select items",
                expiryDate = "Expires in 3 days",
                category = "Food",
                cashbackInfo = "Free stars loyalty"
            ),
            CouponEntity(
                merchantName = "Pharmeasy",
                code = "EASYMED20",
                discountValue = "20% OFF",
                minOrderValue = "On orders above ₹999",
                expiryDate = "Expires in 1 week",
                category = "Bills",
                cashbackInfo = "Up to ₹100 cashback"
            )
        )
        val selected = scannedMerchants.random()
        return@withContext selected
    }
}

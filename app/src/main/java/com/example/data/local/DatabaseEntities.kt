package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "coupons")
data class CouponEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val merchantName: String,
    val code: String,
    val discountValue: String,
    val minOrderValue: String,
    val expiryDate: String,
    val category: String, // Food, Shopping, Travel, Bills, Entertainment, Payments
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val isUsed: Boolean = false,
    val cashbackInfo: String = "",
    val termsAndConditions: String = "Applicable on select cards. Minimum order as specified. Cannot be clubbed with other offers."
)

@Entity(tableName = "savings_records")
data class SavingsRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val merchantName: String,
    val amountSaved: Double,
    val cashbackEarned: Double = 0.0,
    val couponApplied: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val category: String = "Food"
)

@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String // "applied", "copied", "scanned", "reminder"
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val message: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val recommendedCouponCode: String? = null,
    val recommendedMerchant: String? = null
)

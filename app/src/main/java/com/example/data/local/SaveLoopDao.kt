package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SaveLoopDao {

    // --- Coupons ---
    @Query("SELECT * FROM coupons WHERE isArchived = 0 ORDER BY isFavorite DESC, id DESC")
    fun getAllCoupons(): Flow<List<CouponEntity>>

    @Query("SELECT * FROM coupons WHERE isFavorite = 1 AND isArchived = 0")
    fun getFavoriteCoupons(): Flow<List<CouponEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoupon(coupon: CouponEntity)

    @Update
    suspend fun updateCoupon(coupon: CouponEntity)

    @Query("UPDATE coupons SET isFavorite = :isFav WHERE id = :id")
    suspend fun toggleFavorite(id: Int, isFav: Boolean)

    @Query("UPDATE coupons SET isArchived = 1 WHERE id = :id")
    suspend fun archiveCoupon(id: Int)

    @Query("DELETE FROM coupons WHERE id = :id")
    suspend fun deleteCouponById(id: Int)

    @Query("SELECT COUNT(*) FROM coupons WHERE isArchived = 0")
    fun getCouponCount(): Flow<Int>


    // --- Savings Records ---
    @Query("SELECT * FROM savings_records ORDER BY timestamp DESC")
    fun getAllSavingsRecords(): Flow<List<SavingsRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsRecord(record: SavingsRecordEntity)


    // --- Activity Logs ---
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC")
    fun getAllActivityLogs(): Flow<List<ActivityLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityLog(log: ActivityLogEntity)


    // --- Chat Messages ---
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllChatMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages")
    suspend fun clearChatHistory()
}

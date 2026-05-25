package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.CouponEntity
import com.example.data.local.SavingsRecordEntity
import com.example.data.repository.SaveLoopRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class AppScreen {
    SPLASH,
    ONBOARDING_1,
    ONBOARDING_2,
    ONBOARDING_3,
    AUTH,
    PERMISSIONS,
    INITIAL_SCAN,
    MAIN // Controls bottom navigation tabs internally
}

enum class NavigationTab {
    HOME,
    WALLET,
    AI_ASSISTANT,
    ACTIVITY,
    PROFILE
}

class SaveLoopViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SaveLoopRepository(application)

    // --- Screen Navigation States ---
    private val _currentScreen = MutableStateFlow(AppScreen.SPLASH)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _currentTab = MutableStateFlow(NavigationTab.HOME)
    val currentTab: StateFlow<NavigationTab> = _currentTab.asStateFlow()

    // --- Shared Database Flows ---
    val allCoupons = repository.allCoupons.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val favoriteCoupons = repository.favoriteCoupons.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val savingsRecords = repository.savingsRecords.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val activityLogs = repository.activityLogs.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val chatMessages = repository.chatMessages.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val couponCount = repository.couponCount.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0
    )

    // --- Overlay & Detail Dialog States ---
    private val _selectedCoupon = MutableStateFlow<CouponEntity?>(null)
    val selectedCoupon: StateFlow<CouponEntity?> = _selectedCoupon.asStateFlow()

    private val _isAddCouponOpen = MutableStateFlow(false)
    val isAddCouponOpen: StateFlow<Boolean> = _isAddCouponOpen.asStateFlow()

    private val _isOcrInProgress = MutableStateFlow(false)
    val isOcrInProgress: StateFlow<Boolean> = _isOcrInProgress.asStateFlow()

    private val _isCheckoutOverlayActive = MutableStateFlow(false)
    val isCheckoutOverlayActive: StateFlow<Boolean> = _isCheckoutOverlayActive.asStateFlow()

    private val _isNotificationsOpen = MutableStateFlow(false)
    val isNotificationsOpen: StateFlow<Boolean> = _isNotificationsOpen.asStateFlow()

    private val _isPremiumOpen = MutableStateFlow(false)
    val isPremiumOpen: StateFlow<Boolean> = _isPremiumOpen.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    // --- Checkout Simulation Data ---
    private val _checkoutSavingsEstimate = MutableStateFlow(150.0)
    val checkoutSavingsEstimate: StateFlow<Double> = _checkoutSavingsEstimate.asStateFlow()

    // --- Chat Helper States ---
    private val _chatLoading = MutableStateFlow(false)
    val chatLoading: StateFlow<Boolean> = _chatLoading.asStateFlow()

    init {
        // Prepopulate data reactively when ViewModel starts
        viewModelScope.launch {
            repository.checkAndPrepopulate()
        }
    }

    // --- State Actions ---
    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun selectTab(tab: NavigationTab) {
        _currentTab.value = tab
    }

    fun openCouponDetails(coupon: CouponEntity) {
        _selectedCoupon.value = coupon
    }

    fun closeCouponDetails() {
        _selectedCoupon.value = null
    }

    fun toggleAddCoupon(open: Boolean) {
        _isAddCouponOpen.value = open
    }

    fun toggleCheckoutOverlay(open: Boolean) {
        _isCheckoutOverlayActive.value = open
    }

    fun toggleNotificationsCenter(open: Boolean) {
        _isNotificationsOpen.value = open
    }

    fun togglePremiumPromo(open: Boolean) {
        _isPremiumOpen.value = open
    }

    fun toggleSettings(open: Boolean) {
        _isSettingsOpen.value = open
    }

    // --- Database Operations ---
    fun addManualCoupon(
        merchant: String,
        code: String,
        discount: String,
        minOrder: String,
        expiry: String,
        category: String,
        cashback: String
    ) {
        viewModelScope.launch {
            val coupon = CouponEntity(
                merchantName = merchant,
                code = code,
                discountValue = discount,
                minOrderValue = minOrder,
                expiryDate = expiry,
                category = category,
                cashbackInfo = cashback
            )
            repository.addCoupon(coupon)
            toggleAddCoupon(false)
        }
    }

    fun toggleFavoriteCoupon(coupon: CouponEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(coupon.id, !coupon.isFavorite)
        }
    }

    fun archiveCoupon(coupon: CouponEntity) {
        viewModelScope.launch {
            repository.archiveCoupon(coupon.id)
            closeCouponDetails()
        }
    }

    fun deleteCoupon(coupon: CouponEntity) {
        viewModelScope.launch {
            repository.deleteCoupon(coupon.id)
            closeCouponDetails()
        }
    }

    fun logActivity(title: String, description: String, type: String) {
        viewModelScope.launch {
            repository.logActivity(title, description, type)
        }
    }

    fun applyCouponAndRecordSavings(coupon: CouponEntity) {
        viewModelScope.launch {
            val discountAmount = when {
                coupon.discountValue.contains("150") -> 150.0
                coupon.discountValue.contains("100") -> 100.0
                coupon.discountValue.contains("300") -> 300.0
                coupon.discountValue.contains("50") -> 50.0
                coupon.discountValue.contains("10%") -> 200.0
                coupon.discountValue.contains("15%") -> 120.0
                coupon.discountValue.contains("20%") -> 240.0
                else -> 75.0
            }
            val cashbackAmount = when {
                coupon.cashbackInfo.contains("50") -> 50.0
                coupon.cashbackInfo.contains("25") -> 25.0
                coupon.cashbackInfo.contains("120") -> 120.0
                else -> 10.0
            }
            repository.logSavings(
                merchant = coupon.merchantName,
                amount = discountAmount,
                cashback = cashbackAmount,
                coupon = coupon.code,
                category = coupon.category
            )
        }
    }

    // --- AI Assistant Chat ---
    fun sendMessage(userText: String) {
        if (userText.trim().isEmpty()) return

        viewModelScope.launch {
            // Log user message
            repository.addChatMessage(userText, isUser = true)
            _chatLoading.value = true

            // Send to Gemini with offline fallback
            val aiResponse = repository.askAssistant(userText)

            // Look up if any coupon fits this merchant to pin a recommended card
            var recCode: String? = null
            var recMerchant: String? = null
            val currentCoupons = allCoupons.value
            for (coupon in currentCoupons) {
                if (userText.lowercase().contains(coupon.merchantName.lowercase())) {
                    recCode = coupon.code
                    recMerchant = coupon.merchantName
                    break
                }
            }

            // Log AI response
            repository.addChatMessage(aiResponse, isUser = false, recommendedCouponCode = recCode, recommendedMerchant = recMerchant)
            _chatLoading.value = false
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            repository.clearChat()
        }
    }

    // --- Simulated OCR scan ---
    fun scanScrreenshotOCRSimulate(onScanComplete: (CouponEntity) -> Unit) {
        viewModelScope.launch {
            _isOcrInProgress.value = true
            val scannedCoupon = repository.performOcrScanningSimulate()
            _isOcrInProgress.value = false
            repository.addCoupon(scannedCoupon)
            onScanComplete(scannedCoupon)
        }
    }

    // --- Profile Settings & Info ---
    private val _profileName = MutableStateFlow("Terry Melton")
    val profileName: StateFlow<String> = _profileName.asStateFlow()

    private val _profileEmail = MutableStateFlow("terry.melton@gmail.com")
    val profileEmail: StateFlow<String> = _profileEmail.asStateFlow()

    private val _profilePhotoUri = MutableStateFlow<String?>(null)
    val profilePhotoUri: StateFlow<String?> = _profilePhotoUri.asStateFlow()

    private val _profileAvatarIndex = MutableStateFlow(0) // 0 for default TM block, other indexes for preinstalled stylized dynamic avatars
    val profileAvatarIndex: StateFlow<Int> = _profileAvatarIndex.asStateFlow()

    private val _isProfileEditOpen = MutableStateFlow(false)
    val isProfileEditOpen: StateFlow<Boolean> = _isProfileEditOpen.asStateFlow()

    private val _smsAutoFeedEnabled = MutableStateFlow(true)
    val smsAutoFeedEnabled: StateFlow<Boolean> = _smsAutoFeedEnabled.asStateFlow()

    private val _connectedApps = MutableStateFlow(mapOf(
        "Swiggy" to true,
        "Zomato" to true,
        "Amazon India" to true,
        "Myntra" to true,
        "Flipkart" to true,
        "Nykaa" to false,
        "Ajio" to false,
        "Tata CLiQ" to false,
        "BigBasket" to false,
        "JioMart" to false,
        "Etsy" to false,
        "eBay" to false,
        "Walmart" to false,
        "Blinkit" to false,
        "Zepto" to false
    ))
    val connectedApps: StateFlow<Map<String, Boolean>> = _connectedApps.asStateFlow()

    fun toggleConnectedApp(brand: String) {
        val current = _connectedApps.value.toMutableMap()
        val newState = !(current[brand] ?: false)
        current[brand] = newState
        _connectedApps.value = current
        logActivity(
            "Security Settings Sync",
            "${brand} auto-feed listener is now ${if (newState) "ACTIVE" else "DISABLED"}.",
            "copied"
        )
    }

    data class SmsAlert(
        val sender: String,
        val body: String,
        val code: String,
        val merchant: String,
        val discount: String,
        val category: String,
        val cashback: String
    )

    private val _incomingSmsAlert = MutableStateFlow<SmsAlert?>(null)
    val incomingSmsAlert: StateFlow<SmsAlert?> = _incomingSmsAlert.asStateFlow()

    fun updateProfile(name: String, email: String) {
        _profileName.value = name
        _profileEmail.value = email
    }

    fun updateProfilePhoto(uri: String?) {
        _profilePhotoUri.value = uri
        _profileAvatarIndex.value = -1 // custom photo flag
    }

    fun selectAvatarIndex(index: Int) {
        _profileAvatarIndex.value = index
        _profilePhotoUri.value = null
    }

    fun setProfileEditOpen(open: Boolean) {
        _isProfileEditOpen.value = open
    }

    fun setSmsAutoFeedEnabled(enabled: Boolean) {
        _smsAutoFeedEnabled.value = enabled
    }

    fun dismissSmsAlert() {
        _incomingSmsAlert.value = null
    }

    fun receiveSimulatedSms(sender: String, messageText: String) {
        if (!_smsAutoFeedEnabled.value) return

        // Extract a coupon code: looking for alphanumeric ALL-CAPS words of 4 to 12 chars
        val words = messageText.split("\\s+".toRegex())
        var extractedCode = ""
        for (w in words) {
            val clean = w.replace("[,.!?;:\'\"]".toRegex(), "").trim()
            if (clean.length >= 4 && clean.length <= 12 && clean.all { it.isUpperCase() || it.isDigit() } && clean.any { it.isLetter() } && clean.any { it.isDigit() }) {
                extractedCode = clean
                break
            }
        }

        if (extractedCode.isEmpty()) {
            val codeRegex = Regex("[A-Z\\d]{4,10}")
            val match = codeRegex.find(messageText)
            if (match != null) {
                extractedCode = match.value
            }
        }

        // Search merchant
        var merchant = "Store"
        val merchantsList = listOf(
            "Zomato", "Swiggy", "Amazon", "Myntra", "Uber", "Dominos", "Pharmeasy", "Flipkart", "Starbucks",
            "Nykaa", "Ajio", "Tata CLiQ", "BigBasket", "JioMart", "Etsy", "eBay", "Target", "Walmart", 
            "AliExpress", "Sephora", "Blinkit", "Zepto", "BookMyShow", "MakeMyTrip"
        )
        for (m in merchantsList) {
            if (messageText.lowercase().contains(m.lowercase()) || sender.lowercase().contains(m.lowercase())) {
                merchant = m
                break
            }
        }

        // Extracted discount value
        var discount = "₹100 OFF"
        if (messageText.contains("50%")) discount = "50% OFF"
        else if (messageText.contains("30%")) discount = "30% OFF"
        else if (messageText.contains("10%")) discount = "10% OFF"
        else if (messageText.contains("20%")) discount = "20% OFF"
        else {
            val valueRegex = Regex("(?:₹|Rs\\.?\\s*)(\\d+)")
            val vMatch = valueRegex.find(messageText)
            if (vMatch != null) {
                discount = "₹${vMatch.groupValues[1]} OFF"
            }
        }

        val category = when(merchant.lowercase()) {
            "zomato", "swiggy", "dominos", "starbucks" -> "Food"
            "amazon", "myntra", "flipkart" -> "Shopping"
            "uber" -> "Travel"
            else -> "Bills"
        }

        if (extractedCode.isNotEmpty()) {
            _incomingSmsAlert.value = SmsAlert(
                sender = sender,
                body = messageText,
                code = extractedCode,
                merchant = merchant,
                discount = discount,
                category = category,
                cashback = "Simulated via Live SMS Monitor"
            )
        }
    }

    fun approveSmsCoupon(alert: SmsAlert) {
        viewModelScope.launch {
            val coupon = CouponEntity(
                merchantName = alert.merchant,
                code = alert.code,
                discountValue = alert.discount,
                minOrderValue = "Auto-scanned from SMS Notification",
                expiryDate = "Expires soon",
                category = alert.category,
                cashbackInfo = alert.cashback
            )
            repository.addCoupon(coupon)
            repository.logActivity(
                "SMS Code Auto-Feed",
                "Extracted and saved coupon ${alert.code} from SMS sender ${alert.sender}.",
                "scanned"
            )
            dismissSmsAlert()
        }
    }
}

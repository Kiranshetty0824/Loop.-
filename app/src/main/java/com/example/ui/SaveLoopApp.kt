package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.text.BasicTextField
import coil.compose.rememberAsyncImagePainter
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.CouponEntity
import com.example.data.local.ChatMessageEntity
import com.example.data.local.SavingsRecordEntity
import com.example.data.local.ActivityLogEntity
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SaveLoopApp(viewModel: SaveLoopViewModel = viewModel()) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()

    val selectedCoupon by viewModel.selectedCoupon.collectAsState()
    val isAddCouponOpen by viewModel.isAddCouponOpen.collectAsState()
    val isOcrInProgress by viewModel.isOcrInProgress.collectAsState()
    val isCheckoutOverlayActive by viewModel.isCheckoutOverlayActive.collectAsState()
    val isNotificationsOpen by viewModel.isNotificationsOpen.collectAsState()
    val isPremiumOpen by viewModel.isPremiumOpen.collectAsState()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()
    val isProfileEditOpen by viewModel.isProfileEditOpen.collectAsState()
    val profileName by viewModel.profileName.collectAsState()
    val profileEmail by viewModel.profileEmail.collectAsState()
    val profilePhotoUri by viewModel.profilePhotoUri.collectAsState()
    val profileAvatarIndex by viewModel.profileAvatarIndex.collectAsState()
    val incomingSmsAlert by viewModel.incomingSmsAlert.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MatteBlackBg)
    ) {
        // --- Core Screen Navigation System ---
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                fadeIn() with fadeOut()
            },
            label = "ScreenTransition"
        ) { screen ->
            when (screen) {
                AppScreen.SPLASH -> SplashScreen(onTimeout = { viewModel.navigateTo(AppScreen.ONBOARDING_1) })
                AppScreen.ONBOARDING_1 -> OnboardingScreen(
                    step = 1,
                    onNext = { viewModel.navigateTo(AppScreen.ONBOARDING_2) },
                    onSkip = { viewModel.navigateTo(AppScreen.AUTH) }
                )
                AppScreen.ONBOARDING_2 -> OnboardingScreen(
                    step = 2,
                    onNext = { viewModel.navigateTo(AppScreen.ONBOARDING_3) },
                    onSkip = { viewModel.navigateTo(AppScreen.AUTH) }
                )
                AppScreen.ONBOARDING_3 -> OnboardingScreen(
                    step = 3,
                    onNext = { viewModel.navigateTo(AppScreen.AUTH) },
                    onSkip = { viewModel.navigateTo(AppScreen.AUTH) }
                )
                AppScreen.AUTH -> AuthScreen(onLoginSuccess = { viewModel.navigateTo(AppScreen.PERMISSIONS) })
                AppScreen.PERMISSIONS -> PermissionsSetupScreen(onAllGranted = { viewModel.navigateTo(AppScreen.INITIAL_SCAN) })
                AppScreen.INITIAL_SCAN -> InitialScanScreen(onScanFinished = { viewModel.navigateTo(AppScreen.MAIN) })
                AppScreen.MAIN -> MainScaffold(viewModel = viewModel, currentTab = currentTab)
            }
        }

        // --- Dialogs & Overlays Bottom Sheet Modals ---

        // 1. Coupon Details Dialog
        selectedCoupon?.let { coupon ->
            CouponDetailsBottomSheet(
                coupon = coupon,
                onClose = { viewModel.closeCouponDetails() },
                onArchive = { viewModel.archiveCoupon(coupon) },
                onDelete = { viewModel.deleteCoupon(coupon) },
                onToggleFavorite = { viewModel.toggleFavoriteCoupon(coupon) }
            )
        }

        // 2. Add Coupon Manual Overlay Dialog
        if (isAddCouponOpen) {
            AddCouponSheet(
                onClose = { viewModel.toggleAddCoupon(false) },
                onSave = { merchant, code, discount, minOrder, expiry, category, cashback ->
                    viewModel.addManualCoupon(merchant, code, discount, minOrder, expiry, category, cashback)
                }
            )
        }

        // 3. Checkout Simulator Floating Notification Overlay
        if (isCheckoutOverlayActive) {
            CheckoutDetectionOverlay(
                onClose = { viewModel.toggleCheckoutOverlay(false) },
                onApply = {
                    // Grab Zomato coupon from room or fallback
                    viewModel.toggleCheckoutOverlay(false)
                    val zomatoCoupon = viewModel.allCoupons.value.firstOrNull { it.merchantName == "Zomato" }
                        ?: CouponEntity(merchantName = "Zomato", code = "ZOMATO150", discountValue = "₹150 OFF", minOrderValue = "₹499", expiryDate = "", category = "Food")
                    viewModel.applyCouponAndRecordSavings(zomatoCoupon)
                }
            )
        }

        // 4. Notification Center Modal
        if (isNotificationsOpen) {
            NotificationCenterOverlay(onClose = { viewModel.toggleNotificationsCenter(false) })
        }

        // 5. Upgrade Premium Presentation Screen
        if (isPremiumOpen) {
            PremiumSubscriptionScreen(onClose = { viewModel.togglePremiumPromo(false) })
        }

        // 6. Connected Settings Dialog
        if (isSettingsOpen) {
            SettingsOverlay(onClose = { viewModel.toggleSettings(false) })
        }

        // 7. Profile Edit Bottom Sheet
        if (isProfileEditOpen) {
            ProfileEditBottomSheet(
                name = profileName,
                email = profileEmail,
                photoUri = profilePhotoUri,
                avatarIndex = profileAvatarIndex,
                onClose = { viewModel.setProfileEditOpen(false) },
                onSave = { name, email ->
                    viewModel.updateProfile(name, email)
                    viewModel.setProfileEditOpen(false)
                },
                onAvatarSelected = { index ->
                    viewModel.selectAvatarIndex(index)
                },
                onPhotoUploaded = { uri ->
                    viewModel.updateProfilePhoto(uri)
                }
            )
        }

        // 8. Dynamic Automated SMS Alert Toast
        incomingSmsAlert?.let { alert ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(top = 40.dp) // offset from visual notch/top edge
                    .align(Alignment.TopCenter)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DeepCharcoalSurface)
                    .border(1.5.dp, NeonGreen, RoundedCornerShape(16.dp))
                    .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = NeonGreen)
                    .clickable { viewModel.approveSmsCoupon(alert) }
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(NeonGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "SMS Code Alert",
                            tint = NeonGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "SMS REWARD SCANNED",
                                color = NeonGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(GlowingGreen.copy(alpha = 0.2f))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "AUTO FEED",
                                    color = GlowingGreen,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Found code ${alert.code} from ${alert.sender}!",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            "Add ${alert.discount} offer directly to Wallet.",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { viewModel.dismissSmsAlert() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss SMS Auto Feed Toast",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 1 — SPLASH SCREEN
// ==========================================
@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "SplashGlow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    LaunchedEffect(Unit) {
        delay(2200)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MatteBlackBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Draw Custom Beautiful Glowing Infinity Loop
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .drawBehind {
                        // Background glow
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(NeonGreen.copy(alpha = 0.15f * pulseScale), Color.Transparent),
                                center = Offset(size.width / 2f, size.height / 2f),
                                radius = size.width * 0.75f
                            )
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(130.dp)) {
                    val path = Path()
                    val w = size.width
                    val h = size.height
                    val cx = w / 2
                    val cy = h / 2

                    // Custom parametric infinity loop math path
                    // x = r * cos(t) / (1 + sin^2(t))
                    // y = r * cos(t) * sin(t) / (1 + sin^2(t))
                    val r = cx * 1.05f
                    var first = true
                    for (i in 0..360 step 4) {
                        val t = Math.toRadians(i.toDouble())
                        val denom = 1.0 + sin(t) * sin(t)
                        val x = cx + (r * java.lang.Math.cos(t) / denom).toFloat()
                        val y = cy + (r * java.lang.Math.cos(t) * sin(t) / denom).toFloat()
                        if (first) {
                            path.moveTo(x, y)
                            first = false
                        } else {
                            path.lineTo(x, y)
                        }
                    }
                    path.close()

                    // Draw Glowing stroke
                    drawPath(
                        path = path,
                        color = NeonGreen,
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw a coupon tick mark circle dot
                    drawCircle(
                        color = Color.White,
                        radius = 4.dp.toPx(),
                        center = Offset(cx - cx/2, cy + 5.dp.toPx())
                    )
                    drawCircle(
                        color = NeonGreen,
                        radius = 5.dp.toPx(),
                        center = Offset(cx + cx/2, cy - 3.dp.toPx())
                    )
                }

                // Inner brand tag icon overlay
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Star Tag",
                    tint = SoftMint,
                    modifier = Modifier
                        .size(34.dp)
                        .align(Alignment.Center)
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = "SaveLoop",
                color = Color.White,
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.displayMedium,
                letterSpacing = 2.sp,
                modifier = Modifier.testTag("app_brand_logo")
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Never Miss Savings Again.",
                color = NeonGreen,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyLarge,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            CircularProgressIndicator(
                color = NeonGreen,
                strokeWidth = 3.dp,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ==========================================
// SCREEN 2 — ONBOARDING FLOW
// ==========================================
@Composable
fun OnboardingScreen(step: Int, onNext: () -> Unit, onSkip: () -> Unit) {
    val headline = when (step) {
        1 -> "Track Every Coupon"
        2 -> "AI Reminds You Automatically"
        else -> "Save More Everyday"
    }

    val description = when (step) {
        1 -> "Automatically organize rewards, deals, and discount codes scanned directly from your apps, emails, and SMS."
        2 -> "Get high-priority coupon reminder popups exactly when you hit checkout screens on Swiggy, Zomato, or Amazon."
        else -> "Intelligent machine learning algorithms find and combine the highest cashback routes automatically."
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkContrastBg)
            .padding(24.dp)
    ) {
        // Top skip button
        TextButton(
            onClick = onSkip,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
        ) {
            Text("Skip", color = TextMuted, fontSize = 14.sp)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated/Polished Visual Card Placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(280.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                    .background(DeepCharcoalSurface)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                when (step) {
                    1 -> {
                        // Floating coupon cards visual mock
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Coupon Icon",
                                tint = NeonGreen,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CardGray),
                                border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.width(200.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("FOOD150", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    Text("Zomato • ₹150 Saved", color = SoftMint, fontSize = 12.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CardGray),
                                border = BorderStroke(1.dp, GlassBorder),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.width(170.dp).offset(x = 10.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("AMZNSAVE10", color = TextMuted, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                                    Text("Amazon • 10% Off", color = TextMuted, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                    2 -> {
                        // Checkout reminder visual popup UI
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(CardGray)
                                    .padding(14.dp)
                                    .border(1.dp, NeonGreen, RoundedCornerShape(16.dp))
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Notifications, "Alert", tint = NeonGreen, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Checkout Detected!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Apply code ZOMATO150 to save ₹150", color = TextTitanium, fontSize = 11.sp, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                    3 -> {
                        // AI finding combo visual
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Best combo",
                                tint = GlowingGreen,
                                modifier = Modifier.size(70.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Best combination identified!", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Text section
            Text(
                text = headline,
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = description,
                color = TextMuted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.fillMaxWidth(0.9f)
            )
        }

        // Bottom Nav Dot indicators & button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Dots
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) { index ->
                    val isActive = index + 1 == step
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(if (isActive) 18.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (isActive) NeonGreen else GlassBorder)
                    )
                }
            }

            // Next button
            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("onboarding_next_btn")
            ) {
                Text(
                    text = if (step == 3) "Get Started" else "Next",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

// ==========================================
// SCREEN 3 — AUTHENTICATION SCREEN
// ==========================================
@Composable
fun AuthScreen(onLoginSuccess: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MatteBlackBg)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Welcome to SaveLoop",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your futuristic AI savings guardian starts here.",
                color = TextMuted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Premium Fintech Login Card Form
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(DeepCharcoalSurface)
                    .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Text("Secure Account Portal", color = SoftMint, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    TextField(
                        value = "terry.melton@gmail.com",
                        onValueChange = {},
                        label = { Text("Email Address", color = TextMuted) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = CardGray,
                            unfocusedContainerColor = CardGray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = NeonGreen,
                            focusedIndicatorColor = NeonGreen
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    TextField(
                        value = "•••••••••••••",
                        onValueChange = {},
                        label = { Text("Password", color = TextMuted) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = CardGray,
                            unfocusedContainerColor = CardGray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = NeonGreen
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onLoginSuccess,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("email_login_button")
                    ) {
                        Text("Log In Securely", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("OR JOIN WITH", color = TextMuted, fontSize = 11.sp, letterSpacing = 1.sp)

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Social auth buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onLoginSuccess,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, GlassBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).testTag("google_login_btn")
                ) {
                    Text("Google", fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = onLoginSuccess,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, GlassBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Apple ID", fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(CardGray)
                    .clickable { onLoginSuccess() }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Icon(Icons.Default.Settings, "Biometric", tint = NeonGreen, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Bypassing? Touch for Biometric Lock", color = SoftMint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==========================================
// SCREEN 4 — PERMISSION SETUP
// ==========================================
@Composable
fun PermissionsSetupScreen(onAllGranted: () -> Unit) {
    var stepGrant by remember { mutableStateOf(1) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkContrastBg)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
        ) {
            Text(
                text = "Privacy & Permission Setup",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Secure local scanning helps SaveLoop automatically detect promo codes.",
                color = TextMuted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(30.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PermissionRow(
                    title = "Notification Access",
                    desc = "Detects reward codes in bank alerts & shopping push alerts.",
                    active = stepGrant >= 1,
                    onGrant = { stepGrant = maxOf(stepGrant, 2) }
                )

                PermissionRow(
                    title = "SMS Scoping",
                    desc = "Extracts verified cashbacks from merchant order bills.",
                    active = stepGrant >= 2,
                    onGrant = { stepGrant = maxOf(stepGrant, 3) }
                )

                PermissionRow(
                    title = "Storage Scanner",
                    desc = "Scans offline screenshots in your photo gallery.",
                    active = stepGrant >= 3,
                    onGrant = { stepGrant = maxOf(stepGrant, 4) }
                )

                PermissionRow(
                    title = "Accessibility Core Service",
                    desc = "Triggers the automatic reminder widget exactly at checkouts.",
                    active = stepGrant >= 4,
                    onGrant = { stepGrant = maxOf(stepGrant, 5) }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = onAllGranted,
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("accept_all_perms_btn")
            ) {
                Text("Confirm & Safe Scan Home", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
fun PermissionRow(title: String, desc: String, active: Boolean, onGrant: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DeepCharcoalSurface)
            .border(1.dp, if (active) NeonGreen.copy(alpha = 0.5f) else GlassBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (active) NeonGreen.copy(alpha = 0.2f) else CardGray),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (active) Icons.Default.Check else Icons.Default.Close,
                contentDescription = "Status",
                tint = if (active) NeonGreen else TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = if (active) Color.White else TextMuted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(desc, color = TextMuted, fontSize = 11.sp, lineHeight = 15.sp)
        }
        if (!active) {
            TextButton(onClick = onGrant) {
                Text("Authorize", color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Text("Granted", color = SoftMint, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ==========================================
// SCREEN 5 — INITIAL AI SCAN
// ==========================================
@Composable
fun InitialScanScreen(onScanFinished: () -> Unit) {
    var scaleFactor by remember { mutableStateOf(1f) }
    var currentMerchantText by remember { mutableStateOf("Ready to scanning...") }
    val scannedLogs = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) {
        val merchants = listOf("Zomato SMS", "Swiggy push logs", "PhonePe clipboard banner", "Amazon screenshot OCR", "Myntra reward inbox")
        for (item in merchants) {
            delay(800)
            currentMerchantText = "Parsing $item..."
            scaleFactor = 1.1f
            scannedLogs.add("• Analyzed $item")
            delay(200)
            scaleFactor = 1.0f
        }
        delay(600)
        currentMerchantText = "Found 12 active rewards!"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MatteBlackBg)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Initial AI Coupon Ingest Scan",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(30.dp))

            // Scanner Ring
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .drawBehind {
                        drawCircle(
                            color = NeonGreen.copy(alpha = 0.08f),
                            radius = size.width / 2 * scaleFactor
                        )
                    }
                    .border(2.dp, NeonGreen, CircleShape)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "AI Scanner Loop",
                    tint = NeonGreen,
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = currentMerchantText,
                color = SoftMint,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Timeline output
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(110.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                    .background(DeepCharcoalSurface)
                    .padding(14.dp)
            ) {
                LazyColumn {
                    items(scannedLogs.size) { index ->
                        Text(
                            text = scannedLogs[index],
                            color = TextTitanium,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = onScanFinished,
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("finish_scan_btn")
            ) {
                Text("Enter Neon Wallet Dashboard", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

// ==========================================
// MAIN SCAFFOLD & BOTTOM NAVIGATION
// ==========================================
@Composable
fun MainScaffold(
    viewModel: SaveLoopViewModel,
    currentTab: NavigationTab
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("main_scaffold_root"),
        containerColor = MatteBlackBg,
        bottomBar = {
            NavigationBar(
                containerColor = DeepCharcoalSurface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .border(BorderStroke(1.dp, GlassBorder), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                NavigationBarItem(
                    selected = currentTab == NavigationTab.HOME,
                    onClick = { viewModel.selectTab(NavigationTab.HOME) },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        unselectedIconColor = TextMuted,
                        selectedTextColor = NeonGreen,
                        indicatorColor = NeonGreen
                    ),
                    modifier = Modifier.testTag("nav_tab_home")
                )

                NavigationBarItem(
                    selected = currentTab == NavigationTab.WALLET,
                    onClick = { viewModel.selectTab(NavigationTab.WALLET) },
                    icon = { Icon(Icons.Default.Favorite, contentDescription = "Wallet") },
                    label = { Text("Wallet") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        unselectedIconColor = TextMuted,
                        selectedTextColor = NeonGreen,
                        indicatorColor = NeonGreen
                    ),
                    modifier = Modifier.testTag("nav_tab_wallet")
                )

                NavigationBarItem(
                    selected = currentTab == NavigationTab.AI_ASSISTANT,
                    onClick = { viewModel.selectTab(NavigationTab.AI_ASSISTANT) },
                    icon = { Icon(Icons.Default.Star, contentDescription = "AI") },
                    label = { Text("Ask AI") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        unselectedIconColor = TextMuted,
                        selectedTextColor = NeonGreen,
                        indicatorColor = NeonGreen
                    ),
                    modifier = Modifier.testTag("nav_tab_chat")
                )

                NavigationBarItem(
                    selected = currentTab == NavigationTab.ACTIVITY,
                    onClick = { viewModel.selectTab(NavigationTab.ACTIVITY) },
                    icon = { Icon(Icons.Default.Notifications, contentDescription = "History") },
                    label = { Text("Activity") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        unselectedIconColor = TextMuted,
                        selectedTextColor = NeonGreen,
                        indicatorColor = NeonGreen
                    ),
                    modifier = Modifier.testTag("nav_tab_activity")
                )

                NavigationBarItem(
                    selected = currentTab == NavigationTab.PROFILE,
                    onClick = { viewModel.selectTab(NavigationTab.PROFILE) },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        unselectedIconColor = TextMuted,
                        selectedTextColor = NeonGreen,
                        indicatorColor = NeonGreen
                    ),
                    modifier = Modifier.testTag("nav_tab_profile")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                NavigationTab.HOME -> HomeTab(viewModel = viewModel)
                NavigationTab.WALLET -> WalletTab(viewModel = viewModel)
                NavigationTab.AI_ASSISTANT -> AssistantTab(viewModel = viewModel)
                NavigationTab.ACTIVITY -> ActivityTab(viewModel = viewModel)
                NavigationTab.PROFILE -> ProfileTab(viewModel = viewModel)
            }
        }
    }
}

// ==========================================
// SCREEN 6 — HOME DASHBOARD
// ==========================================
@Composable
fun HomeTab(viewModel: SaveLoopViewModel) {
    val coupons by viewModel.allCoupons.collectAsState()
    val savingsRecords by viewModel.savingsRecords.collectAsState()
    val isOcrInProgress by viewModel.isOcrInProgress.collectAsState()
    val profileName by viewModel.profileName.collectAsState()
    val profilePhotoUri by viewModel.profilePhotoUri.collectAsState()
    val profileAvatarIndex by viewModel.profileAvatarIndex.collectAsState()

    val totalSaved = savingsRecords.sumOf { it.amountSaved } + 24650.0 // Milestone simulation offset
    val cashbackEarned = savingsRecords.sumOf { it.cashbackEarned } + 6540.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Top profile bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfileAvatar(
                    name = profileName,
                    photoUri = profilePhotoUri,
                    avatarIndex = profileAvatarIndex,
                    size = 48,
                    borderSize = 1.0,
                    modifier = Modifier.clickable { viewModel.setProfileEditOpen(true) }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Good Evening, ${profileName.split(" ").firstOrNull() ?: "Terry"} 👋", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Always finding you the best deals", color = TextMuted, fontSize = 11.sp)
                }
            }

            IconButton(
                onClick = { viewModel.toggleNotificationsCenter(true) },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(DeepCharcoalSurface)
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = NeonGreen
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Glass savings card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0x1BFFFFFF), Color(0x0DFFFFFF))
                    )
                )
                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(32.dp))
                .padding(24.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("TOTAL SAVED", color = SoftMint, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text("₹${String.format("%,.2f", totalSaved)}", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeonGreen.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("+18.6% vs last mth", color = NeonGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Active Coupons", color = TextMuted, fontSize = 10.sp)
                        Text("${coupons.size + 26}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Expiring Soon", color = TextMuted, fontSize = 10.sp)
                        Text("8", color = AlertRed, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Cashback Cash-in", color = TextMuted, fontSize = 10.sp)
                        Text("₹${String.format("%,.2f", cashbackEarned)}", color = NeonGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Savings Chart dynamic canvas drawing
                Text("Monthly Trend Line", color = TextMuted, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                ) {
                    val points = listOf(
                        Offset(0f, size.height * 0.8f),
                        Offset(size.width * 0.2f, size.height * 0.7f),
                        Offset(size.width * 0.4f, size.height * 0.75f),
                        Offset(size.width * 0.6f, size.height * 0.4f),
                        Offset(size.width * 0.8f, size.height * 0.5f),
                        Offset(size.width, size.height * 0.15f)
                    )
                    // Draw lines
                    for (i in 0 until points.size - 1) {
                        drawLine(
                            color = NeonGreen,
                            start = points[i],
                            end = points[i + 1],
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                    // Draw dots
                    points.forEach { pt ->
                        drawCircle(
                            color = Color.White,
                            radius = 4.dp.toPx(),
                            center = pt
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Quick action rows
        Text("QUICK COUPON CONTROLS", color = SoftMint, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickActionButton(
                label = "Add Coupon",
                icon = Icons.Default.Add,
                modifier = Modifier.weight(1f).testTag("quick_add_btn"),
                isPrimary = false,
                onClick = { viewModel.toggleAddCoupon(true) }
            )

            // Scanning screenshot OCR button simulation
            QuickActionButton(
                label = if (isOcrInProgress) "Scanning..." else "Simulate Scan",
                icon = Icons.Default.Search,
                modifier = Modifier.weight(1.1f).testTag("quick_scan_btn"),
                isPrimary = true,
                onClick = {
                    viewModel.scanScrreenshotOCRSimulate { scanned ->
                        viewModel.openCouponDetails(scanned)
                    }
                }
            )

            QuickActionButton(
                label = "Auto Check",
                icon = Icons.Default.Check,
                modifier = Modifier.weight(1f).testTag("quick_sim_checkout_btn"),
                isPrimary = false,
                onClick = { viewModel.toggleCheckoutOverlay(true) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Expiring Soon
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("EXPIRING SOON", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("View All", color = NeonGreen, fontSize = 12.sp, modifier = Modifier.clickable { viewModel.selectTab(NavigationTab.WALLET) })
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Expiring card list
        coupons.filter { !it.isArchived && it.expiryDate.contains("hour") }.forEach { coupon ->
            HomeCouponRowCard(coupon = coupon, onClick = { viewModel.openCouponDetails(coupon) })
            Spacer(modifier = Modifier.height(10.dp))
        }

        Spacer(modifier = Modifier.height(20.dp))

        // AI Advice box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardGray)
                .border(1.dp, NeonGreen.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(NeonGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Star, "Prompt", tint = NeonGreen)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("AI Saving Intelligence Advice", color = SoftMint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("💡 \"Use Swiggy instead of Zomato for your snack order and save ₹115 extra with combined banks.\"", color = TextTitanium, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    onClick: () -> Unit
) {
    val roundedShape = RoundedCornerShape(24.dp)
    Box(
        modifier = modifier
            .clip(roundedShape)
            .background(if (isPrimary) NeonGreen else Color(0x0DFFFFFF))
            .then(
                if (isPrimary) Modifier else Modifier.border(1.dp, Color(0x1BFFFFFF), roundedShape)
            )
            .clickable { onClick() }
            .padding(vertical = 14.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isPrimary) MatteBlackBg else NeonGreen,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                color = if (isPrimary) MatteBlackBg else Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun HomeCouponRowCard(coupon: CouponEntity, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DeepCharcoalSurface)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Merchant circular logo outline
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(CardGray),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = coupon.merchantName.take(1),
                color = NeonGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(coupon.merchantName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(coupon.couponDetailsSummary(), color = TextMuted, fontSize = 11.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(coupon.discountValue, color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(coupon.expiryDate, color = AlertRed, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ==========================================
// SCREEN 7 & 8 — COUPON WALLET & DETAIL SHEET
// ==========================================
@Composable
fun WalletTab(viewModel: SaveLoopViewModel) {
    val coupons by viewModel.allCoupons.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "Food", "Shopping", "Travel", "Bills", "Payments")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Your Coupon Vault Wallet", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(14.dp))

        // Search bar
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search coupons, brands, restaurants...", color = TextMuted, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, "Search", tint = TextMuted) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = DeepCharcoalSurface,
                unfocusedContainerColor = DeepCharcoalSurface,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedIndicatorColor = NeonGreen
            ),
            modifier = Modifier.fillMaxWidth().testTag("wallet_search_bar"),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // SMS Auto-feed Scanner Panel
        var showSmsTools by remember { mutableStateOf(false) }
        val smsAutoFeedEnabled by viewModel.smsAutoFeedEnabled.collectAsState()
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(DeepCharcoalSurface)
                .border(
                    BorderStroke(1.dp, if (smsAutoFeedEnabled) NeonGreen.copy(alpha = 0.3f) else GlassBorder),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(12.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (smsAutoFeedEnabled) NeonGreen.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "SMS Autofeed",
                                tint = if (smsAutoFeedEnabled) NeonGreen else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "SMS Reward Auto-Scanner",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (smsAutoFeedEnabled) "Listening to inbound reward codes" else "Auto-scanner turned off",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                    
                    Switch(
                        checked = smsAutoFeedEnabled,
                        onCheckedChange = { viewModel.setSmsAutoFeedEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MatteBlackBg,
                            checkedTrackColor = NeonGreen,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier.scale(0.85f)
                    )
                }
                
                if (smsAutoFeedEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .clickable { showSmsTools = !showSmsTools }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (showSmsTools) "Hide SMS Test Utility" else "Simulate Inbound SMS Message",
                                color = NeonGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = if (showSmsTools) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle",
                                tint = NeonGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                
                if (showSmsTools && smsAutoFeedEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Tap any mock message below to simulate an inbound SMS alert that SaveLoop will intercept and auto-extract:",
                        color = TextMuted,
                        fontSize = 10.sp,
                        lineHeight = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val mockSmsMessages = listOf(
                        Pair("Swiggy Alert", "Treat yourself! Apply SWIGGY50 to get flat ₹150 discount on orders above ₹399 this festive season!"),
                        Pair("Zomato SMS", "Craving biryani? Use ZOMATOPT70 to grab ₹200 savings immediately on partner restaurants!"),
                        Pair("Amazon Deals", "Great Indian Sale Live! Promo AMZS20 gives you 20% flat discount on fashionable gear."),
                        Pair("Myntra Reward", "Grab latest shoes! Apply MYNTRA120 code during checkout for flat premium cashback."),
                        Pair("Flipkart Promo", "Big Billion Days! Use FLIPKART100 to save flat 10% on electronics."),
                        Pair("Nykaa Beauty", "Beauty Fest! Redeem NYKAA50 for flat ₹100 discount on cosmetic products."),
                        Pair("Ajio Fashion", "Style Up! Use AJIO30 to grab flat 30% savings on apparel orders above ₹1499."),
                        Pair("Blinkit Delivery", "Groceries in 10 mins! Use BLINKFAST to save ₹80 on your pantry restocking."),
                        Pair("Zepto Grocery", "Fresh organic apples! Apply ZEPTOFRESH to get ₹50 off instantly on fresh cart."),
                        Pair("Tata CLiQ Luxe", "Luxury sale! Promo CLIQLUX20 gives you flat 20% off on designer sunglasses.")
                    )
                    
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        mockSmsMessages.forEach { sms ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CardGray)
                                    .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                                    .clickable {
                                        viewModel.receiveSimulatedSms(sms.first, sms.second)
                                    }
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(sms.first, color = SoftMint, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Text("Tap to simulate 📬", color = NeonGreen, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(sms.second, color = Color.White, fontSize = 11.sp, lineHeight = 14.sp)
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Custom SMS text input simulation
                    var customSmsSender by remember { mutableStateOf("Dominos SMS") }
                    var customSmsBody by remember { mutableStateOf("Use code PIZZA100 to save flat 20% on next 3 gourmet crusts!") }
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MatteBlackBg)
                            .padding(8.dp)
                    ) {
                        Text("Create Custom SMS Simulator:", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        BasicTextField(
                            value = customSmsSender,
                            onValueChange = { customSmsSender = it },
                            textStyle = TextStyle(color = SoftMint, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CardGray, RoundedCornerShape(4.dp))
                                .padding(6.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        BasicTextField(
                            value = customSmsBody,
                            onValueChange = { customSmsBody = it },
                            textStyle = TextStyle(color = Color.White, fontSize = 11.sp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CardGray, RoundedCornerShape(4.dp))
                                .padding(6.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Button(
                            onClick = { viewModel.receiveSimulatedSms(customSmsSender, customSmsBody) },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(28.dp).align(Alignment.End)
                        ) {
                            Text("Simulate Custom Inbound 🚀", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Category scrollable row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                val isSelected = selectedCategory == category
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) NeonGreen else DeepCharcoalSurface)
                        .clickable { selectedCategory = category }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = category,
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Filter coupons
        val filteredCoupons = coupons.filter {
            !it.isArchived &&
                    (selectedCategory == "All" || it.category.equals(selectedCategory, ignoreCase = true)) &&
                    (searchQuery.isEmpty() || it.merchantName.lowercase().contains(searchQuery.lowercase()) || it.code.lowercase().contains(searchQuery.lowercase()))
        }

        if (filteredCoupons.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Clear, "No rewards", tint = TextMuted, modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No local coupons matching filters", color = TextTitanium, fontSize = 14.sp)
                    Text("Scan a screenshot or add one manually!", color = TextMuted, fontSize = 11.sp, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredCoupons.size) { index ->
                    val coupon = filteredCoupons[index]
                    WalletListItemCard(
                        coupon = coupon,
                        onDetail = { viewModel.openCouponDetails(coupon) },
                        onCopy = {
                            viewModel.logActivity("Copied ${coupon.code}", "Copied code for ${coupon.merchantName} during shopping.", "copied")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun WalletListItemCard(coupon: CouponEntity, onDetail: () -> Unit, onCopy: () -> Unit) {
    val clipboardManager = LocalClipboardManager.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DeepCharcoalSurface)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .clickable { onDetail() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Dot indicator favoring state
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(CardGray),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (coupon.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Fav",
                tint = if (coupon.isFavorite) NeonGreen else TextMuted,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(coupon.merchantName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(coupon.code, color = SoftMint, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            Text(coupon.minOrderValue, color = TextMuted, fontSize = 10.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(coupon.discountValue, color = GlowingGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = {
                    clipboardManager.setText(AnnotatedString(coupon.code))
                    onCopy()
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(26.dp)
            ) {
                Text("COPY", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// SCREEN 8 — COUPON DETAILED PANEL
@Composable
fun CouponDetailsBottomSheet(
    coupon: CouponEntity,
    onClose: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable { onClose() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(DeepCharcoalSurface)
                .border(1.dp, GlassBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .clickable(enabled = false) { }
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Notch anchor indicator
                Box(
                    modifier = Modifier
                        .size(40.dp, 4.dp)
                        .clip(CircleShape)
                        .background(GlassBorder)
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(coupon.merchantName, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("Category: ${coupon.category}", color = TextMuted, fontSize = 12.sp)
                    }
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (coupon.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Fav",
                            tint = NeonGreen,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Big Promo code box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardGray)
                        .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("COUPON TARGET CODE", color = TextMuted, fontSize = 10.sp, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = coupon.code,
                            color = NeonGreen,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text("REWARD DESCRIPTION", color = SoftMint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(coupon.discountValue, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(coupon.minOrderValue, color = TextTitanium, fontSize = 13.sp)

                if (coupon.cashbackInfo.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("ESTIMATED CASHBACK", color = SoftMint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(coupon.cashbackInfo, color = GlowingGreen, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Text("TERMS & SPECIAL COMPATIBILITY", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(coupon.termsAndConditions, color = TextMuted, fontSize = 12.sp, lineHeight = 18.sp)

                Spacer(modifier = Modifier.height(30.dp))

                // Bottom row details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onClose,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onArchive,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, GlassBorder),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Text("Archive")
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(CardGray)
                            .size(46.dp)
                    ) {
                        Icon(Icons.Default.Delete, "Delete", tint = AlertRed)
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 9 & 10 — ADD COUPON FLOW / SCREENSHOT
// ==========================================
@Composable
fun AddCouponSheet(
    onClose: () -> Unit,
    onSave: (String, String, String, String, String, String, String) -> Unit
) {
    var merchant by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var discount by remember { mutableStateOf("") }
    var minOrder by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("Expires in 1 week") }
    var category by remember { mutableStateOf("Food") }
    var cashback by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable { onClose() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(DeepCharcoalSurface)
                .border(1.dp, GlassBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .clickable(enabled = false) {}
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Add New Promo Reward", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))

                TextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Merchant Brand (e.g. Zomato, Nike)", color = TextMuted) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = CardGray,
                        unfocusedContainerColor = CardGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = NeonGreen
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("add_coupon_merchant_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                TextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Coupon Code (e.g. FOOD150)", color = TextMuted) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = CardGray,
                        unfocusedContainerColor = CardGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = NeonGreen
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("add_coupon_code_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                TextField(
                    value = discount,
                    onValueChange = { discount = it },
                    label = { Text("Discount value (e.g. ₹150 OFF, 20% OFF)", color = TextMuted) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = CardGray,
                        unfocusedContainerColor = CardGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = NeonGreen
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("add_coupon_discount_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                TextField(
                    value = minOrder,
                    onValueChange = { minOrder = it },
                    label = { Text("Minimum order (e.g. On orders above ₹499)", color = TextMuted) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = CardGray,
                        unfocusedContainerColor = CardGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = NeonGreen
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                TextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (Food, Shopping, Travel, Bills, Payments)", color = TextMuted) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = CardGray,
                        unfocusedContainerColor = CardGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = NeonGreen
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                TextField(
                    value = cashback,
                    onValueChange = { cashback = it },
                    label = { Text("Cashback reward (e.g. Up to ₹50 cashback)", color = TextMuted) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = CardGray,
                        unfocusedContainerColor = CardGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = NeonGreen
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (merchant.isNotEmpty() && code.isNotEmpty() && discount.isNotEmpty()) {
                                onSave(merchant, code, discount, minOrder, expiry, category, cashback)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(46.dp).testTag("save_manual_coupon_btn")
                    ) {
                        Text("Save To Wallet", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onClose,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, GlassBorder),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 11 — AI ASSISTANT CHAT TAB
// ==========================================
@Composable
fun AssistantTab(viewModel: SaveLoopViewModel) {
    val messages by viewModel.chatMessages.collectAsState()
    val isLoading by viewModel.chatLoading.collectAsState()
    var inputQuery by remember { mutableStateOf("") }
    val lazyListState = rememberLazyListState()

    // Suggestions chips
    val suggestions = listOf("Pizza deals today? 🍕", "Best Amazon offer? 🛍️", "Recharge discount? ⚡")

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            delay(100)
            lazyListState.scrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("SaveLoop AI Advisor", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            IconButton(
                onClick = { viewModel.clearChatHistory() },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(CardGray)
                    .size(36.dp)
            ) {
                Icon(Icons.Default.Refresh, "Clear", tint = AlertRed, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Chats list
        LazyColumn(
            state = lazyListState,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(messages.size) { index ->
                val message = messages[index]
                ChatBubble(message = message, onOpenCoupon = { merchant ->
                    viewModel.allCoupons.value.firstOrNull { it.merchantName.equals(merchant, ignoreCase = true) }?.let {
                        viewModel.openCouponDetails(it)
                    }
                })
            }
            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        CircularProgressIndicator(color = NeonGreen, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SaveLoop AI is parsing deals...", color = TextMuted, fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Suggestions row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestions.forEach { suggestion ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(DeepCharcoalSurface)
                        .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                        .clickable {
                            inputQuery = suggestion.replace("🍕", "").replace("🛍️", "").replace("⚡", "").trim()
                            viewModel.sendMessage(inputQuery)
                            inputQuery = ""
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(suggestion, color = SoftMint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Footer inputs
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TextField(
                value = inputQuery,
                onValueChange = { inputQuery = it },
                placeholder = { Text("Ask for coupons, best restaurants...", color = TextMuted, fontSize = 13.sp) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = DeepCharcoalSurface,
                    unfocusedContainerColor = DeepCharcoalSurface,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = NeonGreen
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (inputQuery.isNotEmpty()) {
                        viewModel.sendMessage(inputQuery)
                        inputQuery = ""
                    }
                }),
                modifier = Modifier.weight(1f).testTag("chat_input_field"),
                shape = RoundedCornerShape(12.dp)
            )

            IconButton(
                onClick = {
                    if (inputQuery.isNotEmpty()) {
                        viewModel.sendMessage(inputQuery)
                        inputQuery = ""
                    }
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(NeonGreen)
                    .size(46.dp)
                    .testTag("chat_send_btn")
            ) {
                Icon(Icons.Default.Send, "Send", tint = Color.Black)
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessageEntity, onOpenCoupon: (String) -> Unit) {
    val isUser = message.isUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.85f),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 2.dp,
                            bottomEnd = if (isUser) 2.dp else 16.dp
                        )
                    )
                    .background(if (isUser) NeonGreen else CardGray)
                    .border(1.dp, if (isUser) Color.Transparent else GlassBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = message.message,
                    color = if (isUser) Color.Black else Color.White,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            }

            // Interactive coupon card linked inside AI reply
            if (!isUser && message.recommendedCouponCode != null && message.recommendedMerchant != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DeepCharcoalSurface)
                        .border(1.dp, NeonGreen.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .clickable { onOpenCoupon(message.recommendedMerchant) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Star, "Linked", tint = NeonGreen, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Claim code: ${message.recommendedCouponCode} for ${message.recommendedMerchant}",
                        color = SoftMint,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

// ==========================================
// SCREEN 12 — CHECKOUT WIDGET OVERLAY
// ==========================================
@Composable
fun CheckoutDetectionOverlay(onClose: () -> Unit, onApply: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onClose() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(DeepCharcoalSurface)
                .border(2.dp, NeonGreen, RoundedCornerShape(20.dp))
                .clickable(enabled = false) {}
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(NeonGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Notifications, "Check", tint = NeonGreen, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Checkout Detected • Zomato", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "You have an unused offer!\n\"Save ₹150 instantly using code ZOMATO150.\"",
                    color = TextTitanium,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onApply,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(44.dp).testTag("overlay_apply_btn")
                    ) {
                        Text("Apply Coupon", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onClose,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, GlassBorder),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(44.dp).testTag("overlay_cancel_btn")
                    ) {
                        Text("Not Now")
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 13 & 14 — SAVINGS ANALYTICS & TIMELINE ACTIVITY
// ==========================================
@Composable
fun ActivityTab(viewModel: SaveLoopViewModel) {
    val logs by viewModel.activityLogs.collectAsState()
    val savingsRecords by viewModel.savingsRecords.collectAsState()

    val totalSaved = savingsRecords.sumOf { it.amountSaved } + 24650.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Savings Analytics & Logs", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        // Compact stats metrics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(DeepCharcoalSurface)
                    .padding(12.dp)
            ) {
                Column {
                    Text("Total Saved", color = TextMuted, fontSize = 10.sp)
                    Text("₹${String.format("%,.0f", totalSaved)}", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(DeepCharcoalSurface)
                    .padding(12.dp)
            ) {
                Column {
                    Text("Coupons Applied", color = TextMuted, fontSize = 10.sp)
                    Text("${savingsRecords.size + 28}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dynamic categories bar chart
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(DeepCharcoalSurface)
                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Column {
                Text("CATERGORY BREAKDOWN", color = SoftMint, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                CategoryBar(label = "Food & Dining", percentage = 0.65f, amount = "₹16,020")
                Spacer(modifier = Modifier.height(10.dp))
                CategoryBar(label = "Shopping & Fashion", percentage = 0.25f, amount = "₹6,160")
                Spacer(modifier = Modifier.height(10.dp))
                CategoryBar(label = "Travel & Cab rides", percentage = 0.10f, amount = "₹2,470")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text("ACTIVITY TIMELINE", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No activities logged yet.", color = TextMuted, fontSize = 12.sp)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(logs.size) { index ->
                    val log = logs[index]
                    ActivityTimelineRow(log = log)
                }
            }
        }
    }
}

@Composable
fun CategoryBar(label: String, percentage: Float, amount: String) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White, fontSize = 12.sp)
            Text(amount, color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(CardGray)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percentage)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(NeonGreen)
            )
        }
    }
}

@Composable
fun ActivityTimelineRow(log: ActivityLogEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DeepCharcoalSurface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val colorIcon = when (log.type) {
            "applied" -> NeonGreen
            "copied" -> SoftMint
            else -> GlowingGreen
        }
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(colorIcon)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(log.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(log.description, color = TextMuted, fontSize = 11.sp)
        }
        Text("Just now", color = TextMuted, fontSize = 10.sp)
    }
}

// ==========================================
// SCREEN 15 — NOTIFICATION CENTER OVERLAY
// ==========================================
@Composable
fun NotificationCenterOverlay(onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable { onClose() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(DeepCharcoalSurface)
                .border(1.dp, GlassBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .clickable(enabled = false) {}
                .padding(24.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("SaveLoop Alerts Center", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AlertItem(title = "Zomato code expiring soon!", desc = "ZOMATO150 expires in 2 hours. Redeem it now before it's gone.", color = AlertRed)
                    AlertItem(title = "12 New Coupons Synced", desc = "Successfully synced active savings from Swiggy & Amazon SMS codes.", color = NeonGreen)
                    AlertItem(title = "AI Weekly Report", desc = "You saved ₹1,240 more this week than last week! Great shopping efficiency.", color = SoftMint)
                }

                Spacer(modifier = Modifier.height(30.dp))

                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Text("Dismiss All", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AlertItem(title: String, desc: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardGray)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(desc, color = TextMuted, fontSize = 11.sp, lineHeight = 15.sp)
        }
    }
}

// ==========================================
// SCREEN 16 — PROFILE SCREEN
// ==========================================
@Composable
fun ProfileTab(viewModel: SaveLoopViewModel) {
    val profileName by viewModel.profileName.collectAsState()
    val profileEmail by viewModel.profileEmail.collectAsState()
    val profilePhotoUri by viewModel.profilePhotoUri.collectAsState()
    val profileAvatarIndex by viewModel.profileAvatarIndex.collectAsState()
    val connectedApps by viewModel.connectedApps.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Profile Header
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ProfileAvatar(
                name = profileName,
                photoUri = profilePhotoUri,
                avatarIndex = profileAvatarIndex,
                size = 80,
                borderSize = 2.0,
                modifier = Modifier.clickable { viewModel.setProfileEditOpen(true) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(profileName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(profileEmail, color = TextMuted, fontSize = 13.sp)

            Spacer(modifier = Modifier.height(16.dp))

            // Premium Upgrade CTA Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1E3A1E))
                    .border(1.dp, NeonGreen, RoundedCornerShape(16.dp))
                    .clickable { viewModel.togglePremiumPromo(true) }
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("SaveLoop Premium Active", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Unlock auto apply & prime cashback routes.", color = TextTitanium, fontSize = 11.sp)
                    }
                    Icon(Icons.Default.Settings, "Premium Star", tint = GlowingGreen)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Apps Connected List
        var showAllPlatforms by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("CONNECTED APPS INTEGRATIONS", color = SoftMint, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(
                if (showAllPlatforms) "Show Less" else "View All (15)",
                color = NeonGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { showAllPlatforms = !showAllPlatforms }
            )
        }
        Spacer(modifier = Modifier.height(10.dp))

        val visiblePlatforms = if (showAllPlatforms) {
            connectedApps.keys.toList()
        } else {
            listOf("Swiggy", "Zomato", "Amazon India", "Flipkart")
        }

        visiblePlatforms.forEach { brand ->
            val isConnected = connectedApps[brand] ?: false
            ConnectedAppRow(brand = brand, connected = isConnected) {
                viewModel.toggleConnectedApp(brand)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // System actions
        Text("SYSTEM SETTINGS", color = SoftMint, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(10.dp))

        ProfileMenuRow(label = "Application Settings & Notification Controls", onClick = { viewModel.toggleSettings(true) })
        ProfileMenuRow(label = "Wallet Backup & Privacy controls", onClick = { viewModel.toggleSettings(true) })
        ProfileMenuRow(label = "Log Out Account", onClick = { viewModel.navigateTo(AppScreen.AUTH) })
    }
}

@Composable
fun ConnectedAppRow(brand: String, connected: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DeepCharcoalSurface)
            .clickable { onToggle() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(brand, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(if (connected) "Active AI listener configured" else "Tap to connect securely", color = TextMuted, fontSize = 11.sp)
        }
        Switch(
            checked = connected,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MatteBlackBg,
                checkedTrackColor = NeonGreen,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
            )
        )
    }
}

@Composable
fun ProfileMenuRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextTitanium, fontSize = 13.sp)
        Icon(Icons.Default.Settings, "Go", tint = TextMuted, modifier = Modifier.size(16.dp))
    }
}

// ==========================================
// SCREEN 17 — SETTINGS OVERLAY
// ==========================================
@Composable
fun SettingsOverlay(onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable { onClose() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(DeepCharcoalSurface)
                .clickable(enabled = false) {}
                .padding(24.dp)
        ) {
            Column {
                Text("SaveLoop System Settings", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Biometric Fingerprint Lock", color = TextTitanium, fontSize = 13.sp)
                    Switch(checked = true, onCheckedChange = {}, colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = NeonGreen))
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Quiet Mode Notifications", color = TextTitanium, fontSize = 13.sp)
                    Switch(checked = false, onCheckedChange = {}, colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = NeonGreen))
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("AI Personalization Toggle", color = TextTitanium, fontSize = 13.sp)
                    Switch(checked = true, onCheckedChange = {}, colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = NeonGreen))
                }

                Spacer(modifier = Modifier.height(30.dp))

                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Text("Save preferences", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==========================================
// SCREEN 18 — PREMIUM SUBSCRIPTION SCREEN
// ==========================================
@Composable
fun PremiumSubscriptionScreen(onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f))
            .clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(24.dp))
                .background(DeepCharcoalSurface)
                .border(2.dp, NeonGreen, RoundedCornerShape(24.dp))
                .clickable(enabled = false) {}
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Star, "Star Badge", tint = NeonGreen, modifier = Modifier.size(56.dp))
            Spacer(modifier = Modifier.height(12.dp))

            Text("SaveLoop Super Premium", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Automate 100% of your coupon savings", color = TextMuted, fontSize = 12.sp)

            Spacer(modifier = Modifier.height(20.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                PremiumFeatureRow(label = "Advanced AI recommendations & combo engine")
                PremiumFeatureRow(label = "Auto apply coupons directly during checkout")
                PremiumFeatureRow(label = "Priority cashback routes up to 8% higher")
                PremiumFeatureRow(label = "Family account sharing up to 5 devices")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Active - ₹149/month", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PremiumFeatureRow(label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Check, "Check symbol", tint = NeonGreen, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(label, color = TextTitanium, fontSize = 12.sp)
    }
}

// ==========================================
// HELPERS
// ==========================================
fun CouponEntity.couponDetailsSummary(): String {
    return "Code: $code • $minOrderValue • Category: $category"
}

// ==========================================
// PROFILE REUSABLE COMPONENTS
// ==========================================
@Composable
fun ProfileAvatar(
    name: String,
    photoUri: String?,
    avatarIndex: Int,
    size: Int = 48,
    borderSize: Double = 1.0,
    modifier: Modifier = Modifier
) {
    val sizeDp = size.dp
    val textStyleSize = (size * 0.35).sp
    val shape = CircleShape
    
    Box(
        modifier = modifier
            .size(sizeDp)
            .clip(shape)
            .background(Color(0x0DFFFFFF))
            .then(
                if (borderSize > 0) Modifier.border(borderSize.dp, NeonGreen, shape) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (avatarIndex == -1 && photoUri != null) {
            // Picked custom local gallery image
            val painter = rememberAsyncImagePainter(model = photoUri)
            Image(
                painter = painter,
                contentDescription = "Profile Photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        } else {
            // Preset style theme gradients
            val brush = when (avatarIndex) {
                1 -> Brush.linearGradient(colors = listOf(Color(0xFFFF5E62), Color(0xFFFF9966))) // Coral Sun Theme
                2 -> Brush.linearGradient(colors = listOf(Color(0xFF00F2FE), Color(0xFF4FACFE))) // Cyber Blue Theme
                3 -> Brush.linearGradient(colors = listOf(Color(0xFFD4145A), Color(0xFFFBB03B))) // Cosmo Bright Theme
                4 -> Brush.linearGradient(colors = listOf(NeonGreen, GlowingGreen)) // Signature Volt Accent
                else -> null
            }
            
            if (brush != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(brush)
                ) {
                    Box(
                        modifier = Modifier
                            .size((size * 0.6).dp)
                            .align(Alignment.Center)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (name.length >= 2) name.substring(0, 2).uppercase() else name.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = (size * 0.24).sp
                        )
                    }
                }
            } else {
                // Default clean initial with premium Neon border
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(colors = listOf(NeonGreen, GlowingGreen))
                            )
                            .padding(1.5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(MatteBlackBg),
                            contentAlignment = Alignment.Center
                        ) {
                            val initials = if (name.trim().isNotEmpty()) {
                                val split = name.trim().split(" ")
                                if (split.size >= 2) {
                                    (split[0].take(1) + split[1].take(1)).uppercase()
                                } else {
                                    name.take(2).uppercase()
                                }
                            } else {
                                "TM"
                            }
                            Text(
                                text = initials,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = textStyleSize
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditBottomSheet(
    name: String,
    email: String,
    photoUri: String?,
    avatarIndex: Int,
    onClose: () -> Unit,
    onSave: (String, String) -> Unit,
    onAvatarSelected: (Int) -> Unit,
    onPhotoUploaded: (String?) -> Unit
) {
    var editName by remember { mutableStateOf(name) }
    var editEmail by remember { mutableStateOf(email) }
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            onPhotoUploaded(uri.toString())
        }
    }

    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = DeepCharcoalSurface,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = NeonGreen.copy(alpha = 0.4f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Edit Profile & Avatar",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Interactive current badge preview
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(96.dp)) {
                ProfileAvatar(
                    name = editName,
                    photoUri = photoUri,
                    avatarIndex = avatarIndex,
                    size = 88,
                    borderSize = 3.0
                )
            }
            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { galleryLauncher.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.AddCircle, contentDescription = "Add Picture", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Upload Photo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                
                if (photoUri != null || avatarIndex != 0) {
                    OutlinedButton(
                        onClick = {
                            onPhotoUploaded(null)
                            onAvatarSelected(0)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed),
                        border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Reset Layout", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Designer list preset
            Text(
                text = "SELECT AN AESTHETIC THEME",
                color = SoftMint,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Index 0: Initials
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (avatarIndex == 0) NeonGreen.copy(alpha = 0.2f) else Color.Transparent)
                            .border(1.5.dp, if (avatarIndex == 0) NeonGreen else Color.Transparent, CircleShape)
                            .clickable { onAvatarSelected(0) },
                        contentAlignment = Alignment.Center
                    ) {
                        ProfileAvatar(name = editName, photoUri = null, avatarIndex = 0, size = 40, borderSize = 0.0)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Initials", color = if (avatarIndex == 0) NeonGreen else TextMuted, fontSize = 10.sp)
                }

                // Index 1: Coral Sun
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (avatarIndex == 1) NeonGreen.copy(alpha = 0.2f) else Color.Transparent)
                            .border(1.5.dp, if (avatarIndex == 1) NeonGreen else Color.Transparent, CircleShape)
                            .clickable { onAvatarSelected(1) },
                        contentAlignment = Alignment.Center
                    ) {
                        ProfileAvatar(name = editName, photoUri = null, avatarIndex = 1, size = 40, borderSize = 0.0)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Coral Sun", color = if (avatarIndex == 1) NeonGreen else TextMuted, fontSize = 10.sp)
                }

                // Index 2: Cyber Blue
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (avatarIndex == 2) NeonGreen.copy(alpha = 0.2f) else Color.Transparent)
                            .border(1.5.dp, if (avatarIndex == 2) NeonGreen else Color.Transparent, CircleShape)
                            .clickable { onAvatarSelected(2) },
                        contentAlignment = Alignment.Center
                    ) {
                        ProfileAvatar(name = editName, photoUri = null, avatarIndex = 2, size = 40, borderSize = 0.0)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Cyber Blue", color = if (avatarIndex == 2) NeonGreen else TextMuted, fontSize = 10.sp)
                }

                // Index 3: Cosmopolitan
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (avatarIndex == 3) NeonGreen.copy(alpha = 0.2f) else Color.Transparent)
                            .border(1.5.dp, if (avatarIndex == 3) NeonGreen else Color.Transparent, CircleShape)
                            .clickable { onAvatarSelected(3) },
                        contentAlignment = Alignment.Center
                    ) {
                        ProfileAvatar(name = editName, photoUri = null, avatarIndex = 3, size = 40, borderSize = 0.0)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Cosmo", color = if (avatarIndex == 3) NeonGreen else TextMuted, fontSize = 10.sp)
                }

                // Index 4: Signature Volt Accent
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (avatarIndex == 4) NeonGreen.copy(alpha = 0.2f) else Color.Transparent)
                            .border(1.5.dp, if (avatarIndex == 4) NeonGreen else Color.Transparent, CircleShape)
                            .clickable { onAvatarSelected(4) },
                        contentAlignment = Alignment.Center
                    ) {
                        ProfileAvatar(name = editName, photoUri = null, avatarIndex = 4, size = 40, borderSize = 0.0)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Volt", color = if (avatarIndex == 4) NeonGreen else TextMuted, fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // TextInput Fields
            Text(
                text = "USER PROFILE ATTRIBUTES",
                color = SoftMint,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = editName,
                onValueChange = { editName = it },
                label = { Text("Profile Name", color = TextMuted) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonGreen,
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = NeonGreen,
                    unfocusedLabelColor = TextMuted
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = editEmail,
                onValueChange = { editEmail = it },
                label = { Text("Contact Email", color = TextMuted) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonGreen,
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = NeonGreen,
                    unfocusedLabelColor = TextMuted
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (editName.isNotEmpty()) {
                        onSave(editName, editEmail)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Apply & Sync Changes", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

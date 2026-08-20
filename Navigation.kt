package com.avina.health

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.avina.health.components.AvinaBottomNavigationBar
import com.avina.health.data.AppLanguageManager
import com.avina.health.data.LabUsageManager
import com.avina.health.data.UserPreferencesManager
import com.avina.health.detail.AiRecommendationScreen
import com.avina.health.detail.HealthScoreDetailScreen
import com.avina.health.ocr.OcrTestScreen
import com.avina.health.screen.home.HomeActions
import com.avina.health.screen.home.HomeScreen
import com.avina.health.screen.login.ForgotPasswordScreen
import com.avina.health.screen.login.LoginScreen
import com.avina.health.screen.notification.NotificationScreen
import com.avina.health.screen.onboarding.PersonalInfoScreen
import com.avina.health.screen.profile.LabHistoryScreen
import com.avina.health.screen.profile.ProfileScreen
import com.avina.health.screen.register.RegisterScreen
import com.avina.health.screen.splash.SplashScreen
import com.avina.health.screen.subscription.SubscriptionManagementScreen
import com.avina.health.screen.subscription.SubscriptionScreen
import com.avina.health.screen.support.SupportScreen
import com.avina.health.ui.components.ImageSourceBottomSheet
import com.avina.health.ui.components.LabLimitDialog
import com.avina.health.ui.components.MainAddTestBottomSheet
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import androidx.compose.runtime.LaunchedEffect

object Routes {
    const val SPLASH = "splash"
    const val REGISTER = "register"
    const val PERSONAL_INFO = "personal_info"
    const val LOGIN = "login"
    const val FORGOT_PASSWORD = "forgot_password"
    const val HOME = "home/{userName}"
    const val PROFILE = "profile/{userName}"
    const val HEALTH_SCORE_DETAIL = "health_score_detail"
    const val AI_RECOMMENDATION = "ai_recommendation"
    const val SUBSCRIPTION = "subscription"
    const val SUBSCRIPTION_MANAGEMENT = "subscription_management"
    const val LAB_HISTORY = "lab_history"
    const val NOTIFICATIONS = "notifications"
    const val OCR_TEST = "ocr_test"
    const val SUPPORT = "support"

    private fun encode(value: String) = URLEncoder.encode(value, StandardCharsets.UTF_8.toString())

    fun createHomeRoute(userName: String) = "home/${encode(userName)}"
    fun createProfileRoute(userName: String) = "profile/${encode(userName)}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvinaNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val userPreferencesManager = remember { UserPreferencesManager(context) }
    val usageManager = remember { LabUsageManager(context) }
    val scope = rememberCoroutineScope()

    val isLoggedIn by userPreferencesManager.isLoggedIn.collectAsState(initial = false)
    val savedUserName by userPreferencesManager.userName.collectAsState(initial = "کاربر")

    NavHost(navController = navController, startDestination = Routes.SPLASH) {

        // SPLASH
        composable(Routes.SPLASH) {
            SplashScreen(onFinished = {
                val target = if (isLoggedIn) Routes.createHomeRoute(savedUserName) else Routes.REGISTER
                navController.navigate(target) { popUpTo(0) { inclusive = true } }
            })
        }

        // REGISTER
        composable(Routes.REGISTER) {
            RegisterScreen(
                onLoginClick = { navController.navigate(Routes.LOGIN) },
                onGuestClick = {
                    navController.navigate(Routes.createHomeRoute("مهمان")) { popUpTo(0) { inclusive = true } }
                },
                onRegisterClick = { navController.navigate(Routes.PERSONAL_INFO) }
            )
        }

        // PERSONAL INFO
        composable(Routes.PERSONAL_INFO) {
            PersonalInfoScreen(onNextClick = { name, _, _ ->
                val displayName = name.ifBlank { "کاربر" }
                scope.launch { userPreferencesManager.saveUserSession(displayName, "") }
                navController.navigate(Routes.createHomeRoute(displayName)) { popUpTo(0) { inclusive = true } }
            })
        }

        // LOGIN
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginClick = { emailOrUser, _ ->
                    val displayName = emailOrUser.ifBlank { "کاربر" }
                    scope.launch { userPreferencesManager.saveUserSession(displayName, emailOrUser) }
                    navController.navigate(Routes.createHomeRoute(displayName)) { popUpTo(0) { inclusive = true } }
                },
                onForgotPasswordClick = { navController.navigate(Routes.FORGOT_PASSWORD) },
                onSignUpClick = { navController.navigate(Routes.PERSONAL_INFO) }
            )
        }

        // FORGOT PASSWORD
        composable(Routes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(
                onSendCodeClick = { navController.popBackStack() },
                onBackToLoginClick = { navController.popBackStack() }
            )
        }

        // HOME
        composable(
            route = Routes.HOME,
            arguments = listOf(navArgument("userName") { type = NavType.StringType; defaultValue = "کاربر" })
        ) { entry ->
            val rawName = entry.arguments?.getString("userName") ?: "کاربر"
            val userName = URLDecoder.decode(rawName, StandardCharsets.UTF_8.toString())

            var selectedTab by remember { mutableIntStateOf(0) }
            var showMainAddModal by remember { mutableStateOf(false) }
            var showImageSourceModal by remember { mutableStateOf(false) }
            var showLabLimitDialog by remember { mutableStateOf(false) }
            var lastBackPressTime by remember { mutableLongStateOf(0L) }

            BackHandler {
                val current = System.currentTimeMillis()
                if (current - lastBackPressTime < 2000L) {
                    (context as? Activity)?.finish()
                } else {
                    lastBackPressTime = current
                    Toast.makeText(context, "برای خروج دوباره فشار دهید", Toast.LENGTH_SHORT).show()
                }
            }

            Box(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
                Scaffold(
                    bottomBar = {
                        AvinaBottomNavigationBar(
                            selectedTab = selectedTab,
                            showMiddleSpacer = true,
                            onTabSelected = { tab ->
                                // تب "گزارش PDF" (index 1) قبلاً فقط selectedTab را
                                // عوض می‌کرد بدون باز کردن هیچ صفحه‌ای — همان الگوی
                                // تب پروفایل (index 5) را برایش هم اعمال می‌کنیم:
                                // به‌جای نگه‌داشتن حالت داخل HomeScreen، مستقیم به
                                // صفحه‌ی تاریخچه‌ی گزارش‌ها navigate می‌کنیم.
                                when (tab) {
                                    1 -> navController.navigate(Routes.LAB_HISTORY)
                                    5 -> navController.navigate(Routes.createProfileRoute(userName))
                                    else -> selectedTab = tab
                                }
                            }
                        )
                    }
                ) { padding ->
                    HomeScreen(
                        modifier = Modifier.padding(padding),
                        userName = userName,
                        actions = HomeActions(
                            onNavigateToHealthDetail = { navController.navigate(Routes.HEALTH_SCORE_DETAIL) },
                            onNavigateToAiRecommendation = { navController.navigate(Routes.AI_RECOMMENDATION) },
                            onLogoutClick = {
                                scope.launch {
                                    userPreferencesManager.clearUserSession()
                                    navController.navigate(Routes.REGISTER) { popUpTo(0) { inclusive = true } }
                                }
                            },
                            onNavigateToAddTest = { showMainAddModal = true },
                            onNavigateToReports = { navController.navigate(Routes.LAB_HISTORY) },
                            onNavigateToAi = { navController.navigate(Routes.AI_RECOMMENDATION) },
                            onNavigateToProfile = { navController.navigate(Routes.createProfileRoute(userName)) },
                            onNavigateToAttentionDetail = {},
                            onNotificationClick = { navController.navigate(Routes.NOTIFICATIONS) },
                            onSearchClick = {},
                            onOcrScanClick = { navController.navigate(Routes.OCR_TEST) },
                            onPdfReportClick = { navController.navigate(Routes.LAB_HISTORY) },
                            onConsultationClick = {},
                            onTrendClick = {}
                        )
                    )
                }

                InnerGlowingAddButton(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 26.dp),
                    onClick = { showMainAddModal = true }
                )

                if (showMainAddModal) {
                    MainAddTestBottomSheet(
                        onDismiss = { showMainAddModal = false },
                        onScanImageClick = { showMainAddModal = false; showImageSourceModal = true },
                        onManualEntryClick = { showMainAddModal = false; navController.navigate(Routes.HEALTH_SCORE_DETAIL) }
                    )
                }

                if (showImageSourceModal) {
                    ImageSourceBottomSheet(
                        onDismiss = { showImageSourceModal = false },
                        onFileSelectClick = { showImageSourceModal = false; navController.navigate(Routes.OCR_TEST) },
                        onGallerySelectClick = { showImageSourceModal = false; navController.navigate(Routes.OCR_TEST) },
                        onCameraCaptureClick = { showImageSourceModal = false; navController.navigate(Routes.OCR_TEST) }
                    )
                }

                if (showLabLimitDialog) {
                    LabLimitDialog(
                        onDismiss = { showLabLimitDialog = false },
                        onBuySingleTest = { showLabLimitDialog = false; navController.navigate(Routes.SUBSCRIPTION) },
                        onBuyPremium = { showLabLimitDialog = false; navController.navigate(Routes.SUBSCRIPTION) }
                    )
                }
            }
        }

        // PROFILE
        composable(
            route = Routes.PROFILE,
            arguments = listOf(navArgument("userName") { type = NavType.StringType; defaultValue = "کاربر" })
        ) { entry ->
            val rawName = entry.arguments?.getString("userName") ?: "کاربر"
            val userName = URLDecoder.decode(rawName, StandardCharsets.UTF_8.toString())

            Box(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
                Scaffold(
                    bottomBar = {
                        AvinaBottomNavigationBar(
                            selectedTab = 5,
                            showMiddleSpacer = false,
                            onTabSelected = { tab ->
                                when (tab) {
                                    0 -> navController.navigate(Routes.createHomeRoute(userName)) {
                                        popUpTo(Routes.PROFILE) { inclusive = true }
                                    }
                                    1 -> navController.navigate(Routes.LAB_HISTORY)
                                    else -> {}
                                }
                            }
                        )
                    }
                ) { padding ->
                    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                        ProfileScreen(
                            userName = userName,
                            onSupportClick = { navController.navigate(Routes.SUPPORT) },
                            onLogoutClick = {
                                scope.launch {
                                    userPreferencesManager.clearUserSession()
                                    navController.navigate(Routes.REGISTER) { popUpTo(0) { inclusive = true } }
                                }
                            },
                            onSubscriptionClick = { navController.navigate(Routes.SUBSCRIPTION) },
                            onManageSubscriptionClick = { navController.navigate(Routes.SUBSCRIPTION_MANAGEMENT) },
                            onHistoryClick = { navController.navigate(Routes.LAB_HISTORY) }
                        )
                    }
                }
            }
        }

        // OTHER SCREENS
        composable(Routes.HEALTH_SCORE_DETAIL) { HealthScoreDetailScreen(onBackClick = { navController.popBackStack() }) }
        composable(Routes.AI_RECOMMENDATION) { AiRecommendationScreen(onBackClick = { navController.popBackStack() }) }
        composable(Routes.LAB_HISTORY) {
            LabHistoryScreen(
                userName = savedUserName,
                onBackClick = { navController.popBackStack() },
                onTestClick = { reportId ->
                    // TODO: بعد از اضافه شدن صفحه‌ی جزئیات گزارش، اینجا باید
                    // navController.navigate("report_detail/$reportId") صدا زده شود.
                }
            )
        }
        composable(Routes.NOTIFICATIONS) { NotificationScreen(onBackClick = { navController.popBackStack() }) }
        composable(Routes.OCR_TEST) {
            OcrTestScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        composable(Routes.SUBSCRIPTION) {
            SubscriptionScreen(
                onBackClick = { navController.popBackStack() },
                onSubscribeClick = { plan ->
                    if (plan == "single_test") {
                        usageManager.addPurchasedTest()
                        Toast.makeText(context, "سهمیه اضافه شد!", Toast.LENGTH_SHORT).show()
                    }
                    navController.popBackStack()
                }
            )
        }
        composable(Routes.SUBSCRIPTION_MANAGEMENT) {
            var remaining by remember { mutableStateOf(3) }
            var total by remember { mutableStateOf(3) }
            var premium by remember { mutableStateOf(false) }
            var isLoadingStatus by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                try {
                    val status = com.avina.health.network.LabApiClient.service.getUserStatus()
                    premium = status.isPremium
                    total = status.totalFreeTests
                    remaining = status.remainingFreeTests ?: status.totalFreeTests
                } catch (e: Exception) {
                    // در صورت خطا، مقادیر پیش‌فرض باقی می‌مانند
                } finally {
                    isLoadingStatus = false
                }
            }

            SubscriptionManagementScreen(
                onBackClick = { navController.popBackStack() },
                onUpgradeClick = { navController.navigate(Routes.SUBSCRIPTION) },
                remainingTests = remaining,
                totalTests = total,
                isPremium = premium
            )
        }
        composable(Routes.SUPPORT) {
            val langManager = remember { AppLanguageManager(context) }
            val currentLang by langManager.currentLanguage.collectAsState(initial = "fa")
            SupportScreen(currentLang = currentLang, onBackClick = { navController.popBackStack() })
        }
    }
}

@Composable
private fun InnerGlowingAddButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier
            .size(62.dp)
            .offset(y = (-5).dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .border(
                1.5.dp,
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.1f))),
                CircleShape
            ),
        shape = CircleShape,
        color = Color(0xFF2563EB),
        shadowElevation = 10.dp
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add",
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )
        }
    }
}
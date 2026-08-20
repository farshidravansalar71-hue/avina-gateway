package com.avina.health

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.avina.health.data.AppLanguageManager
import com.avina.health.network.LabApiClient
import com.avina.health.ui.theme.AVINATheme
import com.avina.health.utils.LocalLanguage // دقت کن این باید وجود داشته باشد

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        // تشخیص هوشمند دارک‌مود برای تنظیم استایل نوارهای سیستم
        val isDarkMode = (resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES

        // فیکس کردن رنگ نوار سیستم و انطباق هوشمند با دارک مود
        enableEdgeToEdge(
            statusBarStyle = if (isDarkMode) {
                SystemBarStyle.dark(Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
            },
            navigationBarStyle = if (isDarkMode) {
                SystemBarStyle.dark(Color.parseColor("#111827")) // رنگ تیره برای دارک‌مود
            } else {
                SystemBarStyle.light(Color.parseColor("#F8FAFC"), Color.parseColor("#F8FAFC"))
            }
        )

        super.onCreate(savedInstanceState)

        // مقداردهی اولیه‌ی کلاینت شبکه (باید قبل از هر استفاده از LabApiClient.service انجام شود)
        LabApiClient.init(this)

        setContent {
            // دریافت زبان جاری از مدیریت زبان پروژه
            val languageManager = remember { AppLanguageManager(this) }
            val currentLang by languageManager.currentLanguage.collectAsState(initial = "fa")

            // تنظیم جهت صفحه (فارسی = راست‌چین، انگلیسی = چپ‌چین)
            val direction = if (currentLang == "en") LayoutDirection.Ltr else LayoutDirection.Rtl

            // تزریق سراسری جهت صفحه و زبان به تمام کامپوننت‌های اپلیکیشن
            CompositionLocalProvider(
                LocalLayoutDirection provides direction,
                LocalLanguage provides currentLang
            ) {
                AVINATheme {
                    AvinaNavigation()
                }
            }
        }
    }
}
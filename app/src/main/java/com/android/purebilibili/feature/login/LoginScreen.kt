package com.android.purebilibili.feature.login

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
//  已改用 MaterialTheme.colorScheme.primary
import kotlinx.coroutines.launch

// 登录方式枚举
enum class LoginMethod {
    QR_CODE,    // 扫码登录
    PHONE_SMS,  //  手机短信登录
    WEB_LOGIN   // 网页登录
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: LoginViewModel = viewModel(),
    onLoginSuccess: () -> Unit,
    onClose: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var selectedMethod by remember { mutableStateOf(LoginMethod.QR_CODE) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    
    //  设置沉浸式状态栏和导航栏（进入时修改，离开时恢复）
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        val insetsController = if (window != null) {
            WindowInsetsControllerCompat(window, view)
        } else null
        
        // 保存原始配置
        val originalStatusBarColor = window?.statusBarColor ?: android.graphics.Color.TRANSPARENT
        val originalNavBarColor = window?.navigationBarColor ?: android.graphics.Color.TRANSPARENT
        val originalLightStatusBars = insetsController?.isAppearanceLightStatusBars ?: true
        val originalDecorFits = window?.decorView?.fitsSystemWindows ?: true
        
        // 设置沉浸式
        if (window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            insetsController?.isAppearanceLightStatusBars = false
            insetsController?.isAppearanceLightNavigationBars = false
        }
        
        onDispose {
            // 离开时恢复原始配置
            if (window != null && insetsController != null) {
                WindowCompat.setDecorFitsSystemWindows(window, originalDecorFits)
                window.statusBarColor = originalStatusBarColor
                window.navigationBarColor = originalNavBarColor
                insetsController.isAppearanceLightStatusBars = originalLightStatusBars
            }
        }
    }

    // 第一次进入加载二维码
    LaunchedEffect(Unit) {
        viewModel.loadQrCode()
    }

    // 退出页面时停止轮询
    DisposableEffect(Unit) {
        onDispose { viewModel.stopPolling() }
    }

    // 监听成功
    LaunchedEffect(state) {
        if (state is LoginState.Success) {
            onLoginSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D)) // 深色背景
    ) {
        //  顶部装饰渐变
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    )
                )
        )

        //  浮动装饰圆 (Extracted)
        FloatingDecorations()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // 顶部栏 (Extracted)
            TopBar(onClose = onClose)

            // 主内容
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                //  Logo 和标题 (Extracted)
                BrandingSection()

                Spacer(modifier = Modifier.height(40.dp))

                //  登录方式选择 (Extracted)
                LoginMethodTabs(
                    selectedMethod = selectedMethod,
                    onMethodChange = { selectedMethod = it }
                )

                Spacer(modifier = Modifier.height(24.dp))

                //  登录内容区域
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    AnimatedContent(
                        targetState = selectedMethod,
                        transitionSpec = {
                            fadeIn(tween(300)) + slideInHorizontally { if (targetState.ordinal > initialState.ordinal) it else -it } togetherWith
                                    fadeOut(tween(300)) + slideOutHorizontally { if (targetState.ordinal > initialState.ordinal) -it else it }
                        },
                        label = "login_method"
                    ) { method ->
                        when (method) {
                            LoginMethod.QR_CODE -> QrCodeLoginContent(
                                state = state,
                                onRefresh = { viewModel.loadQrCode() }
                            )
                            LoginMethod.PHONE_SMS -> PhoneLoginContent(
                                state = state,
                                viewModel = viewModel,
                                onLoginSuccess = {
                                    scope.launch { onLoginSuccess() }
                                }
                            )
                            LoginMethod.WEB_LOGIN -> WebLoginContent(
                                onLoginSuccess = {
                                    scope.launch { onLoginSuccess() }
                                }
                            )
                        }
                    }
                }

                //  底部安全提示
                SecurityFooter()

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun SecurityFooter() {
    Text(
        text = "登录即代表同意 Bilibili 服务协议和隐私政策",
        color = Color.White.copy(alpha = 0.3f),
        fontSize = 12.sp,
        modifier = Modifier.padding(bottom = 16.dp)
    )
}
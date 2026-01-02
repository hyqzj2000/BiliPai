package com.android.purebilibili.feature.login

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
//  Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.android.purebilibili.core.store.TokenManager
//  已改用 MaterialTheme.colorScheme.primary
import com.android.purebilibili.core.ui.LoadingAnimation
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch

@Composable
fun FloatingDecorations() {
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val offset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset1"
    )
    val offset2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset2"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // 大圆
        Box(
            modifier = Modifier
                .offset(x = (-60).dp, y = (100 + offset1).dp)
                .size(200.dp)
                .alpha(0.1f)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        )
        // 小圆
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = (60 + offset2).dp)
                .size(120.dp)
                .alpha(0.08f)
                .background(Color(0xFF00D4FF), CircleShape)
        )
    }
}

@Composable
fun TopBar(onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .size(40.dp)
                .background(Color.White.copy(alpha = 0.1f), CircleShape)
        ) {
            Icon(
                CupertinoIcons.Default.ChevronBackward,
                contentDescription = "返回",
                tint = Color.White
            )
        }

        Text(
            text = "安全登录",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.size(40.dp))
    }
}

@Composable
fun BrandingSection() {
    val context = LocalContext.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Logo - 使用 3D 蓝色图标
        Surface(
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 16.dp,
            modifier = Modifier.size(72.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(com.android.purebilibili.R.mipmap.ic_launcher_3d)
                    .crossfade(true)
                    .build(),
                contentDescription = "BiliPai",
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "BiliPai",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Text(
            text = "第三方 Bilibili 客户端",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun LoginMethodTabs(
    selectedMethod: LoginMethod,
    onMethodChange: (LoginMethod) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(4.dp)
                .fillMaxWidth()
            ) {
            LoginMethod.entries.forEach { method ->
                val isSelected = method == selectedMethod
                Surface(
                    onClick = { onMethodChange(method) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (method) {
                                LoginMethod.QR_CODE -> CupertinoIcons.Default.Qrcode
                                LoginMethod.PHONE_SMS -> CupertinoIcons.Default.Iphone
                                LoginMethod.WEB_LOGIN -> CupertinoIcons.Default.Network
                            },
                            contentDescription = null,
                            tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (method) {
                                LoginMethod.QR_CODE -> "扫码"
                                LoginMethod.PHONE_SMS -> "手机号"
                                LoginMethod.WEB_LOGIN -> "网页"
                            },
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QrCodeLoginContent(
    state: LoginState,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        // 二维码卡片
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 24.dp,
            modifier = Modifier.size(260.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(20.dp)
            ) {
                when (state) {
                    is LoginState.Loading -> {
                        LoadingQrCode()
                    }
                    is LoginState.QrCode -> {
                        QrCodeImage(bitmap = state.bitmap)
                    }
                    is LoginState.Scanned -> {
                        ScannedOverlay(bitmap = state.bitmap)
                    }
                    is LoginState.Error -> {
                        ErrorQrCode(message = state.msg, onRetry = onRefresh)
                    }
                    is LoginState.Success -> {
                        SuccessIndicator()
                    }
                    // 手机号登录状态由其他组件处理
                    else -> {
                        LoadingQrCode()
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 提示文字
        when (state) {
            is LoginState.QrCode -> {
                QrCodeHint()
            }
            is LoginState.Scanned -> {
                ScannedHint()
            }
            is LoginState.Error -> {
                TextButton(onClick = onRefresh) {
                    Icon(CupertinoIcons.Default.ArrowClockwise, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("刷新二维码", color = MaterialTheme.colorScheme.primary)
                }
            }
            else -> {}
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun LoadingQrCode() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        //  使用 Lottie 加载动画
        LoadingAnimation(
            size = 64.dp,
            text = "正在加载..."
        )
    }
}

@Composable
private fun QrCodeImage(bitmap: Bitmap) {
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "二维码",
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
    )
}

@Composable
private fun ScannedOverlay(bitmap: Bitmap) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanned")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(contentAlignment = Alignment.Center) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "二维码",
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .alpha(0.2f)
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                modifier = Modifier
                    .size(80.dp)
                    .scale(scale)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        CupertinoIcons.Default.Iphone,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "请在手机上确认",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )
        }
    }
}

@Composable
private fun ErrorQrCode(message: String, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            CupertinoIcons.Default.XmarkCircle,
            contentDescription = null,
            tint = Color(0xFFFF6B6B),
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            color = Color.Gray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SuccessIndicator() {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = 0.5f),
        label = "success_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = Color(0xFF4CAF50),
            modifier = Modifier
                .size(72.dp)
                .scale(scale)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    CupertinoIcons.Default.Checkmark,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "登录成功",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4CAF50)
        )
    }
}

@Composable
private fun QrCodeHint() {
    var showTip by remember { mutableStateOf(false) }
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                CupertinoIcons.Default.Iphone,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "打开「哔哩哔哩」扫一扫",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "首页左上角 → 扫一扫",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 13.sp
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        //  单手机登录提示
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.White.copy(alpha = 0.08f),
            onClick = { showTip = !showTip },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        CupertinoIcons.Default.Lightbulb,
                        contentDescription = null,
                        tint = Color(0xFFFFD54F),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "只有一部手机？点击查看方法",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        if (showTip) CupertinoIcons.Default.ChevronUp else CupertinoIcons.Default.ChevronDown,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                if (showTip) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Color.White.copy(alpha = 0.05f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(10.dp)
                    ) {
                        SinglePhoneTipItem(number = "1", text = "截图保存此二维码")
                        Spacer(modifier = Modifier.height(6.dp))
                        SinglePhoneTipItem(number = "2", text = "打开「哔哩哔哩」→ 扫一扫 → 相册")
                        Spacer(modifier = Modifier.height(6.dp))
                        SinglePhoneTipItem(number = "3", text = "选择截图并确认登录")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "✨ 扫码登录可解锁 4K/HDR 高画质",
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                            fontSize = 11.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SinglePhoneTipItem(number: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
            modifier = Modifier.size(18.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun ScannedHint() {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            color = Color(0xFF4CAF50),
            strokeWidth = 2.dp,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "等待确认中...",
            color = Color(0xFF4CAF50),
            fontSize = 14.sp
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebLoginContent(
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var hasOpenedBrowser by remember { mutableStateOf(false) }
    var checkingLogin by remember { mutableStateOf(false) }
    var loginCheckFailed by remember { mutableStateOf(false) }
    
    // 检查登录状态
    fun checkLoginStatus() {
        scope.launch {
            checkingLogin = true
            loginCheckFailed = false
            try {
                val cookies = CookieManager.getInstance().getCookie("https://passport.bilibili.com")
                    ?: CookieManager.getInstance().getCookie("https://www.bilibili.com")
                com.android.purebilibili.core.util.Logger.d("WebLogin", " 检查 Cookie: $cookies")
                
                if (!cookies.isNullOrEmpty() && cookies.contains("SESSDATA")) {
                    val sessData = cookies.split(";")
                        .map { it.trim() }
                        .find { it.startsWith("SESSDATA=") }
                        ?.substringAfter("SESSDATA=")
                    
                    if (!sessData.isNullOrEmpty()) {
                        com.android.purebilibili.core.util.Logger.d("WebLogin", " 检测到 SESSDATA")
                        TokenManager.saveCookies(context, sessData)
                        onLoginSuccess()
                        return@launch
                    }
                }
                loginCheckFailed = true
            } finally {
                checkingLogin = false
            }
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 提示卡片
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.08f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    CupertinoIcons.Default.Checkmark,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "使用 Bilibili 官方登录页面，更安全",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        // 主卡片
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    CupertinoIcons.Default.Safari,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Text(
                    text = if (hasOpenedBrowser) "在浏览器中完成登录后" else "在浏览器中登录",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = if (hasOpenedBrowser) "返回此应用并点击验证按钮" else "点击下方按钮打开浏览器完成登录",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                if (!hasOpenedBrowser) {
                    // 打开浏览器按钮
                    Button(
                        onClick = {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://passport.bilibili.com/h5-app/passport/login")
                            )
                            context.startActivity(intent)
                            hasOpenedBrowser = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(CupertinoIcons.Default.Safari, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("打开浏览器登录", color = Color.White)
                    }
                } else {
                    // 验证登录状态按钮
                    Button(
                        onClick = { checkLoginStatus() },
                        enabled = !checkingLogin,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (checkingLogin) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("验证中...", color = Color.White)
                        } else {
                            Icon(CupertinoIcons.Default.Checkmark, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("验证登录状态", color = Color.White)
                        }
                    }
                    
                    if (loginCheckFailed) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "未检测到登录，请先在浏览器中完成登录",
                            fontSize = 12.sp,
                            color = Color.Red,
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 重新打开浏览器
                    TextButton(onClick = {
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://passport.bilibili.com/h5-app/passport/login")
                        )
                        context.startActivity(intent)
                    }) {
                        Text("重新打开浏览器", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

/**
 *  手机号登录内容
 */
@Composable
fun PhoneLoginContent(
    state: LoginState,
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    
    var phoneNumber by remember { mutableStateOf("") }
    var smsCode by remember { mutableStateOf("") }
    var isPhoneValid by remember { mutableStateOf(false) }
    var showCaptcha by remember { mutableStateOf(false) }
    var captchaManager by remember { mutableStateOf<CaptchaManager?>(null) }
    
    // 监听状态变化
    LaunchedEffect(state) {
        when (state) {
            is LoginState.Success -> onLoginSuccess()
            else -> {}
        }
    }
    
    // 清理资源
    DisposableEffect(Unit) {
        onDispose {
            captchaManager?.destroy()
        }
    }
    
    // 验证手机号格式
    fun validatePhone(phone: String): Boolean {
        return phone.length == 11 && phone.startsWith("1")
    }
    
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        //  画质限制警告卡片
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFFF9800).copy(alpha = 0.15f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    CupertinoIcons.Default.ExclamationmarkTriangle,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "手机号登录最高 1080P",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "使用「扫码登录」可解锁 4K/HDR 高画质",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        // 主卡片
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (state) {
                    is LoginState.Loading -> {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.height(200.dp)
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    
                    is LoginState.SmsSent -> {
                        // 短信已发送，输入验证码
                        SmsCodeInputSection(
                            smsCode = smsCode,
                            onSmsCodeChange = { smsCode = it },
                            onSubmit = {
                                val code = smsCode.toIntOrNull()
                                if (code != null) {
                                    viewModel.loginBySms(code)
                                }
                            },
                            isLoading = false
                        )
                    }
                    
                    is LoginState.Error -> {
                        //  只有手机登录相关的错误才显示错误界面
                        val isPhoneError = state.msg.contains("手机") || 
                                          state.msg.contains("短信") || 
                                          state.msg.contains("验证码") ||
                                          state.msg.contains("验证")
                        if (isPhoneError) {
                            ErrorSection(
                                message = state.msg,
                                onRetry = { viewModel.resetPhoneLogin() }
                            )
                        } else {
                            // QR 相关的错误，在手机号登录界面显示输入
                            PhoneInputSection(
                                phoneNumber = phoneNumber,
                                onPhoneChange = {
                                    phoneNumber = it.filter { c -> c.isDigit() }.take(11)
                                    isPhoneValid = validatePhone(phoneNumber)
                                },
                                isValid = isPhoneValid,
                                onGetCaptcha = {
                                    if (isPhoneValid) {
                                        viewModel.getCaptcha()
                                    }
                                },
                                onStartCaptcha = { captchaData ->
                                    activity?.let { act ->
                                        captchaManager = CaptchaManager(act)
                                        captchaManager?.startCaptcha(
                                            gt = captchaData.geetest?.gt ?: "",
                                            challenge = captchaData.geetest?.challenge ?: "",
                                            onSuccess = { validate, seccode, challenge ->
                                                viewModel.saveCaptchaResult(validate, seccode, challenge)
                                                viewModel.sendSmsCode(phoneNumber.toLong())
                                            },
                                            onFailed = { error ->
                                                android.util.Log.e("PhoneLogin", "Captcha failed: $error")
                                            }
                                        )
                                    }
                                },
                                captchaReady = false,
                                captchaData = null
                            )
                        }
                    }
                    
                    else -> {
                        // 输入手机号
                        PhoneInputSection(
                            phoneNumber = phoneNumber,
                            onPhoneChange = {
                                phoneNumber = it.filter { c -> c.isDigit() }.take(11)
                                isPhoneValid = validatePhone(phoneNumber)
                            },
                            isValid = isPhoneValid,
                            onGetCaptcha = {
                                if (isPhoneValid) {
                                    viewModel.getCaptcha()
                                }
                            },
                            onStartCaptcha = { captchaData ->
                                activity?.let { act ->
                                    captchaManager = CaptchaManager(act)
                                    captchaManager?.startCaptcha(
                                        gt = captchaData.geetest?.gt ?: "",
                                        challenge = captchaData.geetest?.challenge ?: "",
                                        onSuccess = { validate, seccode, challenge ->
                                            viewModel.saveCaptchaResult(validate, seccode, challenge)
                                            viewModel.sendSmsCode(phoneNumber.toLong())
                                        },
                                        onFailed = { error ->
                                            android.util.Log.e("PhoneLogin", "Captcha failed: $error")
                                        }
                                    )
                                }
                            },
                            captchaReady = state is LoginState.CaptchaReady,
                            captchaData = (state as? LoginState.CaptchaReady)?.captchaData
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhoneInputSection(
    phoneNumber: String,
    onPhoneChange: (String) -> Unit,
    isValid: Boolean,
    onGetCaptcha: () -> Unit,
    onStartCaptcha: (com.android.purebilibili.data.model.response.CaptchaData) -> Unit,
    captchaReady: Boolean,
    captchaData: com.android.purebilibili.data.model.response.CaptchaData?
) {
    // 当验证码准备好时自动触发
    LaunchedEffect(captchaReady) {
        if (captchaReady && captchaData != null) {
            onStartCaptcha(captchaData)
        }
    }
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            CupertinoIcons.Default.Iphone,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "手机号登录",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333333)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 手机号输入框
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = onPhoneChange,
            label = { Text("手机号", color = MaterialTheme.colorScheme.primary) },
            placeholder = { Text("请输入11位手机号", color = Color.Gray) },
            leadingIcon = {
                Text("+86", color = Color(0xFF333333), fontSize = 14.sp, fontWeight = FontWeight.Medium)
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            textStyle = androidx.compose.ui.text.TextStyle(
                color = Color(0xFF333333),  //  深色输入文字
                fontSize = 16.sp
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color(0xFFDDDDDD),  //  未聚焦边框灰色
                focusedTextColor = Color(0xFF333333),
                unfocusedTextColor = Color(0xFF333333),
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = Color.Gray
            )
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 获取验证码按钮
        Button(
            onClick = onGetCaptcha,
            enabled = isValid,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                disabledContainerColor = Color(0xFFFFD0DC),  //  禁用时浅粉色背景
                disabledContentColor = Color(0xFFCC8899)     //  禁用时深粉色文字
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(
                text = if (isValid) "获取验证码" else "请输入手机号",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SmsCodeInputSection(
    smsCode: String,
    onSmsCodeChange: (String) -> Unit,
    onSubmit: () -> Unit,
    isLoading: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            CupertinoIcons.Default.Envelope,
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(48.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "输入验证码",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333333)
        )
        
        Text(
            text = "验证码已发送到您的手机",
            fontSize = 14.sp,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 验证码输入框
        OutlinedTextField(
            value = smsCode,
            onValueChange = { onSmsCodeChange(it.filter { c -> c.isDigit() }.take(6)) },
            label = { Text("验证码") },
            placeholder = { Text("请输入6位验证码") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF4CAF50),
                cursorColor = Color(0xFF4CAF50)
            )
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 登录按钮
        Button(
            onClick = onSubmit,
            enabled = smsCode.length == 6 && !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text("登录", color = Color.White)
            }
        }
    }
}

@Composable
private fun ErrorSection(
    message: String,
    onRetry: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            CupertinoIcons.Default.XmarkCircle,
            contentDescription = null,
            tint = Color(0xFFFF6B6B),
            modifier = Modifier.size(48.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = message,
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("重试", color = Color.White)
        }
    }
}

package com.android.purebilibili.feature.login

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.util.Base64
import com.android.purebilibili.core.util.Logger
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher

/**
 * 极验验证管理器 (WebView 方案)
 * 使用 WebView 加载极验验证，无需外部 SDK 依赖
 */
class CaptchaManager(private val activity: Activity) {
    
    companion object {
        private const val TAG = "CaptchaManager"
    }
    
    private var webView: WebView? = null
    private var dialog: AlertDialog? = null
    
    /**
     * 初始化并启动极验验证
     * @param gt 极验 ID (从 B站 API 获取)
     * @param challenge 极验 challenge (从 B站 API 获取)
     * @param onSuccess 验证成功回调，返回 validate 和 seccode
     * @param onFailed 验证失败回调
     * @param onCancel 用户取消回调
     */
    @SuppressLint("SetJavaScriptEnabled")
    fun startCaptcha(
        gt: String,
        challenge: String,
        onSuccess: (validate: String, seccode: String, challenge: String) -> Unit,
        onFailed: (error: String) -> Unit,
        onCancel: () -> Unit = {}
    ) {
        try {
            Logger.d(TAG, "Starting WebView captcha with gt=$gt, challenge=$challenge")
            
            // 创建 WebView
            webView = WebView(activity).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        Logger.d(TAG, "Captcha page loaded")
                    }
                }
                
                webChromeClient = WebChromeClient()
                
                // 添加 JavaScript 接口
                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onCaptchaSuccess(validate: String, seccode: String, newChallenge: String) {
                        Logger.d(TAG, "Captcha success via JS: validate=$validate, challenge=$newChallenge")
                        activity.runOnUiThread {
                            dialog?.dismiss()
                            //  使用验证后返回的新 challenge
                            onSuccess(validate, seccode, newChallenge)
                        }
                    }
                    
                    @JavascriptInterface
                    fun onCaptchaFailed(error: String) {
                        com.android.purebilibili.core.util.Logger.e(TAG, "Captcha failed via JS: $error")
                        activity.runOnUiThread {
                            dialog?.dismiss()
                            onFailed(error)
                        }
                    }
                    
                    @JavascriptInterface
                    fun onCaptchaCancel() {
                        Logger.d(TAG, "Captcha cancelled")
                        activity.runOnUiThread {
                            dialog?.dismiss()
                            onCancel()
                        }
                    }
                }, "Android")
            }
            
            // 加载极验验证 HTML
            val html = generateGeetestHtml(gt, challenge)
            webView?.loadDataWithBaseURL(
                "https://www.bilibili.com",
                html,
                "text/html",
                "UTF-8",
                null
            )
            
            // 显示对话框 - 使用全屏对话框
            dialog = AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen)
                .setView(webView)
                .setOnCancelListener {
                    onCancel()
                }
                .create()
            
            dialog?.show()
            
            //  设置对话框为接近全屏
            dialog?.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            
        } catch (e: Exception) {
            com.android.purebilibili.core.util.Logger.e(TAG, "Failed to start captcha", e)
            onFailed("验证初始化失败: ${e.message}")
        }
    }
    
    /**
     * 生成极验验证 HTML
     */
    private fun generateGeetestHtml(gt: String, challenge: String): String {
        return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>安全验证</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        html, body {
            width: 100%;
            height: 100%;
            background: #f7f7f7;
            font-family: -apple-system, BlinkMacSystemFont, sans-serif;
        }
        .container {
            width: 100%;
            min-height: 100%;
            display: flex;
            flex-direction: column;
            justify-content: flex-start;
            align-items: center;
            padding: 20px;
            padding-top: 60px;
        }
        .title {
            font-size: 20px;
            font-weight: 600;
            color: #333;
            margin-bottom: 24px;
        }
        #captcha-container {
            width: 100%;
            max-width: 340px;
            background: white;
            border-radius: 16px;
            padding: 20px;
            box-shadow: 0 2px 12px rgba(0,0,0,0.08);
        }
        .loading {
            text-align: center;
            color: #999;
            padding: 40px 0;
            font-size: 14px;
        }
        .loading::after {
            content: '';
            display: inline-block;
            width: 16px;
            height: 16px;
            border: 2px solid #fb7299;
            border-top-color: transparent;
            border-radius: 50%;
            animation: spin 0.8s linear infinite;
            margin-left: 8px;
            vertical-align: middle;
        }
        @keyframes spin {
            to { transform: rotate(360deg); }
        }
        .tip {
            text-align: center;
            color: #999;
            font-size: 12px;
            margin-top: 16px;
        }
    </style>
    <script src="https://static.geetest.com/static/js/gt.0.5.0.js"></script>
</head>
<body>
    <div class="container">
        <div class="title">请完成验证</div>
        <div id="captcha-container">
            <div class="loading">加载中</div>
        </div>
        <div class="tip">点击图片上的文字完成验证</div>
    </div>
    
    <script>
        window.initGeetest({
            gt: "$gt",
            challenge: "$challenge",
            offline: false,
            new_captcha: true,
            product: "bind",
            width: "100%"
        }, function(captchaObj) {
            captchaObj.appendTo("#captcha-container");
            
            captchaObj.onReady(function() {
                document.querySelector('.loading').style.display = 'none';
                captchaObj.verify();
            });
            
            captchaObj.onSuccess(function() {
                var result = captchaObj.getValidate();
                if (result) {
                    window.Android.onCaptchaSuccess(
                        result.geetest_validate,
                        result.geetest_seccode,
                        result.geetest_challenge || "$challenge"
                    );
                } else {
                    window.Android.onCaptchaFailed("验证结果为空");
                }
            });
            
            captchaObj.onError(function(e) {
                window.Android.onCaptchaFailed(e.msg || e.error_code || "验证失败");
            });
            
            captchaObj.onClose(function() {
                window.Android.onCaptchaCancel();
            });
        });
    </script>
</body>
</html>
        """.trimIndent()
    }
    
    /**
     * 销毁资源
     */
    fun destroy() {
        dialog?.dismiss()
        webView?.destroy()
        webView = null
        dialog = null
    }
}

/**
 * RSA 加密工具
 * 用于密码登录时加密密码
 */
object RsaEncryption {
    private const val TAG = "RsaEncryption"
    
    /**
     * 使用 RSA 公钥加密密码
     * @param password 原始密码
     * @param publicKey RSA 公钥 (PEM 格式)
     * @param salt 盐值 (hash)
     * @return Base64 编码的加密密码
     */
    fun encryptPassword(password: String, publicKey: String, salt: String): String? {
        return try {
            // 处理公钥字符串
            val keyStr = publicKey
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("\\s+".toRegex(), "")
            
            // 解码公钥
            val keyBytes = Base64.decode(keyStr, Base64.DEFAULT)
            val keySpec = X509EncodedKeySpec(keyBytes)
            val keyFactory = KeyFactory.getInstance("RSA")
            val pubKey = keyFactory.generatePublic(keySpec)
            
            // 加密 (salt + password)
            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.ENCRYPT_MODE, pubKey)
            val encryptedBytes = cipher.doFinal((salt + password).toByteArray())
            
            // Base64 编码
            Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            com.android.purebilibili.core.util.Logger.e(TAG, "Failed to encrypt password", e)
            null
        }
    }
}

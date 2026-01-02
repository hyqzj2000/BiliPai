// 文件路径: feature/settings/IconSettingsScreen.kt
package com.android.purebilibili.feature.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.purebilibili.R
import com.android.purebilibili.core.theme.*
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.CheckmarkCircle

/**
 *  应用图标设置二级页面
 * 网格布局展示所有可选图标
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconSettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    
    // 图标选项数据
    data class IconOption(val key: String, val name: String, val desc: String, val iconRes: Int)
    val iconOptions = listOf(
        IconOption("3D", "3D立体", "默认", R.mipmap.ic_launcher_3d),
        IconOption("Blue", "经典蓝", "原版", R.mipmap.ic_launcher_blue),
        IconOption("Retro", "复古怀旧", "80年代", R.mipmap.ic_launcher_retro),
        IconOption("Flat", "扁平现代", "Material", R.mipmap.ic_launcher_flat),
        IconOption("Flat Material", "扁平材质", "Material You", R.mipmap.ic_launcher_flat_material),
        IconOption("Neon", "霓虹", "夜间", R.mipmap.ic_launcher_neon),
        IconOption("Telegram Blue", "纸飞机蓝", "Telegram", R.mipmap.ic_launcher_telegram_blue),
        IconOption("Pink", "樱花粉", "可爱", R.mipmap.ic_launcher_telegram_pink),
        IconOption("Purple", "香芋紫", "梦幻", R.mipmap.ic_launcher_telegram_purple),
        IconOption("Green", "薄荷绿", "清新", R.mipmap.ic_launcher_telegram_green),
        IconOption("Dark", "暗夜蓝", "深色模式", R.mipmap.ic_launcher_telegram_dark)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("应用图标", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(CupertinoIcons.Default.ChevronBackward, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // 当前选择提示
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        CupertinoIcons.Default.InfoCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "切换图标后可能需要几秒钟生效",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            // 图标网格
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(iconOptions) { option ->
                    val isSelected = state.appIcon == option.key
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else Color.Transparent
                            )
                            .clickable {
                                if (!isSelected) {
                                    Toast.makeText(context, "正在切换图标...", Toast.LENGTH_SHORT).show()
                                    viewModel.setAppIcon(option.key)
                                }
                            }
                            .padding(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(68.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            //  iOS 风格圆角矩形图标
                            AsyncImage(
                                model = option.iconRes,
                                contentDescription = option.name,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(13.5.dp))  // iOS比例: 22.37%
                            )
                            
                            // 选中标记
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(
                                        CupertinoIcons.Filled.CheckmarkCircle,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .align(Alignment.Center)
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = option.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary 
                                    else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        
                        Text(
                            text = option.desc,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

package com.nfcpressure.app

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.NfcV
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nfcpressure.app.ui.screens.MainScreen
import com.nfcpressure.app.ui.theme.NFCPressureAppTheme
import com.nfcpressure.app.viewmodel.PressureViewModel

/**
 * 主Activity
 * 处理NFC标签读取和应用UI
 */
class MainActivity : ComponentActivity() {
    
    // NFC适配器
    private var nfcAdapter: NfcAdapter? = null
    
    // PendingIntent用于NFC回调
    private lateinit var pendingIntent: PendingIntent
    
    // Intent过滤器
    private lateinit var intentFilters: Array<IntentFilter>
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // 初始化NFC适配器
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        
        // 创建PendingIntent
        val intent = Intent(this, javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        pendingIntent = PendingIntent.getActivity(this, 0, intent, flags)
        
        // 创建IntentFilter - 匹配NDEF和Tech发现
        val ndefFilter = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
            try {
                addDataType("text/plain")
            } catch (e: IntentFilter.MalformedMimeTypeException) {
                e.printStackTrace()
            }
        }
        
        val techFilter = IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
        
        intentFilters = arrayOf(ndefFilter, techFilter)
        
        setContent {
            NFCPressureAppTheme {
                val viewModel: PressureViewModel = viewModel()
                val context = LocalContext.current
                
                // 监听UI状态
                val uiState by viewModel.uiState.collectAsState()
                
                // 显示错误Toast
                LaunchedEffect(uiState.errorMessage) {
                    uiState.errorMessage?.let { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        viewModel.clearError()
                    }
                }
                
                // 显示成功Toast
                LaunchedEffect(uiState.successMessage) {
                    uiState.successMessage?.let { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        viewModel.clearSuccess()
                    }
                }
                
                Scaffold(
                    snackbarHost = {
                        // SnackbarHost for additional messages
                    }
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        MainScreen(
                            uiState = uiState,
                            onSetZeroPoint = viewModel::setZeroPoint,
                            onSetReferencePoint = viewModel::setReferencePoint,
                            onResetCalibration = viewModel::resetCalibration,
                            onClearHistory = viewModel::clearHistory
                        )
                        
                        // NFC未启用提示
                        if (!uiState.isNfcEnabled && uiState.isNfcSupported) {
                            NfcDisabledBanner(
                                onClick = {
                                    // 打开NFC设置
                                    val nfcSettingsIntent = Intent(Settings.ACTION_NFC_SETTINGS)
                                    startActivity(nfcSettingsIntent)
                                },
                                modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter)
                            )
                        }
                        
                        // NFC不支持提示
                        if (!uiState.isNfcSupported) {
                            NfcNotSupportedBanner(
                                modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter)
                            )
                        }
                    }
                }
            }
        }
        
        // 处理启动Intent
        handleIntent(intent)
    }
    
    override fun onResume() {
        super.onResume()
        
        // 更新NFC状态
        val viewModel = (viewModel as? PressureViewModel)
        viewModel?.updateNfcState(nfcAdapter?.isEnabled == true)
        
        // 启用前台调度
        nfcAdapter?.let { adapter ->
            if (adapter.isEnabled) {
                adapter.enableForegroundDispatch(this, pendingIntent, intentFilters, null)
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        
        // 禁用前台调度
        nfcAdapter?.disableForegroundDispatch(this)
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
    
    /**
     * 处理NFC Intent
     */
    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        
        when (intent.action) {
            NfcAdapter.ACTION_NDEF_DISCOVERED,
            NfcAdapter.ACTION_TECH_DISCOVERED,
            NfcAdapter.ACTION_TAG_DISCOVERED -> {
                // 获取Tag对象
                val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
                }
                
                tag?.let { processTag(it) }
            }
        }
    }
    
    /**
     * 处理NFC标签
     */
    private fun processTag(tag: Tag) {
        // 检查是否为NfcV (ISO 15693)
        val techList = tag.techList
        
        if (techList.any { it.contains("NfcV") }) {
            // 使用ViewModel处理
            val viewModel = (viewModel as? PressureViewModel)
            viewModel?.handleNfcTag(tag)
        } else {
            // 不支持的标签类型
            Toast.makeText(
                this,
                "不支持的NFC标签类型，请使用ISO 15693标签",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

/**
 * NFC未启用提示条
 */
@Composable
private fun NfcDisabledBanner(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            Text(
                text = "⚠️ NFC未启用，点击打开设置",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

/**
 * NFC不支持提示条
 */
@Composable
private fun NfcNotSupportedBanner(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "❌ 当前设备不支持NFC功能",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

package com.nfcpressure.app

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.nfcpressure.app.ui.screens.MainScreen
import com.nfcpressure.app.ui.theme.NFCPressureAppTheme
import com.nfcpressure.app.viewmodel.PressureViewModel

class MainActivity : ComponentActivity() {
    
    private var nfcAdapter: NfcAdapter? = null
    private lateinit var viewModel: PressureViewModel
    private var readerModeEnabled = false
    
    private val readerCallback = NfcAdapter.ReaderCallback { tag ->
        processTag(tag)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        viewModel = ViewModelProvider(this)[PressureViewModel::class.java]
        
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        
        setContent {
            NFCPressureAppTheme {
                val uiState by viewModel.uiState.collectAsState()
                val context = LocalContext.current
                
                LaunchedEffect(uiState.errorMessage) {
                    uiState.errorMessage?.let { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        viewModel.clearError()
                    }
                }
                
                LaunchedEffect(uiState.successMessage) {
                    uiState.successMessage?.let { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        viewModel.clearSuccess()
                    }
                }
                
                Scaffold { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        MainScreen(
                            uiState = uiState,
                            onStartReading = { startReaderMode() },
                            onStopReading = { stopReaderMode() },
                            onSetZeroPoint = viewModel::setZeroPoint,
                            onSetReferencePoint = viewModel::setReferencePoint,
                            onResetCalibration = viewModel::resetCalibration,
                            onClearHistory = viewModel::clearHistory
                        )
                        
                        if (!uiState.isNfcEnabled && uiState.isNfcSupported) {
                            NfcDisabledBanner(
                                onClick = {
                                    startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
                                },
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )
                        }
                        
                        if (!uiState.isNfcSupported) {
                            NfcNotSupportedBanner(
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )
                        }
                    }
                }
            }
        }
        
        handleIntent(intent)
    }
    
    override fun onResume() {
        super.onResume()
        viewModel.updateNfcState(nfcAdapter?.isEnabled == true)
    }
    
    override fun onPause() {
        super.onPause()
        stopReaderMode()
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
    
    fun startReaderMode() {
        nfcAdapter?.let { adapter ->
            if (adapter.isEnabled && !readerModeEnabled) {
                viewModel.startReading()
                // FLAG_READER_NFC_V for ISO 15693 tags
                adapter.enableReaderMode(this, readerCallback, NfcAdapter.FLAG_READER_NFC_V, null)
                readerModeEnabled = true
            }
        }
    }
    
    fun stopReaderMode() {
        nfcAdapter?.let { adapter ->
            if (readerModeEnabled) {
                adapter.disableReaderMode(this)
                readerModeEnabled = false
                viewModel.stopReading()
            }
        }
    }
    
    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            NfcAdapter.ACTION_NDEF_DISCOVERED,
            NfcAdapter.ACTION_TECH_DISCOVERED,
            NfcAdapter.ACTION_TAG_DISCOVERED -> {
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
    
    private fun processTag(tag: Tag) {
        if (tag.techList.any { it.contains("NfcV") }) {
            viewModel.handleNfcTag(tag)
            // 读到数据后自动停止ReaderMode
            stopReaderMode()
        } else {
            Toast.makeText(this, "不支持的NFC标签类型，请使用ISO 15693标签", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
private fun NfcDisabledBanner(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "⚠️ NFC未启用，点击打开设置",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun NfcNotSupportedBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
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

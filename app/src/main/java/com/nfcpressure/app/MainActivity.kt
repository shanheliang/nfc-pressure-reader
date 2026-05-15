package com.nfcpressure.app

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
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
    private lateinit var pendingIntent: PendingIntent
    private lateinit var intentFilters: Array<IntentFilter>
    private lateinit var viewModel: PressureViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[PressureViewModel::class.java]
        
        // Initialize NFC
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        
        val intent = Intent(this, javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        pendingIntent = PendingIntent.getActivity(this, 0, intent, flags)
        
        val ndefFilter = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
            try { addDataType("text/plain") } 
            catch (e: IntentFilter.MalformedMimeTypeException) { e.printStackTrace() }
        }
        val techFilter = IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
        intentFilters = arrayOf(ndefFilter, techFilter)
        
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
        nfcAdapter?.let { adapter ->
            if (adapter.isEnabled) {
                adapter.enableForegroundDispatch(this, pendingIntent, intentFilters, null)
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
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

package com.nfcpressure.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nfcpressure.app.data.CalibrationData
import com.nfcpressure.app.data.HistoryItem
import com.nfcpressure.app.data.PressureData
import com.nfcpressure.app.ui.components.*
import com.nfcpressure.app.ui.theme.*
import com.nfcpressure.app.viewmodel.UiState
import kotlinx.coroutines.delay

/**
 * 主屏幕
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: UiState,
    onSetZeroPoint: () -> Unit,
    onSetReferencePoint: (Float) -> Unit,
    onResetCalibration: () -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "NFC压力监测",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                ),
                actions = {
                    // NFC状态指示
                    Icon(
                        imageVector = if (uiState.isNfcEnabled) Icons.Default.SignalCellular4Bar 
                                      else Icons.Default.SignalCellularOff,
                        contentDescription = "NFC状态",
                        tint = if (uiState.isNfcEnabled) Color.White else Color.Gray,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab栏
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("监测") },
                    icon = { Icon(Icons.Default.Monitor, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("校准") },
                    icon = { Icon(Icons.Default.Tune, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("历史") },
                    icon = { Icon(Icons.Default.History, contentDescription = null) }
                )
            }
            
            // Tab内容
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "tabContent"
            ) { tab ->
                when (tab) {
                    0 -> MonitorTab(uiState)
                    1 -> CalibrationTab(
                        uiState = uiState,
                        onSetZeroPoint = onSetZeroPoint,
                        onSetReferencePoint = onSetReferencePoint,
                        onResetCalibration = onResetCalibration
                    )
                    2 -> HistoryTab(
                        history = uiState.history,
                        onClearHistory = onClearHistory
                    )
                }
            }
        }
    }
    
    // 错误提示Snackbar
    uiState.errorMessage?.let { error ->
        LaunchedEffect(error) {
            delay(3000)
        }
    }
}

/**
 * 监测Tab
 */
@Composable
private fun MonitorTab(uiState: UiState) {
    val pressureData = uiState.pressureData
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 提示区域
        if (pressureData == null && !uiState.isReading) {
            Spacer(modifier = Modifier.weight(1f))
            
            // NFC图标动画
            Icon(
                imageVector = Icons.Default.Nfc,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "将手机贴近NFC标签",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "确保手机NFC天线区域对准传感器卡片",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.weight(1f))
        }
        
        // 读取中状态
        if (uiState.isReading) {
            Spacer(modifier = Modifier.weight(1f))
            
            CircularProgressIndicator(
                modifier = Modifier.size(60.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "正在读取...",
                style = MaterialTheme.typography.bodyLarge
            )
            
            Spacer(modifier = Modifier.weight(1f))
        }
        
        // 显示数据
        if (pressureData != null && !uiState.isReading) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // 仪表盘
            PressureGauge(
                pressure = pressureData.pressureMmHg,
                modifier = Modifier.size(220.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 数据详情卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // 状态行
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (pressureData.isValid) StatusValid 
                                        else StatusInvalid
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (pressureData.isValid) "数据有效" else "数据无效",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (pressureData.isValid) StatusValid else StatusInvalid
                            )
                        }
                        
                        // 电池指示
                        val batteryText = when (pressureData.batteryIndicator) {
                            1 -> "2.5V"
                            2 -> "3.0V"
                            3 -> "3.3V"
                            else -> "未知"
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.BatteryFull,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.Gray
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = batteryText,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 详细数据
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        DataItem(
                            label = "ADC原始值",
                            value = pressureData.adcRaw.toString(),
                            suffix = "/4095"
                        )
                        DataItem(
                            label = "FSR阻值",
                            value = pressureData.formattedResistance,
                            suffix = ""
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        DataItem(
                            label = "ADC百分比",
                            value = String.format("%.1f", pressureData.adcPercentage),
                            suffix = "%"
                        )
                        DataItem(
                            label = "力值",
                            value = String.format("%.2f", pressureData.forceNewton),
                            suffix = "N"
                        )
                    }
                }
            }
            
            // UID信息
            uiState.rawData?.let { raw ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "标签UID: ${raw.uidHex}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

/**
 * 数据项
 */
@Composable
private fun DataItem(
    label: String,
    value: String,
    suffix: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
            Text(
                text = suffix,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 2.dp, start = 2.dp)
            )
        }
    }
}

/**
 * 校准Tab
 */
@Composable
private fun CalibrationTab(
    uiState: UiState,
    onSetZeroPoint: () -> Unit,
    onSetReferencePoint: (Float) -> Unit,
    onResetCalibration: () -> Unit
) {
    val calibration = uiState.calibration
    var referencePressure by remember { mutableStateOf("40") }
    var showResetConfirm by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "两点校准",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "通过两点校准可以获得更准确的压力值",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 校准步骤
        CalibrationStepCard(
            stepNumber = 1,
            title = "设置零点",
            description = "移除所有压力，确保传感器处于无压力状态后点击",
            isCompleted = calibration.zeroAdc >= 0,
            isEnabled = uiState.pressureData != null,
            buttonText = if (calibration.zeroAdc >= 0) "已设置" else "设置零点",
            onClick = onSetZeroPoint,
            currentValue = if (calibration.zeroAdc >= 0) "ADC: ${calibration.zeroAdc}" else null
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        CalibrationStepCard(
            stepNumber = 2,
            title = "设置参考点",
            description = "施加已知压力（建议40mmHg）后输入实际压力值并点击",
            isCompleted = calibration.isCalibrated,
            isEnabled = calibration.zeroAdc >= 0 && uiState.pressureData != null,
            buttonText = if (calibration.isCalibrated) "已设置" else "设置参考",
            onClick = { 
                val pressure = referencePressure.toFloatOrNull() ?: 40f
                onSetReferencePoint(pressure)
            }
        ) {
            // 参考压力输入
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "参考压力:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = referencePressure,
                    onValueChange = { referencePressure = it.filter { c -> c.isDigit() || c == '.' } },
                    modifier = Modifier.width(100.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    suffix = { Text("mmHg") },
                    enabled = !calibration.isCalibrated
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // 校准状态和重置
        if (calibration.isCalibrated) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = GaugeGreen.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = GaugeGreen
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "校准已完成",
                            style = MaterialTheme.typography.bodyLarge,
                            color = GaugeGreen
                        )
                    }
                    
                    TextButton(
                        onClick = { showResetConfirm = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("重置")
                    }
                }
            }
        }
        
        // 校准提示
        if (!calibration.isCalibrated) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = GaugeYellow.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = GaugeYellow,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "请先进行校准以获得准确的压力读数",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
    
    // 重置确认对话框
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("确认重置") },
            text = { Text("确定要重置校准数据吗？重置后需要重新校准。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onResetCalibration()
                        showResetConfirm = false
                    }
                ) {
                    Text("确认重置", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 校准步骤卡片
 */
@Composable
private fun CalibrationStepCard(
    stepNumber: Int,
    title: String,
    description: String,
    isCompleted: Boolean,
    isEnabled: Boolean,
    buttonText: String,
    onClick: () -> Unit,
    currentValue: String? = null,
    content: @Composable (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) GaugeGreen.copy(alpha = 0.05f) 
                             else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 步骤编号
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCompleted) GaugeGreen 
                            else MaterialTheme.colorScheme.primary
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text(
                            text = stepNumber.toString(),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            
            // 当前值显示
            currentValue?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = GaugeGreen
                )
            }
            
            // 额外内容
            content?.invoke()
            
            // 按钮
            if (!isCompleted) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onClick,
                    enabled = isEnabled,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(buttonText)
                }
                
                if (!isEnabled) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "请先读取NFC标签",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

/**
 * 历史记录Tab
 */
@Composable
private fun HistoryTab(
    history: List<HistoryItem>,
    onClearHistory: () -> Unit
) {
    var showClearConfirm by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        HistoryListHeader(
            itemCount = history.size,
            onClearClick = { showClearConfirm = true }
        )
        
        if (history.isEmpty()) {
            EmptyHistoryMessage(
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = history,
                    key = { it.id }
                ) { item ->
                    HistoryItemCard(item = item)
                }
            }
        }
    }
    
    // 清除确认对话框
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("确认清除") },
            text = { Text("确定要清除所有历史记录吗？此操作无法撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearHistory()
                        showClearConfirm = false
                    }
                ) {
                    Text("清除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

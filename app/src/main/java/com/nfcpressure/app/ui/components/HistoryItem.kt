package com.nfcpressure.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nfcpressure.app.data.HistoryItem
import com.nfcpressure.app.ui.theme.GaugeGreen
import com.nfcpressure.app.ui.theme.GaugeRed
import com.nfcpressure.app.ui.theme.GaugeYellow

/**
 * 历史记录列表项
 */
@Composable
fun HistoryItemCard(
    item: HistoryItem,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null
) {
    val pressureColor = when {
        item.pressureMmHg < 0 -> Color.Gray
        item.pressureMmHg <= 30 -> GaugeGreen
        item.pressureMmHg <= 50 -> GaugeYellow
        else -> GaugeRed
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 小仪表盘
            MiniPressureGauge(
                pressure = item.pressureMmHg,
                modifier = Modifier.size(50.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 数值和详情
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.Baseline
                ) {
                    Text(
                        text = if (item.pressureMmHg < 0) "--" 
                               else String.format("%.1f", item.pressureMmHg),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = pressureColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "mmHg",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    
                    if (item.isCalibrated) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(GaugeGreen.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "已校准",
                                style = MaterialTheme.typography.labelSmall,
                                color = GaugeGreen
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row {
                    Text(
                        text = "ADC: ${item.adcRaw}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    val resistanceStr = when {
                        item.fsrResistance >= 1_000_000 -> 
                            String.format("%.1fMΩ", item.fsrResistance / 1_000_000)
                        item.fsrResistance >= 1000 -> 
                            String.format("%.1fkΩ", item.fsrResistance / 1000)
                        item.fsrResistance > 0 -> 
                            String.format("%.0fΩ", item.fsrResistance)
                        else -> "∞"
                    }
                    Text(
                        text = "FSR: $resistanceStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            
            // 时间
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = item.formattedTime,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }
    }
}

/**
 * 历史记录列表头部
 */
@Composable
fun HistoryListHeader(
    itemCount: Int,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "历史记录 ($itemCount)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        if (itemCount > 0) {
            TextButton(onClick = onClearClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "清除",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("清除")
            }
        }
    }
}

/**
 * 空历史记录提示
 */
@Composable
fun EmptyHistoryMessage(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "暂无历史记录",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "贴近NFC标签读取数据后将自动保存",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

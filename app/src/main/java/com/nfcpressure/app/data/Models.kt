package com.nfcpressure.app.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * NFC标签读取的原始数据
 */
data class NfcRawData(
    val uid: ByteArray,
    val block0: ByteArray,  // 4字节：ADC值+状态+电池+校验
    val block1: ByteArray   // 4字节：校准参数
) {
    // 解析Block 0
    val adcHigh8: Int get() = block0[0].toInt() and 0xFF
    val adcLow4: Int get() = (block0[1].toInt() shr 4) and 0x0F
    val status: Int get() = block0[1].toInt() and 0x0F
    val batteryIndicator: Int get() = block0[2].toInt() and 0xFF
    val checksum: Int get() = block0[3].toInt() and 0xFF
    
    // 组合12位ADC值
    val adcValue: Int get() = (adcHigh8 shl 4) or adcLow4
    
    // 校验和验证
    val isChecksumValid: Boolean get() = 
        ((block0[0].toInt() + block0[1].toInt() + block0[2].toInt()) and 0xFF) == checksum
    
    // 数据是否有效
    val isDataValid: Boolean get() = isChecksumValid && (status == 0x01)
    
    // UID转为十六进制字符串
    val uidHex: String get() = uid.joinToString("") { 
        String.format("%02X", it) 
    }
    
    // 解析Block 1 - 校准参数
    val r1Resistance: Int get() = ((block1[0].toInt() and 0xFF) shl 8) or (block1[1].toInt() and 0xFF)
    val vehTenthVolts: Int get() = block1[2].toInt() and 0xFF
    
    // V_EH参考电压（伏特）
    val vehVoltage: Float get() = vehTenthVolts / 10.0f
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as NfcRawData
        return uid.contentEquals(other.uid) && 
               block0.contentEquals(other.block0) && 
               block1.contentEquals(other.block1)
    }
    
    override fun hashCode(): Int {
        var result = uid.contentHashCode()
        result = 31 * result + block0.contentHashCode()
        result = 31 * result + block1.contentHashCode()
        return result
    }
}

/**
 * 解析后的压力数据
 */
data class PressureData(
    val adcRaw: Int,           // 原始ADC值 (0-4095)
    val fsrResistance: Float,  // FSR阻值 (Ω)
    val forceNewton: Float,    // 力 (N)
    val pressureMmHg: Float,   // 压力 (mmHg)
    val isValid: Boolean,      // 数据是否有效
    val batteryIndicator: Int, // 电池电压指示
    val timestamp: Long = System.currentTimeMillis()
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
    
    val formattedDateTime: String
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
    
    // ADC原始值百分比
    val adcPercentage: Float get() = adcRaw / 4095f * 100f
    
    // 格式化FSR阻值
    val formattedResistance: String
        get() = when {
            fsrResistance >= 1_000_000 -> String.format("%.2fMΩ", fsrResistance / 1_000_000)
            fsrResistance >= 1000 -> String.format("%.1fkΩ", fsrResistance / 1000)
            else -> String.format("%.0fΩ", fsrResistance)
        }
}

/**
 * 校准数据
 */
data class CalibrationData(
    val zeroAdc: Int = -1,           // 零点ADC值
    val referenceAdc: Int = -1,      // 参考点ADC值
    val referencePressure: Float = 40f, // 参考点压力值 (mmHg)
    val isCalibrated: Boolean = false
) {
    // 计算校准斜率
    val slope: Float get() = if (zeroAdc >= 0 && referenceAdc > zeroAdc && isCalibrated) {
        referencePressure / (referenceAdc - zeroAdc)
    } else {
        1f
    }
    
    // 应用校准：返回校准后的压力值
    fun calibrate(adcValue: Int): Float {
        if (!isCalibrated || zeroAdc < 0) return -1f
        val calibrated = (adcValue - zeroAdc) * slope
        return maxOf(0f, calibrated)
    }
}

/**
 * 历史记录项
 */
data class HistoryItem(
    val id: Long = System.currentTimeMillis(),
    val pressureMmHg: Float,
    val adcRaw: Int,
    val fsrResistance: Float,
    val isCalibrated: Boolean,
    val timestamp: Long = System.currentTimeMillis()
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
    
    val formattedDate: String
        get() = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
}

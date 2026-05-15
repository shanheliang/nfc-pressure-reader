package com.nfcpressure.app.data

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow

/**
 * FSR406压力传感器查表数据
 * 阻值(Ω) -> 力(N)
 */
private val FSR_TABLE = listOf(
    Pair(1_000_000f, 0.05f),   // 1MΩ -> 0.05N
    Pair(100_000f, 0.1f),      // 100kΩ -> 0.1N
    Pair(50_000f, 0.2f),       // 50kΩ -> 0.2N
    Pair(30_000f, 0.38f),      // 30kΩ -> 0.38N (≈40mmHg)
    Pair(20_000f, 0.5f),       // 20kΩ -> 0.5N
    Pair(10_000f, 1.0f),       // 10kΩ -> 1.0N
    Pair(5_000f, 2.0f),        // 5kΩ -> 2.0N
    Pair(2_000f, 5.0f),        // 2kΩ -> 5.0N
    Pair(1_000f, 10.0f)        // 1kΩ -> 10N
)

/**
 * FSR406传感器面积（直径19.8mm圆形）
 * 面积 ≈ π × (9.9mm)² ≈ 3.08 cm²
 */
private const val FSR_AREA_CM2 = 3.08f

/**
 * 压力转换工具类
 */
object PressureConverter {
    
    /**
     * 从原始ADC数据计算压力值
     * @param rawData NFC读取的原始数据
     * @param calibration 校准数据
     * @return 解析后的压力数据
     */
    fun calculatePressure(rawData: NfcRawData, calibration: CalibrationData): PressureData {
        val adcValue = rawData.adcValue
        
        // 获取参考电压
        val vRef = when (rawData.batteryIndicator) {
            1 -> 2.5f
            2 -> 3.0f
            3 -> 3.3f
            else -> 3.0f // 默认值
        }
        val vEh = rawData.vehVoltage
        
        // 计算分压输出电压
        val vOut = adcValue * (vRef / 4096f)
        
        // 计算FSR电阻
        // R_FSR = R1 × Vout / (V_EH - Vout)
        val r1 = rawData.r1Resistance.toFloat()
        val rFsr = if (vEh > vOut) {
            r1 * vOut / (vEh - vOut)
        } else {
            Float.MAX_VALUE
        }
        
        // FSR阻值转力
        val force = resistanceToForce(rFsr)
        
        // 力转压力
        val pressureMmHg = forceToPressure(force)
        
        // 应用校准
        val calibratedPressure = if (calibration.isCalibrated) {
            calibration.calibrate(adcValue)
        } else {
            pressureMmHg
        }
        
        return PressureData(
            adcRaw = adcValue,
            fsrResistance = rFsr,
            forceNewton = force,
            pressureMmHg = calibratedPressure,
            isValid = rawData.isDataValid,
            batteryIndicator = rawData.batteryIndicator
        )
    }
    
    /**
     * 使用FSR查表+对数插值计算力
     * FSR的阻值与力呈非线性关系，近似符合：R ∝ 1/F
     */
    private fun resistanceToForce(resistance: Float): Float {
        if (resistance <= 0 || resistance > 10_000_000f) {
            return 0f // 开路或短路
        }
        
        // 查表找相邻两点进行线性插值
        // 使用对数插值更准确
        val logR = ln(resistance)
        
        for (i in 0 until FSR_TABLE.size - 1) {
            val (r1, f1) = FSR_TABLE[i]
            val (r2, f2) = FSR_TABLE[i + 1]
            
            val logR1 = ln(r1)
            val logR2 = ln(r2)
            
            // 判断是否在当前区间内
            if (logR >= logR1 && logR <= logR2) {
                // 对数插值
                val t = (logR - logR1) / (logR2 - logR1)
                val logF1 = ln(f1)
                val logF2 = ln(f2)
                val logF = logF1 + t * (logF2 - logF1)
                return kotlin.math.exp(logF).toFloat()
            }
        }
        
        // 超出范围，使用外推
        return when {
            resistance > FSR_TABLE[0].first -> {
                // 阻值大于最大表值，力小于最小力
                val ratio = FSR_TABLE[0].first / resistance
                FSR_TABLE[0].second * ratio
            }
            else -> {
                // 阻值小于最小表值，力大于最大力
                val ratio = resistance / FSR_TABLE.last().first
                FSR_TABLE.last().second / ratio
            }
        }
    }
    
    /**
     * 力转换为压力（mmHg）
     * P(mmHg) = F(N) / A(m²) / 133.322
     * A = 3.08 cm² = 3.08e-4 m²
     */
    private fun forceToPressure(forceNewton: Float): Float {
        if (forceNewton <= 0) return 0f
        
        val areaM2 = FSR_AREA_CM2 * 1e-4f // 转换为平方米
        val pressurePa = forceNewton / areaM2
        val pressureMmHg = pressurePa / 133.322f
        
        return pressureMmHg
    }
    
    /**
     * 计算FSR电阻（供显示用）
     * R_FSR = R1 × Vout / (V_EH - Vout)
     */
    fun calculateFsrResistance(
        adcValue: Int,
        r1Resistance: Int,
        vEh: Float,
        vRef: Float = 3.0f
    ): Float {
        if (adcValue <= 0 || vEh <= 0) return Float.MAX_VALUE
        
        val vOut = adcValue * (vRef / 4096f)
        if (vEh <= vOut) return Float.MAX_VALUE
        
        return r1Resistance * vOut / (vEh - vOut)
    }
}

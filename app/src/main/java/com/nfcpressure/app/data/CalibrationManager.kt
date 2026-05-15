package com.nfcpressure.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// DataStore扩展
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "calibration")

/**
 * 校准数据管理器
 * 使用DataStore持久化校准参数
 */
class CalibrationManager(private val context: Context) {
    
    companion object {
        // 校准参数键
        private val KEY_ZERO_ADC = intPreferencesKey("zero_adc")
        private val KEY_REFERENCE_ADC = intPreferencesKey("reference_adc")
        private val KEY_REFERENCE_PRESSURE = floatPreferencesKey("reference_pressure")
        private val KEY_IS_CALIBRATED = booleanPreferencesKey("is_calibrated")
        
        // 历史记录键
        private val KEY_HISTORY_COUNT = intPreferencesKey("history_count")
        private fun historyPressureKey(index: Int) = floatPreferencesKey("history_pressure_$index")
        private fun historyAdcKey(index: Int) = intPreferencesKey("history_adc_$index")
        private fun historyTimeKey(index: Int) = longPreferencesKey("history_time_$index")
        
        // 最大历史记录数
        const val MAX_HISTORY = 10
    }
    
    /**
     * 获取校准数据流
     */
    val calibrationFlow: Flow<CalibrationData> = context.dataStore.data.map { prefs ->
        CalibrationData(
            zeroAdc = prefs[KEY_ZERO_ADC] ?: -1,
            referenceAdc = prefs[KEY_REFERENCE_ADC] ?: -1,
            referencePressure = prefs[KEY_REFERENCE_PRESSURE] ?: 40f,
            isCalibrated = prefs[KEY_IS_CALIBRATED] ?: false
        )
    }
    
    /**
     * 保存零点校准
     */
    suspend fun saveZeroPoint(adcValue: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ZERO_ADC] = adcValue
        }
    }
    
    /**
     * 保存参考点校准
     */
    suspend fun saveReferencePoint(adcValue: Int, pressureMmHg: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_REFERENCE_ADC] = adcValue
            prefs[KEY_REFERENCE_PRESSURE] = pressureMmHg
            prefs[KEY_IS_CALIBRATED] = true
        }
    }
    
    /**
     * 重置校准
     */
    suspend fun resetCalibration() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_ZERO_ADC)
            prefs.remove(KEY_REFERENCE_ADC)
            prefs.remove(KEY_REFERENCE_PRESSURE)
            prefs[KEY_IS_CALIBRATED] = false
        }
    }
    
    /**
     * 获取校准状态
     */
    val isCalibratedFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_IS_CALIBRATED] ?: false
    }
    
    /**
     * 获取校准步骤状态
     * 0 = 未开始, 1 = 零点已设置, 2 = 参考点已设置
     */
    val calibrationStepFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        when {
            prefs[KEY_IS_CALIBRATED] == true -> 2
            prefs[KEY_ZERO_ADC] != null -> 1
            else -> 0
        }
    }
}

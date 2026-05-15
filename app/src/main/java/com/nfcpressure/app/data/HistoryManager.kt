package com.nfcpressure.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// 历史记录DataStore
private val Context.historyDataStore: DataStore<Preferences> by preferencesDataStore(name = "history")

/**
 * 历史记录管理器
 * 使用DataStore持久化最近10条读取记录
 */
class HistoryManager(private val context: Context) {
    
    companion object {
        private val KEY_HISTORY_0_PRESSURE = floatPreferencesKey("h0_pressure")
        private val KEY_HISTORY_0_ADC = intPreferencesKey("h0_adc")
        private val KEY_HISTORY_0_RESISTANCE = floatPreferencesKey("h0_resistance")
        private val KEY_HISTORY_0_CALIBRATED = booleanPreferencesKey("h0_calibrated")
        private val KEY_HISTORY_0_TIME = longPreferencesKey("h0_time")
        
        private val KEY_HISTORY_1_PRESSURE = floatPreferencesKey("h1_pressure")
        private val KEY_HISTORY_1_ADC = intPreferencesKey("h1_adc")
        private val KEY_HISTORY_1_RESISTANCE = floatPreferencesKey("h1_resistance")
        private val KEY_HISTORY_1_CALIBRATED = booleanPreferencesKey("h1_calibrated")
        private val KEY_HISTORY_1_TIME = longPreferencesKey("h1_time")
        
        private val KEY_HISTORY_2_PRESSURE = floatPreferencesKey("h2_pressure")
        private val KEY_HISTORY_2_ADC = intPreferencesKey("h2_adc")
        private val KEY_HISTORY_2_RESISTANCE = floatPreferencesKey("h2_resistance")
        private val KEY_HISTORY_2_CALIBRATED = booleanPreferencesKey("h2_calibrated")
        private val KEY_HISTORY_2_TIME = longPreferencesKey("h2_time")
        
        private val KEY_HISTORY_3_PRESSURE = floatPreferencesKey("h3_pressure")
        private val KEY_HISTORY_3_ADC = intPreferencesKey("h3_adc")
        private val KEY_HISTORY_3_RESISTANCE = floatPreferencesKey("h3_resistance")
        private val KEY_HISTORY_3_CALIBRATED = booleanPreferencesKey("h3_calibrated")
        private val KEY_HISTORY_3_TIME = longPreferencesKey("h3_time")
        
        private val KEY_HISTORY_4_PRESSURE = floatPreferencesKey("h4_pressure")
        private val KEY_HISTORY_4_ADC = intPreferencesKey("h4_adc")
        private val KEY_HISTORY_4_RESISTANCE = floatPreferencesKey("h4_resistance")
        private val KEY_HISTORY_4_CALIBRATED = booleanPreferencesKey("h4_calibrated")
        private val KEY_HISTORY_4_TIME = longPreferencesKey("h4_time")
        
        private val KEY_HISTORY_5_PRESSURE = floatPreferencesKey("h5_pressure")
        private val KEY_HISTORY_5_ADC = intPreferencesKey("h5_adc")
        private val KEY_HISTORY_5_RESISTANCE = floatPreferencesKey("h5_resistance")
        private val KEY_HISTORY_5_CALIBRATED = booleanPreferencesKey("h5_calibrated")
        private val KEY_HISTORY_5_TIME = longPreferencesKey("h5_time")
        
        private val KEY_HISTORY_6_PRESSURE = floatPreferencesKey("h6_pressure")
        private val KEY_HISTORY_6_ADC = intPreferencesKey("h6_adc")
        private val KEY_HISTORY_6_RESISTANCE = floatPreferencesKey("h6_resistance")
        private val KEY_HISTORY_6_CALIBRATED = booleanPreferencesKey("h6_calibrated")
        private val KEY_HISTORY_6_TIME = longPreferencesKey("h6_time")
        
        private val KEY_HISTORY_7_PRESSURE = floatPreferencesKey("h7_pressure")
        private val KEY_HISTORY_7_ADC = intPreferencesKey("h7_adc")
        private val KEY_HISTORY_7_RESISTANCE = floatPreferencesKey("h7_resistance")
        private val KEY_HISTORY_7_CALIBRATED = booleanPreferencesKey("h7_calibrated")
        private val KEY_HISTORY_7_TIME = longPreferencesKey("h7_time")
        
        private val KEY_HISTORY_8_PRESSURE = floatPreferencesKey("h8_pressure")
        private val KEY_HISTORY_8_ADC = intPreferencesKey("h8_adc")
        private val KEY_HISTORY_8_RESISTANCE = floatPreferencesKey("h8_resistance")
        private val KEY_HISTORY_8_CALIBRATED = booleanPreferencesKey("h8_calibrated")
        private val KEY_HISTORY_8_TIME = longPreferencesKey("h8_time")
        
        private val KEY_HISTORY_9_PRESSURE = floatPreferencesKey("h9_pressure")
        private val KEY_HISTORY_9_ADC = intPreferencesKey("h9_adc")
        private val KEY_HISTORY_9_RESISTANCE = floatPreferencesKey("h9_resistance")
        private val KEY_HISTORY_9_CALIBRATED = booleanPreferencesKey("h9_calibrated")
        private val KEY_HISTORY_9_TIME = longPreferencesKey("h9_time")
        
        const val MAX_HISTORY = 10
    }
    
    // 历史记录键集合
    private val historyKeys = listOf(
        HistoryKeys(
            pressure = KEY_HISTORY_0_PRESSURE,
            adc = KEY_HISTORY_0_ADC,
            resistance = KEY_HISTORY_0_RESISTANCE,
            calibrated = KEY_HISTORY_0_CALIBRATED,
            time = KEY_HISTORY_0_TIME
        ),
        HistoryKeys(
            pressure = KEY_HISTORY_1_PRESSURE,
            adc = KEY_HISTORY_1_ADC,
            resistance = KEY_HISTORY_1_RESISTANCE,
            calibrated = KEY_HISTORY_1_CALIBRATED,
            time = KEY_HISTORY_1_TIME
        ),
        HistoryKeys(
            pressure = KEY_HISTORY_2_PRESSURE,
            adc = KEY_HISTORY_2_ADC,
            resistance = KEY_HISTORY_2_RESISTANCE,
            calibrated = KEY_HISTORY_2_CALIBRATED,
            time = KEY_HISTORY_2_TIME
        ),
        HistoryKeys(
            pressure = KEY_HISTORY_3_PRESSURE,
            adc = KEY_HISTORY_3_ADC,
            resistance = KEY_HISTORY_3_RESISTANCE,
            calibrated = KEY_HISTORY_3_CALIBRATED,
            time = KEY_HISTORY_3_TIME
        ),
        HistoryKeys(
            pressure = KEY_HISTORY_4_PRESSURE,
            adc = KEY_HISTORY_4_ADC,
            resistance = KEY_HISTORY_4_RESISTANCE,
            calibrated = KEY_HISTORY_4_CALIBRATED,
            time = KEY_HISTORY_4_TIME
        ),
        HistoryKeys(
            pressure = KEY_HISTORY_5_PRESSURE,
            adc = KEY_HISTORY_5_ADC,
            resistance = KEY_HISTORY_5_RESISTANCE,
            calibrated = KEY_HISTORY_5_CALIBRATED,
            time = KEY_HISTORY_5_TIME
        ),
        HistoryKeys(
            pressure = KEY_HISTORY_6_PRESSURE,
            adc = KEY_HISTORY_6_ADC,
            resistance = KEY_HISTORY_6_RESISTANCE,
            calibrated = KEY_HISTORY_6_CALIBRATED,
            time = KEY_HISTORY_6_TIME
        ),
        HistoryKeys(
            pressure = KEY_HISTORY_7_PRESSURE,
            adc = KEY_HISTORY_7_ADC,
            resistance = KEY_HISTORY_7_RESISTANCE,
            calibrated = KEY_HISTORY_7_CALIBRATED,
            time = KEY_HISTORY_7_TIME
        ),
        HistoryKeys(
            pressure = KEY_HISTORY_8_PRESSURE,
            adc = KEY_HISTORY_8_ADC,
            resistance = KEY_HISTORY_8_RESISTANCE,
            calibrated = KEY_HISTORY_8_CALIBRATED,
            time = KEY_HISTORY_8_TIME
        ),
        HistoryKeys(
            pressure = KEY_HISTORY_9_PRESSURE,
            adc = KEY_HISTORY_9_ADC,
            resistance = KEY_HISTORY_9_RESISTANCE,
            calibrated = KEY_HISTORY_9_CALIBRATED,
            time = KEY_HISTORY_9_TIME
        )
    )
    
    /**
     * 获取历史记录流
     */
    val historyFlow: Flow<List<HistoryItem>> = context.historyDataStore.data.map { prefs ->
        historyKeys.mapNotNull { keys ->
            val time = prefs[keys.time] ?: return@mapNotNull null
            HistoryItem(
                pressureMmHg = prefs[keys.pressure] ?: 0f,
                adcRaw = prefs[keys.adc] ?: 0,
                fsrResistance = prefs[keys.resistance] ?: 0f,
                isCalibrated = prefs[keys.calibrated] ?: false,
                timestamp = time
            )
        }.sortedByDescending { it.timestamp }
    }
    
    /**
     * 添加历史记录
     */
    suspend fun addHistory(item: HistoryItem) {
        context.historyDataStore.edit { prefs ->
            // 先将所有现有记录向后移动一位
            for (i in MAX_HISTORY - 2 downTo 0) {
                val currentKeys = historyKeys[i]
                val nextKeys = historyKeys[i + 1]
                
                val currentTime = prefs[currentKeys.time]
                if (currentTime != null) {
                    prefs[nextKeys.pressure] = prefs[currentKeys.pressure] ?: 0f
                    prefs[nextKeys.adc] = prefs[currentKeys.adc] ?: 0
                    prefs[nextKeys.resistance] = prefs[currentKeys.resistance] ?: 0f
                    prefs[nextKeys.calibrated] = prefs[currentKeys.calibrated] ?: false
                    prefs[nextKeys.time] = currentTime
                }
            }
            
            // 添加新记录到第一个位置
            val firstKeys = historyKeys[0]
            prefs[firstKeys.pressure] = item.pressureMmHg
            prefs[firstKeys.adc] = item.adcRaw
            prefs[firstKeys.resistance] = item.fsrResistance
            prefs[firstKeys.calibrated] = item.isCalibrated
            prefs[firstKeys.time] = item.timestamp
        }
    }
    
    /**
     * 清除所有历史记录
     */
    suspend fun clearHistory() {
        context.historyDataStore.edit { prefs ->
            historyKeys.forEach { keys ->
                prefs.remove(keys.pressure)
                prefs.remove(keys.adc)
                prefs.remove(keys.resistance)
                prefs.remove(keys.calibrated)
                prefs.remove(keys.time)
            }
        }
    }
    
    /**
     * 历史记录键集合
     */
    private data class HistoryKeys(
        val pressure: Preferences.Key<Float>,
        val adc: Preferences.Key<Int>,
        val resistance: Preferences.Key<Float>,
        val calibrated: Preferences.Key<Boolean>,
        val time: Preferences.Key<Long>
    )
}

package com.nfcpressure.app.viewmodel

import android.app.Application
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nfcpressure.app.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * UI状态
 */
data class UiState(
    val pressureData: PressureData? = null,
    val rawData: NfcRawData? = null,
    val calibration: CalibrationData = CalibrationData(),
    val history: List<HistoryItem> = emptyList(),
    val isReading: Boolean = false,
    val isNfcEnabled: Boolean = false,
    val isNfcSupported: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showCalibrationDialog: Boolean = false,
    val showReferenceDialog: Boolean = false
)

/**
 * 主ViewModel
 */
class PressureViewModel(application: Application) : AndroidViewModel(application) {
    
    private val nfcReader = NfcVReader()
    private val calibrationManager = CalibrationManager(application)
    private val historyManager = HistoryManager(application)
    
    // UI状态
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    init {
        // 检查NFC支持状态
        val nfcAdapter = NfcAdapter.getDefaultAdapter(application)
        _uiState.update { 
            it.copy(
                isNfcSupported = nfcAdapter != null,
                isNfcEnabled = nfcAdapter?.isEnabled == true
            )
        }
        
        // 监听校准数据变化
        viewModelScope.launch {
            calibrationManager.calibrationFlow.collect { calibration ->
                _uiState.update { it.copy(calibration = calibration) }
            }
        }
        
        // 监听历史记录变化
        viewModelScope.launch {
            historyManager.historyFlow.collect { history ->
                _uiState.update { it.copy(history = history) }
            }
        }
    }
    
    /**
     * 处理NFC标签
     */
    fun handleNfcTag(tag: Tag) {
        viewModelScope.launch {
            _uiState.update { it.copy(isReading = true, errorMessage = null) }
            
            try {
                val rawData = nfcReader.readTag(tag)
                
                if (rawData == null) {
                    _uiState.update { 
                        it.copy(
                            isReading = false,
                            errorMessage = "读取失败，请重试"
                        )
                    }
                    return@launch
                }
                
                // 校验数据
                if (!rawData.isChecksumValid) {
                    _uiState.update { 
                        it.copy(
                            isReading = false,
                            errorMessage = "数据校验失败"
                        )
                    }
                    return@launch
                }
                
                // 计算压力值
                val currentCalibration = _uiState.value.calibration
                val pressureData = PressureConverter.calculatePressure(rawData, currentCalibration)
                
                _uiState.update { 
                    it.copy(
                        pressureData = pressureData,
                        rawData = rawData,
                        isReading = false,
                        successMessage = "读取成功"
                    )
                }
                
                // 保存到历史记录
                val historyItem = HistoryItem(
                    pressureMmHg = pressureData.pressureMmHg,
                    adcRaw = pressureData.adcRaw,
                    fsrResistance = pressureData.fsrResistance,
                    isCalibrated = currentCalibration.isCalibrated,
                    timestamp = System.currentTimeMillis()
                )
                historyManager.addHistory(historyItem)
                
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isReading = false,
                        errorMessage = e.message ?: "未知错误"
                    )
                }
            }
        }
    }
    
    /**
     * 设置零点校准
     */
    fun setZeroPoint() {
        val currentData = _uiState.value.rawData
        if (currentData == null) {
            _uiState.update { it.copy(errorMessage = "请先读取NFC标签") }
            return
        }
        
        viewModelScope.launch {
            calibrationManager.saveZeroPoint(currentData.adcValue)
            _uiState.update { it.copy(successMessage = "零点已设置") }
        }
    }
    
    /**
     * 设置参考点校准
     */
    fun setReferencePoint(pressureMmHg: Float) {
        val currentData = _uiState.value.rawData
        if (currentData == null) {
            _uiState.update { it.copy(errorMessage = "请先读取NFC标签") }
            return
        }
        
        viewModelScope.launch {
            calibrationManager.saveReferencePoint(currentData.adcValue, pressureMmHg)
            _uiState.update { it.copy(successMessage = "参考点已设置，校准完成") }
        }
    }
    
    /**
     * 重置校准
     */
    fun resetCalibration() {
        viewModelScope.launch {
            calibrationManager.resetCalibration()
            _uiState.update { it.copy(successMessage = "校准已重置") }
        }
    }
    
    /**
     * 清除历史记录
     */
    fun clearHistory() {
        viewModelScope.launch {
            historyManager.clearHistory()
            _uiState.update { it.copy(successMessage = "历史记录已清除") }
        }
    }
    
    /**
     * 更新NFC状态
     */
    fun updateNfcState(enabled: Boolean) {
        _uiState.update { it.copy(isNfcEnabled = enabled) }
    }
    
    /**
     * 显示校准对话框
     */
    fun showCalibrationDialog() {
        _uiState.update { it.copy(showCalibrationDialog = true) }
    }
    
    /**
     * 隐藏校准对话框
     */
    fun dismissCalibrationDialog() {
        _uiState.update { it.copy(showCalibrationDialog = false) }
    }
    
    /**
     * 显示参考点对话框
     */
    fun showReferenceDialog() {
        _uiState.update { it.copy(showReferenceDialog = true) }
    }
    
    /**
     * 隐藏参考点对话框
     */
    fun dismissReferenceDialog() {
        _uiState.update { it.copy(showReferenceDialog = false) }
    }
    
    /**
     * 清除错误消息
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    /**
     * 清除成功消息
     */
    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }
    
    /**
     * 检查是否需要校准提示
     */
    fun needsCalibrationHint(): Boolean {
        val step = _uiState.value.calibration.let {
            when {
                it.isCalibrated -> 2
                it.zeroAdc >= 0 -> 1
                else -> 0
            }
        }
        return step < 2
    }
}

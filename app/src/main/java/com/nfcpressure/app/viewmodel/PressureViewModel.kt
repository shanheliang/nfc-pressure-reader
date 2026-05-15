package com.nfcpressure.app.viewmodel

import android.app.Application
import android.nfc.NfcAdapter
import android.nfc.Tag
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nfcpressure.app.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class UiState(
    val pressureData: PressureData? = null,
    val rawData: NfcRawData? = null,
    val calibration: CalibrationData = CalibrationData(),
    val history: List<HistoryItem> = emptyList(),
    val isReading: Boolean = false,
    val isNfcEnabled: Boolean = false,
    val isNfcSupported: Boolean = false,
    val isReaderModeActive: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class PressureViewModel(application: Application) : AndroidViewModel(application) {
    
    private val nfcReader = NfcVReader()
    private val calibrationManager = CalibrationManager(application)
    private val historyManager = HistoryManager(application)
    
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    init {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(application)
        _uiState.update { 
            it.copy(
                isNfcSupported = nfcAdapter != null,
                isNfcEnabled = nfcAdapter?.isEnabled == true
            )
        }
        
        viewModelScope.launch {
            calibrationManager.calibrationFlow.collect { calibration ->
                _uiState.update { it.copy(calibration = calibration) }
            }
        }
        
        viewModelScope.launch {
            historyManager.historyFlow.collect { history ->
                _uiState.update { it.copy(history = history) }
            }
        }
    }
    
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
                
                if (!rawData.isChecksumValid) {
                    _uiState.update { 
                        it.copy(
                            isReading = false,
                            errorMessage = "数据校验失败"
                        )
                    }
                    return@launch
                }
                
                val currentCalibration = _uiState.value.calibration
                val pressureData = PressureConverter.calculatePressure(rawData, currentCalibration)
                
                _uiState.update { 
                    it.copy(
                        pressureData = pressureData,
                        rawData = rawData,
                        isReading = false,
                        isReaderModeActive = false,
                        successMessage = "读取成功"
                    )
                }
                
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
                        isReaderModeActive = false,
                        errorMessage = e.message ?: "未知错误"
                    )
                }
            }
        }
    }
    
    /**
     * 开始读取（按钮触发）
     */
    fun startReading() {
        _uiState.update { it.copy(isReading = true, isReaderModeActive = true, errorMessage = null) }
    }
    
    /**
     * 停止读取
     */
    fun stopReading() {
        _uiState.update { it.copy(isReading = false, isReaderModeActive = false) }
    }
    
    /**
     * 设置ReaderMode活跃状态（由Activity调用）
     */
    fun setReaderModeActive(active: Boolean) {
        _uiState.update { it.copy(isReaderModeActive = active) }
    }
    
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
    
    fun resetCalibration() {
        viewModelScope.launch {
            calibrationManager.resetCalibration()
            _uiState.update { it.copy(successMessage = "校准已重置") }
        }
    }
    
    fun clearHistory() {
        viewModelScope.launch {
            historyManager.clearHistory()
            _uiState.update { it.copy(successMessage = "历史记录已清除") }
        }
    }
    
    fun updateNfcState(enabled: Boolean) {
        _uiState.update { it.copy(isNfcEnabled = enabled) }
    }
    
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }
}

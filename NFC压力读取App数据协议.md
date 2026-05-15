# NFC压力读取App数据协议文档

**文档版本**: 1.0  
**编写日期**: 2026年5月  
**适用硬件**: ST25DV04KC NFC芯片 + FSR406压力传感器  
**App版本**: 1.0.0  

---

## 目录

1. [概述](#1-概述)
2. [NFC通信协议](#2-nfc通信协议)
3. [ST25DV EEPROM内存映射](#3-st25dv-eeprom内存映射)
4. [数据帧格式](#4-数据帧格式)
5. [压力值计算公式](#5-压力值计算公式)
6. [App读取流程](#6-app读取流程)
7. [错误处理机制](#7-错误处理机制)
8. [UI显示规格](#8-ui显示规格)

---

## 1. 概述

本App基于Android NFC API，读取符合ISO 15693协议的NFC无源压力传感卡片。卡片使用ST25DV04KC芯片存储数据，通过FSR406压力传感器采集压力信息。

### 1.1 技术规格

| 项目 | 规格 |
|------|------|
| NFC协议 | ISO 15693 (NFC-V) |
| NFC芯片 | ST25DV04KC |
| 压力传感器 | FSR406 |
| ADC分辨率 | 12位 (0-4095) |
| 最小SDK | API 24 (Android 7.0) |
| 目标SDK | API 34 |

### 1.2 App架构

```
MainActivity.kt
    └── PressureViewModel.kt
            ├── NfcVReader.kt          # NFC通信层
            ├── PressureConverter.kt   # 数据转换层
            ├── CalibrationManager.kt  # 校准管理
            └── HistoryManager.kt      # 历史记录
```

---

## 2. NFC通信协议

### 2.1 协议类型

- **协议名称**: ISO 15693 (NFC-V) - Extended Commands
- **Android类**: `android.nfc.tech.NfcV`
- **技术声明**: `android.hardware.nfc.tech.IsoDep`
- **通信模式**: 非寻址模式 (Flags=0x02，不带UID)

### 2.2 ISO 15693 Extended 命令格式

#### Extended Read Single Block (读取单块) ⭐

**命令码**: `0x30`

**请求帧格式** (共4字节):

| 字节位置 | 长度 | 内容 | 说明 |
|----------|------|------|------|
| 0 | 1 | Flags | `0x02` (非addressed模式，不带UID) |
| 1 | 1 | Command | `0x30` |
| 2 | 1 | Block Number LSB | 块号低字节 |
| 3 | 1 | Block Number MSB | 块号高字节 |

**响应帧格式**:

| 字节位置 | 长度 | 内容 | 说明 |
|----------|------|------|------|
| 0 | 1 | Error Code | `0x00`=成功，其他=错误 |
| 1-4 | 4 | Block Data | 块数据 (4字节) |

**示例命令** (读取Block 0x0000):
```
Request:  [02] [30] [00] [00]      // Flags + CMD + LSB + MSB
Response: [00] [D0] [07] [5A] [01]  // 成功，ErrorCode(0x00) + 4字节数据
Response: [XX] [...]                // 失败，XX为错误码
```

#### Extended Read Multiple Blocks (读取多块)

**命令码**: `0x33`

**请求帧格式**:

| 字节位置 | 长度 | 内容 | 说明 |
|----------|------|------|------|
| 0 | 1 | Flags | `0x02` (非addressed模式) |
| 1 | 1 | Command | `0x33` |
| 2 | 1 | First Block LSB | 起始块号低字节 |
| 3 | 1 | First Block MSB | 起始块号高字节 |
| 4 | 1 | Number of Blocks | 块数量 |

#### Extended Write Single Block (写入单块) ⭐新增

**命令码**: `0x31`

**请求帧格式** (共8字节):

| 字节位置 | 长度 | 内容 | 说明 |
|----------|------|------|------|
| 0 | 1 | Flags | `0x02` |
| 1 | 1 | Command | `0x31` |
| 2 | 1 | Block Number LSB | 块号低字节 |
| 3 | 1 | Block Number MSB | 块号高字节 |
| 4 | 1 | Data Byte 0 | 数据字节0 |
| 5 | 1 | Data Byte 1 | 数据字节1 |
| 6 | 1 | Data Byte 2 | 数据字节2 |
| 7 | 1 | Data Byte 3 | 数据字节3 |

**响应帧格式**:

| 字节位置 | 长度 | 内容 | 说明 |
|----------|------|------|------|
| 0 | 1 | Error Code | `0x00`=成功，其他=错误 |

**示例命令** (写入Block 0x0001):
```
Request:  [02] [31] [01] [00] [00] [B8] [1C] [00]  // 写入0x00,0xB8,0x1C,0x00
Response: [00]                                    // 成功
```

#### 能量采集控制命令 ⭐新增

**Present Password (密码认证)** - 命令码: `0xB3`

| 字节位置 | 内容 | 说明 |
|----------|------|------|
| 0 | `0x02` | Flags |
| 1 | `0xB3` | Command |
| 2 | `0x02` | Length |
| 3-10 | `00 00 00 00 00 00 00 00` | 8字节密码(默认全0) |

**Enable Energy Harvesting** - 命令码: `0xAE`

| 字节位置 | 内容 | 说明 |
|----------|------|------|
| 0 | `0x02` | Flags |
| 1 | `0xAE` | Command |
| 2 | `0x02` | Length |
| 3 | `0x02` | System Command |
| 4 | `0x01` | Enable (0x00=禁用) |

**示例** (启用能量采集):
```
1. Present Password:  [02] [B3] [02] [00 00 00 00 00 00 00 00]
2. Enable EH:          [02] [AE] [02] [02] [01]
```

#### 重试机制 ⭐新增

**safeTransceive机制**:

| 参数 | 值 | 说明 |
|------|-----|------|
| 最大重试次数 | 5 | COMMAND_RETRY |
| 重试间隔 | 5ms | COMMAND_DELAY_MS |
| 成功判断 | `response[0] == 0x00` | 与ST官方一致 |

5次都失败后抛出 `IOException("NFC transceive failed after 5 retries")`

### 2.3 Flags字节说明

| Bit | 名称 | 说明 |
|-----|------|------|
| 0 | Error Flag | 响应中: 0=成功, 1=有错误 |
| 1 | Subcarrier | 0=单副载波 |
| 2 | Data Rate | 0=低速率 |
| 3 | Inventory | 0=非Inventory命令 |
| 4 | Protocol Extension | 0=无扩展 |
| 5 | Select | 0=不选择 |
| 6 | Addressed | **0=非寻址模式(不带UID)** |
| 7 | High Data Rate | 0=低速率 |

---

## 3. ST25DV EEPROM内存映射

ST25DV04KC EEPROM共4Kb，分成256个块(Block)，每块4字节。

### 3.1 内存布局

| 块地址 | 用途 | 容量 | 说明 |
|--------|------|------|------|
| 0x00 | 数据块 | 4字节 | ADC值、状态、电池、校验和 |
| 0x01 | 校准参数块 | 4字节 | R1阻值、V_EH电压 |

### 3.2 Block 0 详细定义 (0x0000)

```
┌─────────────────────────────────────────────────────────────┐
│  Byte 0        │  Byte 1              │  Byte 2  │  Byte 3  │
├─────────────────┼─────────────────────┼──────────┼─────────┤
│  ADC[11:4]      │  ADC[3:0] │ STATUS   │  BAT     │  CS     │
│  (高8位)        │  (低4位)  │ (4位)     │  (8位)   │  (8位)  │
└─────────────────┴─────────────────────┴──────────┴─────────┘
```

| 字节 | 位字段 | 含义 | 范围 |
|------|--------|------|------|
| 0 | ADC[11:4] | ADC值高8位 | 0-255 |
| 1[7:4] | ADC[3:0] | ADC值低4位 | 0-15 |
| 1[3:0] | STATUS | 状态标志 | 0x01=有效 |
| 2 | BAT | 电池电压指示 | 1=2.5V, 2=3.0V, 3=3.3V |
| 3 | CS | 校验和 | block0[0]+block0[1]+block0[2] & 0xFF |

### 3.3 Block 1 详细定义 (0x0001)

```
┌─────────────────────────────────────────────────────────────┐
│  Byte 0          │  Byte 1          │  Byte 2  │  Byte 3    │
├──────────────────┼──────────────────┼──────────┼───────────┤
│  R1[15:8]        │  R1[7:0]         │  V_EH    │  Reserved  │
│  (高8位)         │  (低8位)          │  (0.1V)  │  (预留)    │
└──────────────────┴──────────────────┴──────────┴───────────┘
```

| 字节 | 含义 | 单位 | 默认值 |
|------|------|------|--------|
| 0-1 | R1分压电阻阻值 | Ω (大端序) | 47000Ω |
| 2 | V_EH标称电压 | 0.1V | 30 (=3.0V) |
| 3 | 预留 | - | 0x00 |

---

## 4. 数据帧格式

### 4.1 NFC原始数据结构 (NfcRawData)

```kotlin
data class NfcRawData(
    val uid: ByteArray,      // 8字节UID
    val block0: ByteArray,   // 4字节数据块
    val block1: ByteArray    // 4字节校准块
)
```

### 4.2 数据解析规则

#### ADC值 (12位)

```
ADC = (block0[0] << 4) | (block0[1] >> 4)
     ↑
     高8位                    低4位
```

**取值范围**: 0 - 4095

#### 状态标志

```
STATUS = block0[1] & 0x0F
```

| 值 | 含义 |
|----|------|
| 0x01 | 数据有效 |
| 其他 | 数据无效 |

#### 校验和

```
checksum = (block0[0] + block0[1] + block0[2]) & 0xFF
```

**校验条件**: `block0[3] == checksum`

### 4.3 电池电压指示

```
BAT = block0[2] & 0xFF

根据BAT值确定参考电压Vref:
  BAT = 1 → Vref = 2.5V
  BAT = 2 → Vref = 3.0V
  BAT = 3 → Vref = 3.3V
  其他  → Vref = 3.0V (默认)
```

### 4.4 校准参数解析

```
R1 = (block1[0] << 8) | block1[1]   // 大端序16位整数
V_EH = block1[2] / 10.0            // 单位: 伏特
```

---

## 5. 压力值计算公式

### 5.1 计算流程

```
原始字节 → ADC值 → Vout电压 → FSR阻值 → 力值 → 压力值
```

### 5.2 公式详解

#### Step 1: 计算分压输出电压

```
Vout = ADC × (Vref / 4096)
```

其中:
- ADC: 12位ADC原始值 (0-4095)
- Vref: 参考电压 (根据电池指示器确定)
- Vout: 分压电路输出电压

#### Step 2: 计算FSR电阻

```
R_FSR = R1 × Vout / (V_EH - Vout)
```

其中:
- R1: 分压电阻阻值 (从Block 1读取)
- V_EH: 电源电压 (从Block 1读取)
- R_FSR: FSR传感器阻值

**边界条件**: 若 `V_EH ≤ Vout`，则 `R_FSR = +∞`

#### Step 3: FSR阻值转力值 (查表+对数插值)

FSR406阻值与力的关系是非线性的，使用对数插值查表法:

```kotlin
// FSR406 阻值-力对照表
FSR_TABLE = [
    (1_000_000Ω, 0.05N),   // 1MΩ
    (100_000Ω, 0.1N),      // 100kΩ
    (50_000Ω, 0.2N),       // 50kΩ
    (30_000Ω, 0.38N),      // 30kΩ
    (20_000Ω, 0.5N),       // 20kΩ
    (10_000Ω, 1.0N),       // 10kΩ
    (5_000Ω, 2.0N),        // 5kΩ
    (2_000Ω, 5.0N),        // 2kΩ
    (1_000Ω, 10.0N)        // 1kΩ
]

// 对数插值算法
logF = logF1 + t × (logF2 - logF1)
其中: t = (logR - logR1) / (logR2 - logR1)
```

#### Step 4: 力转压力

```
P(mmHg) = F(N) / A(m²) / 133.322
```

其中:
- F: 力值 (牛顿)
- A: FSR406有效面积 = 3.08 cm² = 3.08×10⁻⁴ m²
- 133.322: 1 mmHg ≈ 133.322 Pa

### 5.3 校准公式

App支持两点校准，公式如下:

```
斜率 = 参考压力 / (参考ADC - 零点ADC)

校准后压力 = max(0, (ADC - 零点ADC) × 斜率)
```

**校准步骤**:
1. 零点校准: 在无压力状态下记录 `零点ADC`
2. 参考点校准: 在已知压力下记录 `参考ADC` 和 `参考压力`

### 5.4 完整计算代码

```kotlin
fun calculatePressure(rawData: NfcRawData, calibration: CalibrationData): PressureData {
    // 1. 获取参考电压
    val vRef = when (rawData.batteryIndicator) {
        1 -> 2.5f
        2 -> 3.0f
        3 -> 3.3f
        else -> 3.0f
    }
    
    // 2. 计算输出电压
    val vOut = rawData.adcValue * (vRef / 4096f)
    
    // 3. 计算FSR阻值
    val rFsr = r1 * vOut / (vehVoltage - vOut)
    
    // 4. 查表计算力值
    val force = resistanceToForce(rFsr)
    
    // 5. 计算压力
    val pressure = force / (3.08e-4f) / 133.322f
    
    // 6. 应用校准
    val calibratedPressure = if (calibration.isCalibrated) {
        calibration.calibrate(adcValue)
    } else {
        pressure
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
```

---

## 6. App读取流程

### 6.1 整体流程图

```
┌──────────────────────────────────────────────────────────────┐
│                     用户将手机贴近NFC标签                      │
└──────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────┐
│  MainActivity.handleIntent()                                │
│  - 检测 ACTION_TAG_DISCOVERED                               │
│  - 检查 techList 是否包含 "NfcV"                           │
└──────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────┐
│  PressureViewModel.handleNfcTag()                           │
│  - 启动协程处理                                              │
│  - 设置 isReading = true                                    │
└──────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────┐
│  NfcVReader.readTag()                                       │
│  1. NfcV.get(tag) 获取NFC-V实例                            │
│  2. nfcV.connect() 建立连接                                 │
│  3. 读取UID (tag.id)                                        │
│  4. 读取Block 0 → readSingleBlock(0x00)                     │
│  5. 读取Block 1 → readSingleBlock(0x01)                     │
│  6. 构造 NfcRawData 对象                                    │
│  7. 关闭连接                                                │
└──────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────┐
│  数据校验 (NfcRawData)                                      │
│  - 校验和验证: (byte0 + byte1 + byte2) == byte3             │
│  - 状态验证: status == 0x01                                 │
└──────────────────────────────────────────────────────────────┘
                              │
                    ┌─────────┴─────────┐
                    │                   │
                  通过                  失败
                    │                   │
                    ▼                   ▼
┌─────────────────────────┐  ┌─────────────────────────┐
│ PressureConverter.     │  │ 显示错误: "数据校验失败"  │
│ calculatePressure()    │  └─────────────────────────┘
│ - 计算ADC值            │
│ - 计算FSR阻值          │
│ - 查表得到力值         │
│ - 力转压力             │
│ - 应用校准             │
└─────────────────────────┘
                    │
                    ▼
┌──────────────────────────────────────────────────────────────┐
│  保存历史记录 (HistoryManager)                               │
│  - 创建 HistoryItem                                         │
│  - 保存到 DataStore                                         │
└──────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────┐
│  更新UI状态                                                  │
│  - pressureData: PressureData                                │
│  - rawData: NfcRawData                                      │
│  - isReading: false                                        │
│  - 显示成功提示                                              │
└──────────────────────────────────────────────────────────────┘
```

### 6.2 NFC读取伪代码

```kotlin
suspend fun readTag(tag: Tag): NfcRawData? {
    var nfcV: NfcV? = null
    
    try {
        // 1. 获取连接
        nfcV = NfcV.get(tag)
        nfcV.connect()
        
        // 2. 获取UID
        val uid = tag.id
        
        // 3. 读取数据块
        val block0 = readSingleBlock(nfcV, uid, 0x00)
            ?: return null
        val block1 = readSingleBlock(nfcV, uid, 0x01)
            ?: return null
        
        // 4. 返回原始数据
        return NfcRawData(uid, block0, block1)
        
    } finally {
        nfcV?.close()
    }
}

private fun readSingleBlock(
    nfcV: NfcV,
    uid: ByteArray,
    blockNumber: Byte
): ByteArray? {
    // 构建Read Single Block命令
    val command = byteArrayOf(
        0x02,                    // Flags
        0x20,                    // CMD_READ_SINGLE_BLOCK
        *uid,                    // 8字节UID
        blockNumber              // 块号
    )
    
    val response = nfcV.transceive(command)
    
    // 检查响应标志
    if (response[0].toInt() and 0x01 == 0) {
        // 成功: 提取4字节数据(跳过flags)
        return response.copyOfRange(1, 5)
    }
    return null
}
```

---

## 7. 错误处理机制

### 7.1 错误类型

| 错误类型 | 错误码 | 处理方式 | 用户提示 |
|----------|--------|----------|----------|
| 标签丢失 | TAG_LOST | 重新读取 | "读取失败，请重试" |
| 校验和错误 | CHECKSUM_ERROR | 丢弃数据 | "数据校验失败" |
| 连接失败 | CONNECTION_ERROR | 关闭后重试 | "NFC连接失败" |
| 标签类型不支持 | UNSUPPORTED_TAG | 检查标签类型 | "不支持的NFC标签类型" |

### 7.2 NfcReadResult定义

```kotlin
sealed class NfcReadResult {
    data class Success(val data: NfcRawData) : NfcReadResult()
    data class Error(val message: String, val code: Int = -1) : NfcReadResult()
    object TagLost : NfcReadResult()
    object ChecksumError : NfcReadResult()
}
```

### 7.3 异常捕获层级

```
try {
    // NFC通信
    nfcV.connect()
    nfcV.transceive(command)
} catch (e: IOException) {
    // 连接断开、传输错误
    return NfcReadResult.TagLost
} catch (e: TagLostException) {
    // 标签移走
    return NfcReadResult.TagLost
} finally {
    try { nfcV?.close() } catch (e: Exception) { }
}
```

### 7.4 数据有效性判断

```kotlin
val NfcRawData.isChecksumValid: Boolean
    get() = ((block0[0] + block0[1] + block0[2]) and 0xFF) == checksum

val NfcRawData.isDataValid: Boolean
    get() = isChecksumValid && (status == 0x01)
```

---

## 8. UI显示规格

### 8.1 主界面布局

```
┌─────────────────────────────────────────┐
│  NFC压力监测                    [NFC状态]│
├─────────────────────────────────────────┤
│  [监测]    [校准]    [历史]              │
├─────────────────────────────────────────┤
│                                         │
│              ┌─────────┐                │
│              │  圆形   │                │
│              │  仪表盘  │                │
│              │  78.5   │                │
│              │  mmHg   │                │
│              └─────────┘                │
│                                         │
│  ┌─────────────────────────────────────┐│
│  │ ● 数据有效     UID: E004015267...  ││
│  ├─────────────────────────────────────┤│
│  │ ADC原始值:    │  FSR阻值:           ││
│  │ 2048         │  50.2kΩ             ││
│  ├─────────────────────────────────────┤│
│  │ 力值:         │  参考电压:          ││
│  │ 0.2N         │  3.0V               ││
│  └─────────────────────────────────────┘│
│                                         │
└─────────────────────────────────────────┘
```

### 8.2 仪表盘颜色分区

| 范围 | 颜色 | 含义 |
|------|------|------|
| 0-30 mmHg | 绿色 (#4CAF50) | 正常压力 |
| 30-50 mmHg | 黄色 (#FFC107) | 关注 |
| 50+ mmHg | 红色 (#F44336) | 高压警告 |

### 8.3 显示数据项

| 字段 | 单位 | 精度 | 来源 |
|------|------|------|------|
| 压力值 | mmHg | 1位小数 | 计算结果 |
| ADC原始值 | - | 整数 | 原始数据 |
| FSR阻值 | Ω/kΩ/MΩ | 自适应 | 计算结果 |
| 力值 | N | 2位小数 | 计算结果 |
| 电池指示 | - | 1位小数 | 原始数据 |
| UID | - | 十六进制 | tag.id |
| 时间戳 | - | HH:mm:ss | System.currentTimeMillis() |

---

## 附录A: FSR406阻值-力对照表(完整)

| 阻值(Ω) | 力(N) | 压力(mmHg) | 说明 |
|---------|-------|------------|------|
| 1,000,000 | 0.05 | 12.7 | 最小压力 |
| 100,000 | 0.1 | 25.4 | |
| 50,000 | 0.2 | 50.8 | |
| 30,000 | 0.38 | 96.4 | |
| 20,000 | 0.5 | 127.0 | |
| 10,000 | 1.0 | 254.0 | |
| 5,000 | 2.0 | 507.9 | |
| 2,000 | 5.0 | 1269.8 | |
| 1,000 | 10.0 | 2539.5 | 最大压力 |

*注: 压力计算基于FSR406面积3.08cm²*

---

## 附录B: 版本信息

| 项目 | 值 |
|------|-----|
| App版本 | 1.0.0 |
| versionCode | 1 |
| minSdk | 24 |
| targetSdk | 34 |
| compileSdk | 34 |
| Kotlin版本 | 1.9.x |
| Compose版本 | 2024.02.00 |
| JDK版本 | 17 |

---

*文档结束*

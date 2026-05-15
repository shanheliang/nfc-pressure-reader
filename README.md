# NFC压力读取App

Android应用，用于读取NFC无源压力传感卡片的数据，支持ISO 15693协议的NFC标签。

## 硬件要求

- Android 7.0 (API 24) 或更高版本
- NFC功能支持（设备需支持NFC）

## 功能特性

### 1. NFC读取
- 自动检测ISO 15693协议的NFC标签（ST25DV04KC）
- 读取EEPROM Block 0（压力数据）和Block 1（校准参数）
- 实时解析ADC值、FSR阻值、压力值

### 2. 压力显示
- 圆形仪表盘显示（0-120 mmHg范围）
- 颜色分区：绿色(0-30)、黄色(30-50)、红色(50+)
- 大数字压力值显示
- ADC原始值和FSR阻值详情

### 3. 两点校准
- 零点校准：移除压力时设置
- 参考点校准：施加已知压力时设置
- 校准数据持久化存储

### 4. 历史记录
- 最近10次读取记录
- 显示时间戳、压力值、ADC值、FSR阻值
- 支持清除历史

## 数据格式

### EEPROM Block 0（地址0x0000）
| 字节 | 内容 |
|------|------|
| 0 | ADC高8位 (ADC[11:4]) |
| 1 | ADC低4位 + 状态标志 |
| 2 | 电池电压指示 |
| 3 | 校验和 |

### EEPROM Block 1（地址0x0001）
| 字节 | 内容 |
|------|------|
| 0-1 | R1阻值（大端序，默认47000Ω） |
| 2 | V_EH标称值（0.1V单位，默认30=3.0V） |
| 3 | 预留 |

## 编译说明

### 环境要求
- Android Studio Hedgehog 或更高版本
- JDK 17
- Gradle 8.2

### 编译步骤

1. **克隆项目**
   ```bash
   git clone <repository_url>
   cd NFC压力读取App
   ```

2. **使用Android Studio打开**
   - 打开Android Studio
   - 选择 "Open an Existing Project"
   - 选择 `NFC压力读取App` 目录
   - 等待Gradle同步完成

3. **命令行编译**
   ```bash
   # 同步Gradle
   ./gradlew tasks
   
   # Debug构建
   ./gradlew assembleDebug
   
   # Release构建（需要签名配置）
   ./gradlew assembleRelease
   ```

4. **安装APK**
   ```bash
   # 通过adb安装
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

## 使用说明

### 首次使用

1. 确保设备NFC功能已开启（设置 → NFC）
2. 安装并打开应用
3. 首次读取时会显示NFC未启用提示，点击打开设置

### 读取压力数据

1. 将NFC传感器卡片放置在手机NFC天线区域
   - 通常位于手机背部摄像头附近
   - 部分手机位于屏幕中央
2. 保持手机贴近卡片，直到读取完成
3. 查看显示的压力值和详细信息

### 校准

1. 切换到"校准"Tab
2. **设置零点**：
   - 确保传感器无任何压力
   - 点击"设置零点"
3. **设置参考点**：
   - 在传感器上施加已知压力（建议40mmHg）
   - 输入实际压力值
   - 点击"设置参考"
4. 校准完成后自动应用

### 注意事项

- 读取时请保持手机与NFC标签稳定接触
- 如读取失败，请重新贴近标签
- 校准后读数会更准确
- 不同批次的传感器可能有差异

## 项目结构

```
NFC压力读取App/
├── app/
│   ├── src/main/
│   │   ├── java/com/nfcpressure/app/
│   │   │   ├── MainActivity.kt          # 主Activity
│   │   │   ├── data/                     # 数据层
│   │   │   │   ├── Models.kt             # 数据模型
│   │   │   │   ├── NfcVReader.kt         # NFC读取器
│   │   │   │   ├── PressureConverter.kt  # 压力转换
│   │   │   │   ├── CalibrationManager.kt # 校准管理
│   │   │   │   └── HistoryManager.kt     # 历史记录管理
│   │   │   ├── viewmodel/                # ViewModel
│   │   │   │   └── PressureViewModel.kt
│   │   │   └── ui/                       # UI层
│   │   │       ├── theme/                # 主题
│   │   │       ├── components/           # UI组件
│   │   │       └── screens/              # 屏幕
│   │   ├── res/                          # 资源文件
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

## 技术栈

- **语言**: Kotlin
- **UI框架**: Jetpack Compose
- **架构**: MVVM
- **状态管理**: StateFlow + Compose State
- **本地存储**: DataStore Preferences
- **NFC协议**: ISO 15693 (NfcV)

## 传感器数据转换

### ADC到压力计算流程

1. **读取ADC原始值**（12bit，0-4095）
2. **计算分压输出电压**：`Vout = ADC × (Vref / 4096)`
3. **计算FSR电阻**：`R_FSR = R1 × Vout / (V_EH - Vout)`
4. **查表转换力值**（FSR406阻值-力对照表）
5. **力转压力**：`P(mmHg) = F(N) / A(m²) / 133.322`
   - FSR406面积 ≈ 3.08 cm²

### FSR406阻值-力对照表

| 阻值 | 力 |
|------|-----|
| 1MΩ | 0.05N |
| 100kΩ | 0.1N |
| 50kΩ | 0.2N |
| 30kΩ | 0.38N |
| 20kΩ | 0.5N |
| 10kΩ | 1.0N |
| 5kΩ | 2.0N |
| 2kΩ | 5.0N |
| 1kΩ | 10.0N |

## 常见问题

### Q: 手机无法读取标签？
A: 
- 确认NFC功能已开启
- 尝试调整手机位置，天线位置因机型而异
- 确认标签为ISO 15693协议

### Q: 读数不准确？
A: 
- 进行两点校准
- 确认传感器放置正确
- 检查电池电压是否正常

### Q: 数据校验失败？
A: 
- 重新贴近标签
- 检查传感器MCU是否正常工作
- 可能是电磁干扰

## 许可证

MIT License

## 作者

NFC Pressure Team

## 版本历史

- **1.0.0** (2026-01-20)
  - 初始版本
  - 支持NFC读取和压力显示
  - 两点校准功能
  - 历史记录
# Test

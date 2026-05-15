package com.nfcpressure.app.data

import android.nfc.Tag
import android.nfc.tech.NfcV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * ISO 15693 (NFC-V) 读取器 - ST25DV04KC专用版本
 * 
 * 对比ST官方STAssetTracking_Android的ST25DVTag.kt，实现以下协议修复：
 * 1. Flags=0x02 (非寻址模式) + 去掉UID字段
 * 2. 使用Extended命令: 0x30 (读) / 0x31 (写)
 * 3. 块号使用2字节格式 (LSB + MSB)
 * 4. safeTransceive重试机制 (5次重试 + 5ms延迟)
 * 5. 响应校验: response[0] == 0x00 表示成功
 * 6. 新增能量采集控制功能
 */
class NfcVReader {
    
    companion object {
        // ========== 命令码定义 (ST25DV Extended Commands) ==========
        private const val REQUEST_HEADER: Byte = 0x02  // ST25DV请求头
        
        // ISO 15693 Extended Commands
        private const val CMD_EXTENDED_READ_SINGLE_BLOCK: Byte = 0x30  // Extended读单块
        private const val CMD_EXTENDED_WRITE_SINGLE_BLOCK: Byte = 0x31 // Extended写单块
        
        // ST25DV系统命令
        private const val CMD_PRESENT_PASSWORD: Byte = 0xB3.toByte()  // 密码认证
        private const val CMD_ENABLE_EH: Byte = 0xAE.toByte()          // 能量采集使能
        
        // ========== 重试机制参数 ==========
        private const val COMMAND_RETRY: Int = 5      // 最大重试次数
        private const val COMMAND_DELAY_MS: Long = 5L // 重试间隔(ms)
        
        // ========== 响应状态码 ==========
        private const val RESPONSE_OK: Byte = 0x00     // 成功响应
        
        // ========== 块地址常量 ==========
        const val BLOCK0_ADDR: Short = 0x0000  // 数据块
        const val BLOCK1_ADDR: Short = 0x0001  // 校准参数块
    }
    
    /**
     * Short扩展函数：获取低字节 (LSB)
     */
    private fun Short.lsb(): Byte = (this.toInt() and 0xFF).toByte()
    
    /**
     * Short扩展函数：获取高字节 (MSB)
     */
    private fun Short.msb(): Byte = ((this.toInt() shr 8) and 0xFF).toByte()
    
    // ========== 公开API ==========
    
    /**
     * 读取NFC标签数据
     * @param tag Tag对象
     * @return NfcRawData或null
     */
    suspend fun readTag(tag: Tag): NfcRawData? = withContext(Dispatchers.IO) {
        var nfcV: NfcV? = null
        
        try {
            nfcV = NfcV.get(tag)
            nfcV.connect()
            
            // 读取UID（仅用于返回数据，不参与命令）
            val uid = tag.id
            
            // 读取Block 0 (数据块)
            val block0 = readBlock(nfcV, BLOCK0_ADDR)
                ?: return@withContext null
            
            // 读取Block 1 (校准参数块)
            val block1 = readBlock(nfcV, BLOCK1_ADDR)
                ?: return@withContext null
            
            NfcRawData(
                uid = uid,
                block0 = block0,
                block1 = block1
            )
            
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            try {
                nfcV?.close()
            } catch (e: Exception) {
                // 忽略关闭异常
            }
        }
    }
    
    /**
     * 读取单个块 - 使用Extended Read Single Block (0x30)
     * 
     * 命令格式 (非寻址模式，不带UID):
     * +--------+--------+------+------+------+
     * | Flags  | Cmd    | Addr_LSB | Addr_MSB |
     * +--------+--------+------+------+------+
     * | 0x02   | 0x30   | LSB  | MSB  |
     * +--------+--------+------+------+------+
     * 共4字节
     */
    fun readBlock(nfcV: NfcV, address: Short): ByteArray? {
        val command = byteArrayOf(
            REQUEST_HEADER,            // 0x02: 非寻址模式
            CMD_EXTENDED_READ_SINGLE_BLOCK, // 0x30: Extended Read Single Block
            address.lsb(),             // 块号低字节
            address.msb()              // 块号高字节
        )
        
        return try {
            val response = safeTransceive(nfcV, command)
            // 响应格式: [0x00] [byte0] [byte1] [byte2] [byte3] (共5字节)
            response.copyOfRange(0, 4) // 返回4字节数据
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 写入单个块 - 使用Extended Write Single Block (0x31)
     * 
     * 命令格式:
     * +--------+--------+------+------+------+------+------+------+------+
     * | Flags  | Cmd    | Addr_LSB | Addr_MSB | D0   | D1   | D2   | D3   |
     * +--------+--------+------+------+------+------+------+------+------+
     * | 0x02   | 0x31   | LSB  | MSB  | data0 | data1 | data2 | data3 |
     * +--------+--------+------+------+------+------+------+------+------+
     * 共8字节
     * 
     * @param address 块地址
     * @param data 4字节数据
     * @return true成功, false失败
     */
    fun writeBlock(nfcV: NfcV, address: Short, data: ByteArray): Boolean {
        if (data.size != 4) {
            throw IllegalArgumentException("Data must be exactly 4 bytes")
        }
        
        val command = byteArrayOf(
            REQUEST_HEADER,             // 0x02: 非寻址模式
            CMD_EXTENDED_WRITE_SINGLE_BLOCK, // 0x31: Extended Write Single Block
            address.lsb(),             // 块号低字节
            address.msb(),             // 块号高字节
            data[0],                    // 数据字节0
            data[1],                    // 数据字节1
            data[2],                    // 数据字节2
            data[3]                     // 数据字节3
        )
        
        return try {
            safeTransceive(nfcV, command)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 启用能量采集
     * 
     * 流程:
     * 1. presentPassword() - 发送密码认证
     * 2. enableEH - 发送能量采集使能命令
     * 
     * 能量采集使能命令:
     * [0x02] [0xAE] [0x02] [0x02] [0x01]
     *  - 0x02: Flags
     *  - 0xAE: Enable EH命令
     *  - 0x02: 后续字节数
     *  - 0x02: 系统命令标识
     *  - 0x01: 使能 (0x00=禁用)
     */
    fun enableEnergyHarvesting(nfcV: NfcV): Boolean {
        return try {
            // 先进行密码认证
            presentPassword(nfcV)
            
            // 发送能量采集使能命令
            val command = byteArrayOf(
                REQUEST_HEADER,         // 0x02
                CMD_ENABLE_EH,          // 0xAE
                0x02,                   // 后续字节数
                0x02,                   // 系统命令
                0x01                    // 使能: 0x01
            )
            safeTransceive(nfcV, command)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 禁用能量采集
     * 
     * 能量采集禁用命令:
     * [0x02] [0xAE] [0x02] [0x02] [0x00]
     */
    fun disableEnergyHarvesting(nfcV: NfcV): Boolean {
        return try {
            // 先进行密码认证
            presentPassword(nfcV)
            
            // 发送能量采集禁用命令
            val command = byteArrayOf(
                REQUEST_HEADER,         // 0x02
                CMD_ENABLE_EH,          // 0xAE
                0x02,                   // 后续字节数
                0x02,                   // 系统命令
                0x00                    // 禁用
            )
            safeTransceive(nfcV, command)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 发送密码认证 (Present Password)
     * 
     * 默认使用全0密码:
     * [0x02] [0xB3] [0x02] [0x00 0x00 0x00 0x00 0x00 0x00 0x00 0x00]
     *  - 0x02: Flags
     *  - 0xB3: Present Password命令
     *  - 0x02: 后续字节数(密码ID长度)
     *  - 8字节: 全0密码
     */
    private fun presentPassword(nfcV: NfcV) {
        val command = byteArrayOf(
            REQUEST_HEADER,         // 0x02
            CMD_PRESENT_PASSWORD,    // 0xB3
            0x02,                   // 后续字节数
            0x00, 0x00, 0x00, 0x00, // 8字节全0密码
            0x00, 0x00, 0x00, 0x00
        )
        safeTransceive(nfcV, command)
    }
    
    /**
     * 安全传输 - 带重试机制的transceive
     * 
     * 参考ST官方ST25DVTag.kt实现:
     * - 最多重试5次
     * - 每次失败后延迟5ms
     * - 响应码0x00表示成功
     * - 成功后返回去掉状态码的纯数据
     */
    private fun safeTransceive(nfcV: NfcV, command: ByteArray): ByteArray {
        var nTry = 0
        var errorCode: Byte
        
        do {
            try {
                val response = nfcV.transceive(command)
                
                if (response.isNotEmpty()) {
                    errorCode = response[0]
                    
                    if (errorCode == RESPONSE_OK) {
                        // 成功: 返回去掉状态码的纯数据
                        return response.copyOfRange(1, response.size)
                    }
                } else {
                    errorCode = -1
                }
            } catch (e: IOException) {
                // IO异常，标记为错误
                errorCode = -2
            }
            
            // 失败，等待后重试
            nTry++
            if (nTry < COMMAND_RETRY) {
                Thread.sleep(COMMAND_DELAY_MS)
            }
            
        } while (nTry < COMMAND_RETRY)
        
        // 达到最大重试次数，抛出异常
        throw IOException("NFC transceive failed after $COMMAND_RETRY retries, error code: $errorCode")
    }
    
    // ========== 备用方法 ==========
    
    /**
     * 读取多个连续块 (保留的备用方法)
     * 当前App使用分块读取，此方法暂不使用
     */
    private fun readMultipleBlocks(
        nfcV: NfcV, 
        startAddress: Short, 
        blockCount: Byte
    ): ByteArray? {
        // Extended Read Multiple Block: 0x34
        val command = byteArrayOf(
            REQUEST_HEADER,  // 0x02
            0x34,            // Extended Read Multiple Blocks
            startAddress.lsb(),
            startAddress.msb(),
            blockCount
        )
        
        return try {
            val response = safeTransceive(nfcV, command)
            response
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

/**
 * NFC读取结果
 * 
 * 用于高层API返回读取状态
 */
sealed class NfcReadResult {
    /** 读取成功 */
    data class Success(val data: NfcRawData) : NfcReadResult()
    
    /** 读取失败 */
    data class Error(val message: String, val code: Int = -1) : NfcReadResult()
    
    /** 标签丢失 */
    object TagLost : NfcReadResult()
    
    /** 校验和错误 */
    object ChecksumError : NfcReadResult()
}

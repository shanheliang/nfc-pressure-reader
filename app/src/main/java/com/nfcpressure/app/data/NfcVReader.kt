package com.nfcpressure.app.data

import android.nfc.Tag
import android.nfc.tech.NfcV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ISO 15693 (NFC-V) 读取器
 * 用于读取ST25DV04KC NFC芯片
 */
class NfcVReader {
    
    companion object {
        // ISO 15693 命令码
        private const val CMD_READ_SINGLE_BLOCK = 0x20.toByte()
        private const val CMD_READ_MULTIPLE_BLOCKS = 0x23.toByte()
        
        // ST25DV EEPROM块地址
        const val BLOCK0_ADDR = 0x00.toByte()  // 数据块
        const val BLOCK1_ADDR = 0x01.toByte()  // 校准参数块
    }
    
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
            
            // 读取UID
            val uid = tag.id
            
            // 读取Block 0 (数据块)
            val block0 = readSingleBlock(nfcV, uid, BLOCK0_ADDR)
                ?: return@withContext null
            
            // 读取Block 1 (校准参数块)
            val block1 = readSingleBlock(nfcV, uid, BLOCK1_ADDR)
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
     * 读取单个块
     * Read Single Block命令格式：
     * flags(1B) + cmd(1B) + UID(8B) + block_number(1B)
     */
    private fun readSingleBlock(nfcV: NfcV, uid: ByteArray, blockNumber: Byte): ByteArray? {
        // flags: 高位设置表示"更多响应信息"位
        val flags = 0x02.toByte() // 0x02 = Subcarrier band = 0, Data rate = 0, 非_addressed模式
        
        // 构建命令
        val command = byteArrayOf(
            flags,
            CMD_READ_SINGLE_BLOCK,
            *uid,
            blockNumber
        )
        
        return try {
            val response = nfcV.transceive(command)
            // 响应格式：flags + block_data(4B)
            // 如果flags的bit 0 = 1表示有错误
            if (response.isNotEmpty() && (response[0].toInt() and 0x01) == 0) {
                // 提取4字节数据（跳过flags字节）
                response.copyOfRange(1, 5)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 读取多个连续块（备用方法）
     * Read Multiple Blocks命令格式：
     * flags(1B) + cmd(1B) + UID(8B) + first_block(1B) + num_blocks(1B)
     */
    private fun readMultipleBlocks(
        nfcV: NfcV, 
        uid: ByteArray, 
        startBlock: Byte, 
        blockCount: Byte
    ): ByteArray? {
        val flags = 0x02.toByte()
        
        val command = byteArrayOf(
            flags,
            CMD_READ_MULTIPLE_BLOCKS,
            *uid,
            startBlock,
            blockCount
        )
        
        return try {
            val response = nfcV.transceive(command)
            if (response.isNotEmpty() && (response[0].toInt() and 0x01) == 0) {
                // 提取数据（跳过flags字节）
                response.copyOfRange(1, response.size)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

/**
 * NFC读取结果
 */
sealed class NfcReadResult {
    data class Success(val data: NfcRawData) : NfcReadResult()
    data class Error(val message: String, val code: Int = -1) : NfcReadResult()
    object TagLost : NfcReadResult()
    object ChecksumError : NfcReadResult()
}

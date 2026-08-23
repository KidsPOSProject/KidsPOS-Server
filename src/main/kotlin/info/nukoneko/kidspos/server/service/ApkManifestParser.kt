package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.domain.exception.InvalidFileException
import org.springframework.stereotype.Component
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.util.zip.ZipInputStream

data class ApkManifestInfo(
    val versionName: String,
    val versionCode: Int,
)

@Component
class ApkManifestParser {
    fun parse(input: InputStream): ApkManifestInfo {
        val manifest =
            try {
                extractManifest(input)
            } catch (e: IOException) {
                throw InvalidFileException("APKファイルとして読み取れません: ${e.message}")
            } ?: throw InvalidFileException("APKファイルとして読み取れません（AndroidManifest.xml が見つかりません）")

        return try {
            parseManifest(manifest)
        } catch (e: IndexOutOfBoundsException) {
            throw InvalidFileException("AndroidManifest.xml の形式が不正です: ${e.message}")
        } catch (e: NegativeArraySizeException) {
            throw InvalidFileException("AndroidManifest.xml の形式が不正です: ${e.message}")
        }
    }

    private fun extractManifest(input: InputStream): ByteArray? {
        ZipInputStream(BufferedInputStream(input)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: return null
                if (entry.name == MANIFEST_ENTRY_NAME) {
                    return zip.readNBytes(MAX_MANIFEST_SIZE)
                }
            }
        }
    }

    private fun parseManifest(bytes: ByteArray): ApkManifestInfo {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        if (bytes.size < CHUNK_HEADER_SIZE || buffer.short(0) != CHUNK_TYPE_XML) {
            throw InvalidFileException("AndroidManifest.xml の形式が不正です")
        }

        val end = minOf(bytes.size, buffer.int(4).coerceAtLeast(CHUNK_HEADER_SIZE))
        var strings: List<String> = emptyList()
        var resourceIds: IntArray = IntArray(0)
        var offset = buffer.short(2)

        while (offset + CHUNK_HEADER_SIZE <= end) {
            val chunkType = buffer.short(offset)
            val chunkHeaderSize = buffer.short(offset + 2)
            val chunkSize = buffer.int(offset + 4)
            if (chunkSize < CHUNK_HEADER_SIZE || chunkHeaderSize < CHUNK_HEADER_SIZE || offset + chunkSize > end) {
                throw InvalidFileException("AndroidManifest.xml の形式が不正です")
            }

            when (chunkType) {
                CHUNK_TYPE_STRING_POOL -> strings = parseStringPool(buffer, bytes, offset)
                CHUNK_TYPE_RESOURCE_MAP -> resourceIds = parseResourceMap(buffer, offset, chunkHeaderSize, chunkSize)
                CHUNK_TYPE_START_ELEMENT -> {
                    val elementName = strings.getOrNull(buffer.int(offset + chunkHeaderSize + 4))
                    if (elementName == MANIFEST_ELEMENT_NAME) {
                        return parseManifestAttributes(buffer, offset, chunkHeaderSize, strings, resourceIds)
                    }
                }
            }

            offset += chunkSize
        }

        throw InvalidFileException("AndroidManifest.xml に manifest 要素が見つかりません")
    }

    private fun parseResourceMap(
        buffer: ByteBuffer,
        offset: Int,
        headerSize: Int,
        chunkSize: Int,
    ): IntArray {
        val count = (chunkSize - headerSize) / 4
        return IntArray(count) { buffer.int(offset + headerSize + it * 4) }
    }

    private fun parseStringPool(
        buffer: ByteBuffer,
        bytes: ByteArray,
        offset: Int,
    ): List<String> {
        val headerSize = buffer.short(offset + 2)
        val stringCount = buffer.int(offset + 8)
        val isUtf8 = buffer.int(offset + 16) and STRING_POOL_UTF8_FLAG != 0
        val stringsStart = buffer.int(offset + 20)

        return (0 until stringCount).map { index ->
            val position = offset + stringsStart + buffer.int(offset + headerSize + index * 4)
            if (isUtf8) readUtf8String(bytes, position) else readUtf16String(buffer, bytes, position)
        }
    }

    private fun readUtf16String(
        buffer: ByteBuffer,
        bytes: ByteArray,
        position: Int,
    ): String {
        var cursor = position
        var length = buffer.short(cursor)
        cursor += 2
        if (length and 0x8000 != 0) {
            length = ((length and 0x7FFF) shl 16) or buffer.short(cursor)
            cursor += 2
        }
        return String(bytes, cursor, length * 2, StandardCharsets.UTF_16LE)
    }

    private fun readUtf8String(
        bytes: ByteArray,
        position: Int,
    ): String {
        var cursor = position
        // UTF-8 プールは「UTF-16 換算の長さ」「UTF-8 バイト長」の順に格納されており、前者は読み飛ばす
        cursor += lengthFieldSize(bytes, cursor)
        val byteLength = readVariableLength(bytes, cursor)
        cursor += lengthFieldSize(bytes, cursor)
        return String(bytes, cursor, byteLength, StandardCharsets.UTF_8)
    }

    private fun readVariableLength(
        bytes: ByteArray,
        position: Int,
    ): Int {
        val first = bytes[position].toInt() and 0xFF
        return if (first and 0x80 == 0) {
            first
        } else {
            ((first and 0x7F) shl 8) or (bytes[position + 1].toInt() and 0xFF)
        }
    }

    private fun lengthFieldSize(
        bytes: ByteArray,
        position: Int,
    ): Int = if (bytes[position].toInt() and 0x80 == 0) 1 else 2

    private fun parseManifestAttributes(
        buffer: ByteBuffer,
        offset: Int,
        headerSize: Int,
        strings: List<String>,
        resourceIds: IntArray,
    ): ApkManifestInfo {
        val extension = offset + headerSize
        val attributeStart = buffer.short(extension + 8)
        val attributeSize = buffer.short(extension + 10)
        val attributeCount = buffer.short(extension + 12)

        var versionName: String? = null
        var versionCode: Int? = null

        for (index in 0 until attributeCount) {
            val attribute = extension + attributeStart + index * attributeSize
            val nameIndex = buffer.int(attribute + 4)
            val rawValueIndex = buffer.int(attribute + 8)
            val dataType = buffer.get(attribute + 15).toInt() and 0xFF
            val data = buffer.int(attribute + 16)

            when (attributeKey(nameIndex, strings, resourceIds)) {
                ATTRIBUTE_VERSION_NAME ->
                    versionName = resolveVersionName(dataType, data, rawValueIndex, strings)
                ATTRIBUTE_VERSION_CODE ->
                    versionCode = resolveVersionCode(dataType, data, strings)
            }
        }

        if (versionName.isNullOrBlank() || versionCode == null) {
            throw InvalidFileException("APKからバージョン情報を取得できませんでした")
        }
        return ApkManifestInfo(versionName, versionCode)
    }

    private fun attributeKey(
        nameIndex: Int,
        strings: List<String>,
        resourceIds: IntArray,
    ): String? =
        when (resourceIds.getOrNull(nameIndex)) {
            RESOURCE_ID_VERSION_CODE -> ATTRIBUTE_VERSION_CODE
            RESOURCE_ID_VERSION_NAME -> ATTRIBUTE_VERSION_NAME
            else -> strings.getOrNull(nameIndex)?.takeIf { it == ATTRIBUTE_VERSION_CODE || it == ATTRIBUTE_VERSION_NAME }
        }

    private fun resolveVersionName(
        dataType: Int,
        data: Int,
        rawValueIndex: Int,
        strings: List<String>,
    ): String {
        val index = if (dataType == VALUE_TYPE_STRING) data else rawValueIndex
        return strings.getOrNull(index)
            ?: throw InvalidFileException("APKのバージョン名がリソース参照のため読み取れません")
    }

    private fun resolveVersionCode(
        dataType: Int,
        data: Int,
        strings: List<String>,
    ): Int =
        when (dataType) {
            VALUE_TYPE_INT_DEC, VALUE_TYPE_INT_HEX -> data
            VALUE_TYPE_STRING -> strings.getOrNull(data)?.toIntOrNull()
            else -> null
        } ?: throw InvalidFileException("APKのバージョンコードを数値として読み取れません")

    private fun ByteBuffer.short(index: Int): Int = getShort(index).toInt() and 0xFFFF

    private fun ByteBuffer.int(index: Int): Int = getInt(index)

    companion object {
        private const val MANIFEST_ENTRY_NAME = "AndroidManifest.xml"
        private const val MANIFEST_ELEMENT_NAME = "manifest"
        private const val MAX_MANIFEST_SIZE = 8 * 1024 * 1024
        private const val CHUNK_HEADER_SIZE = 8
        private const val CHUNK_TYPE_XML = 0x0003
        private const val CHUNK_TYPE_STRING_POOL = 0x0001
        private const val CHUNK_TYPE_RESOURCE_MAP = 0x0180
        private const val CHUNK_TYPE_START_ELEMENT = 0x0102
        private const val STRING_POOL_UTF8_FLAG = 0x0100
        private const val RESOURCE_ID_VERSION_CODE = 0x0101021B
        private const val RESOURCE_ID_VERSION_NAME = 0x0101021C
        private const val ATTRIBUTE_VERSION_CODE = "versionCode"
        private const val ATTRIBUTE_VERSION_NAME = "versionName"
        private const val VALUE_TYPE_STRING = 0x03
        private const val VALUE_TYPE_INT_DEC = 0x10
        private const val VALUE_TYPE_INT_HEX = 0x11
    }
}

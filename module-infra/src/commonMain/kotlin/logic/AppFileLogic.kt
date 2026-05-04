package logic

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.SHA256
import kotlin.random.Random
import kotlin.time.Clock
import model.FileInfo
import neton.core.http.BadRequestException
import neton.core.http.HttpException
import neton.core.http.NetonErrorCode
import neton.core.http.UploadFile
import neton.database.dsl.*
import neton.logging.Logger
import neton.storage.StorageOperator
import neton.storage.WriteOptions
import table.FileInfoTable

/**
 * 应用唯一用户态文件入口（spec MODULE_MEMBER_PROFILE_SPEC §4.2）。
 *
 * 职责：
 * 1. 按 [FileBusinessPolicyRegistry] 校验 businessType / mime / size
 * 2. 计算 SHA-256 内容指纹
 * 3. 走 [StorageOperator]（neton-storage）写盘
 * 4. 落 `infra_files` 一行，返回 [FileInfo]（含 fileId / url）
 *
 * **目录布局是脚手架强制规范，不允许业务定制**：
 *
 * ```
 * {basePath}/{businessType}/{ownerUid}/{sha256}.{ext}
 * ```
 *
 * 同样地 url 格式也固定 `/app-api/infra/file/get/{fileId}`，不暴露磁盘路径。
 */
class AppFileLogic(
    private val log: Logger,
    private val storage: StorageOperator,
    private val policyRegistry: FileBusinessPolicyRegistry,
) {

    /**
     * 接收用户态上传：校验 → 写盘 → 落库。
     *
     * 同 sha256 + ownerUid + businessType 的内容上传第二次时，**复用已有记录**
     * （storage_key 完全一致，写盘是幂等覆盖，DB 直接返 existing fileId）。
     */
    suspend fun upload(
        ownerUid: Long,
        businessType: String,
        file: UploadFile,
    ): FileInfo {
        val policy = policyRegistry.policyOrNull(businessType)
            ?: throw BadRequestException("INVALID_BUSINESS_TYPE: $businessType")

        val mimeType = (file.contentType ?: "application/octet-stream").lowercase()
        if (mimeType !in policy.allowedMimeTypes) {
            throw HttpException(
                code = NetonErrorCode.INVALID_FORMAT,
                message = "INVALID_MIME_TYPE: $mimeType not allowed for $businessType",
            )
        }
        if (file.size > policy.maxSizeBytes) {
            throw HttpException(
                code = NetonErrorCode.PAYLOAD_TOO_LARGE,
                message = "FILE_TOO_LARGE: ${file.size} > ${policy.maxSizeBytes}",
            )
        }
        if (file.size <= 0) {
            throw BadRequestException("EMPTY_FILE")
        }

        val bytes = file.bytes()
        if (bytes.size.toLong() != file.size) {
            log.warn(
                "AppFileLogic.upload: declared size=${file.size} but bytes=${bytes.size} — using actual",
            )
        }

        val sha256Hex = sha256Hex(bytes)
        val ext = extensionFor(mimeType, file.filename)
        val storageKey = storageKeyOf(businessType, ownerUid, sha256Hex, ext)

        // 幂等：同 owner + same businessType + same sha256 已上传过 → 直接返 existing
        val existing = FileInfoTable.oneWhere {
            and(
                FileInfo::ownerUid eq ownerUid,
                FileInfo::businessType eq businessType,
                FileInfo::sha256 eq sha256Hex,
                FileInfo::status eq 1,
            )
        }
        if (existing != null) {
            log.info(
                "AppFileLogic.upload: dedup hit fileId=${existing.fileId} owner=$ownerUid sha=$sha256Hex",
            )
            return existing
        }

        // 写盘（neton-storage 抽象，第一版 local，后续切 S3 仅改 storage.conf）
        storage.write(storageKey, bytes, WriteOptions())

        val fileId = generateFileId()
        val url = publicUrlOf(fileId)
        val now = Clock.System.now().toEpochMilliseconds()

        val record = FileInfo(
            fileId = fileId,
            ownerUid = ownerUid,
            businessType = businessType,
            configId = null,
            name = file.filename,
            path = storageKey,
            url = url,
            mimeType = mimeType,
            size = bytes.size.toLong(),
            sha256 = sha256Hex,
            status = 1,
        )
        val inserted = FileInfoTable.insert(record)
        log.info(
            "AppFileLogic.upload: ok fileId=$fileId owner=$ownerUid type=$businessType size=${bytes.size} sha=$sha256Hex",
        )
        // KSP 生成的 insert 通常回填自增 id；fileId 等保持原值
        return inserted
    }

    /** 按 fileId 查记录；下载/引用时用。 */
    suspend fun getByFileId(fileId: String): FileInfo? {
        if (fileId.isBlank()) return null
        return FileInfoTable.oneWhere { FileInfo::fileId eq fileId }
    }

    /**
     * 按 fileId 读字节。仅供 [AppFileController] 在确认完归属/可见性后调用。
     */
    suspend fun readBytes(record: FileInfo): ByteArray =
        storage.read(record.path)

    /**
     * 鉴权语义（spec §4.2）：
     * - PUBLIC：任意已登录用户可读
     * - PRIVATE：仅 owner 可读
     */
    fun isReadableBy(record: FileInfo, viewerUid: Long): Boolean {
        if (record.status != 1) return false
        val type = record.businessType ?: return false
        val policy = policyRegistry.policyOrNull(type) ?: return false
        return when (policy.visibility) {
            FileVisibility.PUBLIC -> true
            FileVisibility.PRIVATE -> record.ownerUid == viewerUid
        }
    }

    companion object {
        /** spec §4.2：目录布局脚手架强制 `{businessType}/{ownerUid}/{sha256}.{ext}` */
        fun storageKeyOf(businessType: String, ownerUid: Long, sha256Hex: String, ext: String): String =
            "$businessType/$ownerUid/$sha256Hex.$ext"

        /** spec §4.2：返给客户端的 URL 永远走 `/app-api/infra/file/get/{fileId}` */
        fun publicUrlOf(fileId: String): String = "/app-api/infra/file/get/$fileId"

        private val sha256 = CryptographyProvider.Default.get(SHA256).hasher()

        private fun sha256Hex(bytes: ByteArray): String {
            val digest = sha256.hashBlocking(bytes)
            return digest.toHexString()
        }

        @OptIn(ExperimentalStdlibApi::class)
        private fun ByteArray.toHexString(): String =
            joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }

        private fun extensionFor(mimeType: String, fallbackName: String): String =
            when (mimeType) {
                "image/jpeg" -> "jpg"
                "image/png" -> "png"
                "image/webp" -> "webp"
                "application/pdf" -> "pdf"
                "text/plain" -> "txt"
                else -> fallbackName.substringAfterLast('.', missingDelimiterValue = "bin")
                    .lowercase()
                    .takeIf { it.isNotBlank() && it.length <= 8 }
                    ?: "bin"
            }

        private fun generateFileId(): String {
            // 16 byte random → 32 hex chars。`file_` 前缀方便 grep / log。
            val bytes = ByteArray(16).also { Random.nextBytes(it) }
            val hex = bytes.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
            return "file_$hex"
        }
    }
}

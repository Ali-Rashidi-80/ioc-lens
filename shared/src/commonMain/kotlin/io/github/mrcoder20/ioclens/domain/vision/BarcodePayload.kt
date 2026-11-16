package io.github.mrcoder20.ioclens.domain.vision

enum class BarcodeType {
    QR_CODE,
    DATA_MATRIX,
    AZTEC,
    PDF_417,
    CODE_128,
    CODE_39,
    OTHER
}

enum class QuishingRisk {
    SAFE,
    SUSPICIOUS,
    HIGH_RISK,
    UNKNOWN
}

data class BarcodePayload(
    val rawText: String,
    val defangedText: String,
    val type: BarcodeType,
    val detectedIocType: String?,
    val quishingRisk: QuishingRisk,
    val riskReason: String?,
    val unmaskedTargetUrl: String? = null,
    val punycodeHost: String? = null,
    val timestamp: Long = 0L
)

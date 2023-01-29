package de.thm.ap.mobile_scanner.model

import java.util.UUID

data class Image(
    val imageId: Long? = null,
    val uri: String? = null,
    var uuid: UUID? = null

)

package de.thm.ap.mobile_scanner.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity
data class Image(
    @PrimaryKey
    val imageId: Long? = null,
    val uri: String? = null,
    var uuid: UUID? = null

)

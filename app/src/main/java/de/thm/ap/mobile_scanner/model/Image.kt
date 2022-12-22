package de.thm.ap.mobile_scanner.model

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Image(
    @PrimaryKey
    val imageId: Long? = null,
    val uri: String? = null
)

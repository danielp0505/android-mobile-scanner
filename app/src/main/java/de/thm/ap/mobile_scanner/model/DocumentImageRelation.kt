package de.thm.ap.mobile_scanner.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    primaryKeys = ["documentId", "imageId"],
    foreignKeys = [
        ForeignKey(
            entity = Document::class,
            parentColumns = ["documentId"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Image::class,
            parentColumns = ["imageId"],
            childColumns = ["imageId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class DocumentImageRelation(
    @ColumnInfo(index = true)
    var documentId: Long = 0,
    @ColumnInfo(index = true)
    var imageId: Long = 0
)

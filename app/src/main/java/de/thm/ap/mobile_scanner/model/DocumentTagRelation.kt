package de.thm.ap.mobile_scanner.model

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
  primaryKeys = ["documentId", "tagId"]
)
data class DocumentTagRelation(
  @ColumnInfo(index = true)
  var documentId: Long = 0,
  @ColumnInfo(index = true)
  var tagId: Long = 0
)

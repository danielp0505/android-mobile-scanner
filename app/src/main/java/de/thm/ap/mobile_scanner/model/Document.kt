package de.thm.ap.mobile_scanner.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity()
data class Document(
  @PrimaryKey
  var documentId: Long? = null,
  var uuid: String = UUID.randomUUID().toString(),
  var title: String? = null,
)

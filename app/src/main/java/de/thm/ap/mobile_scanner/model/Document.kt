package de.thm.ap.mobile_scanner.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity()
data class Document(
  @PrimaryKey
  var documentId: Long? = null,
  /**
   * URI used for Firebase Firestore reference path
   */
  var uri: String? = null,
  var title: String? = null,
)

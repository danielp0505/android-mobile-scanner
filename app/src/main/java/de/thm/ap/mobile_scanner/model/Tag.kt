package de.thm.ap.mobile_scanner.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity()
data class Tag(
  @PrimaryKey var tagId: Long? = null,
  val name: String? = null
)

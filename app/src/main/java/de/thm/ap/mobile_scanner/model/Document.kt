package de.thm.ap.mobile_scanner.model

data class Document(
  var documentId: Long? = null,
  /**
   * URI used for Firebase Firestore reference path
   */
  var uri: String? = null,
  var title: String? = null,
)

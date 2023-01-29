package de.thm.ap.mobile_scanner.data

import de.thm.ap.mobile_scanner.model.*

interface DocumentDAO {

  data class TagWithDocuments(
    val tag: Tag,
    val documents: List<Document>
  )

  data class DocumentWithTags(
    val document: Document,
    val tags: List<Tag>
  )


  data class DocumentWithImages(
    val document: Document,
    val images: List<Image>

  )

}


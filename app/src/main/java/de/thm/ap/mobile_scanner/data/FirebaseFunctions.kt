package de.thm.ap.mobile_scanner.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import de.thm.ap.mobile_scanner.model.Document
import de.thm.ap.mobile_scanner.model.Tag

fun convertQueryToDocumentWithTagsList(querySnapshot: QuerySnapshot): MutableList<DocumentDAO.DocumentWithTags> {
    val docWithTagsList: MutableList<DocumentDAO.DocumentWithTags> = mutableListOf()
    var increment: Long = 0
    querySnapshot.forEach{ documentSnapshot: DocumentSnapshot ->
        val title = documentSnapshot.get("title")
        val tags = documentSnapshot.get("tags")
        val path = documentSnapshot.reference.path //use path in place of uri
        if(tags is Array<*> && tags.isArrayOf<String>() && title is String){
            val tagList: List<Tag> = tags.mapNotNull { name ->
                Tag(name = name as String?)
            }
            val documentWithTags = DocumentDAO.DocumentWithTags(
                Document(documentId = increment ,title = title, uri = path), tagList)
            docWithTagsList.add(documentWithTags)
            increment++
        }
    }
    return docWithTagsList
}
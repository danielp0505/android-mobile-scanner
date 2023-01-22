package de.thm.ap.mobile_scanner.data

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import de.thm.ap.mobile_scanner.model.Document
import de.thm.ap.mobile_scanner.model.Tag

fun convertQueryToDocumentWithTagsList(querySnapshot: QuerySnapshot): MutableList<DocumentDAO.DocumentWithTags> {
    val docWithTagsList: MutableList<DocumentDAO.DocumentWithTags> = mutableListOf()
    var increment: Long = 0
    querySnapshot.forEach { documentSnapshot: DocumentSnapshot ->
        val title = documentSnapshot.get("title")
        val tags = documentSnapshot.get("tags")
        val path = documentSnapshot.reference.path //use path in place of uri

        Log.v("DOCLIST", "Title: $title | Path: $path | Tags: $tags")
            var i: Long = 0
            val tagList: List<Tag> = if(tags is List<*>) tags.map{ tag -> Tag(i++, tag.toString())}
                                        else emptyList()
            val documentWithTags = DocumentDAO.DocumentWithTags(
                Document(documentId = increment, title = title as String?, uri = path), tagList
            )
            docWithTagsList.add(documentWithTags)
            increment++
    }
    return docWithTagsList
}

/**
 * Delete document and associated images given a valid document path
 */
//todo: implement delete
fun deleteDocumentAndImages(path: String){
    //Document Path Format: users/{userUID}/documents/{documentUID}
    //Image Storage: {userUID}/{documentUID}/{imageNumber}
}
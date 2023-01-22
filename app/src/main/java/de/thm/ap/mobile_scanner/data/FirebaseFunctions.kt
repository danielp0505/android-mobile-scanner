package de.thm.ap.mobile_scanner.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import de.thm.ap.mobile_scanner.model.Document
import de.thm.ap.mobile_scanner.model.Tag

fun convertQueryToDocumentWithTagsList(querySnapshot: QuerySnapshot): MutableList<DocumentDAO.DocumentWithTags> {
    val docWithTagsList: MutableList<DocumentDAO.DocumentWithTags> = mutableListOf()
    var increment: Long = 0
    querySnapshot.forEach { documentSnapshot: DocumentSnapshot ->
        val title = documentSnapshot.get("title")
        val tags = documentSnapshot.get("tags")
        val path = documentSnapshot.reference.path //use path in place of uri

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

fun deleteDocumentAndImages(path: String){
    //Document Path Format: users/{userUID}/documents/{documentUID}
    //Image Storage: {userUID}/{documentUID}/{imageNumber}
    Log.v("DELETION", "Path: $path")
    val firestore = Firebase.firestore
    val storage: StorageReference? =
    FirebaseAuth.getInstance().currentUser?.let {
        Firebase.storage
            .getReference(it.uid)
    }
    //delete images first, if something goes wrong the user can retry deleting the document
    firestore.document(path).let { docRef ->
        storage?.child(docRef.id)
                ?.listAll()?.addOnSuccessListener { listResult ->
                    var deletedItems = 0
                    val totalItems = listResult.items.size
                    listResult.items.forEach{
                        it.delete().addOnSuccessListener {
                            deletedItems++
                            if(deletedItems == totalItems){
                                docRef.delete().addOnSuccessListener {
                                }
                            }
                        }
                    }
            }
    }
}
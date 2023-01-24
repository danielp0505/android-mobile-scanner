package de.thm.ap.mobile_scanner.data

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import de.thm.ap.mobile_scanner.model.Document
import de.thm.ap.mobile_scanner.model.Image
import de.thm.ap.mobile_scanner.model.Tag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*
import kotlin.coroutines.CoroutineContext

fun runWithDocumentShapshot(documentUID: String, f: (DocumentSnapshot) -> Unit) {
    ReferenceCollection.userDocReference
        ?.collection("documents")
        ?.document(documentUID)
        ?.get()?.addOnSuccessListener(f)
}
val firebaseStorage: StorageReference? =
    FirebaseAuth.getInstance().currentUser?.let {
        Firebase.storage
            .getReference(it.uid)
    }

fun forEachFirebaseImage(context: CoroutineScope, documentUri: String, f: (Image) -> Unit){
    context.launch(Dispatchers.IO) {
        ReferenceCollection.userDocReference
            ?.collection("documents")
            ?.document(documentUri)?.get()?.await()?.let {
                val images = it.get("images")
                if (!(images is List<*>)) return@let

                images.map {
                    val uuid = UUID.fromString(it.toString())
                    val uri = firebaseStorage?.child(uuid.toString())?.downloadUrl?.await()

                    Image(uuid = UUID.fromString(it.toString()), uri =uri.toString())
                }.forEach(f)
            }

    }
}


fun convertQueryToDocumentWithTagsList(querySnapshot: QuerySnapshot): MutableList<DocumentDAO.DocumentWithTags> {
    val docWithTagsList: MutableList<DocumentDAO.DocumentWithTags> = mutableListOf()
    var increment: Long = 0
    querySnapshot.forEach { documentSnapshot: DocumentSnapshot ->
        val title = documentSnapshot.get("title")
        val tags = documentSnapshot.get("tags")
        val path = documentSnapshot.reference.id //use ID in place of uri

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
 * Delete document and associated images given a valid document UID of the user.
 */

fun deleteDocumentAndImages(UID: String){
    //Document Path Format: users/{userUID}/documents/{documentUID}
    //Image Storage: {userUID}/{documentUID}/{imageNumber}
    val firestore = Firebase.firestore
    val storage: StorageReference? =
    FirebaseAuth.getInstance().currentUser?.let {
        Firebase.storage
            .getReference(it.uid)
    }
    //delete images first, if something goes wrong the user can retry deleting the document
    ReferenceCollection.userDocReference?.collection("documents")?.document(UID).let { docRef ->
        if(docRef != null){
        storage?.child(docRef.id)
                ?.listAll()?.addOnSuccessListener { listResult ->
                var deletedItems = 0
                val totalItems = listResult.items.size
                listResult.items.forEach {
                    it.delete().addOnSuccessListener {
                        deletedItems++
                        if (deletedItems == totalItems) {
                            docRef.delete().addOnSuccessListener {
                            }
                        }
                    }
                }
            }
            }
    }
}
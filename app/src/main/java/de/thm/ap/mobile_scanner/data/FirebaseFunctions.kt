package de.thm.ap.mobile_scanner.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import de.thm.ap.mobile_scanner.model.Document
import de.thm.ap.mobile_scanner.model.Image
import de.thm.ap.mobile_scanner.model.Tag
import de.thm.ap.mobile_scanner.ui.screens.DocumentWithTags
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

val firebaseStorage: StorageReference? =
    FirebaseAuth.getInstance().currentUser?.let {
        Firebase.storage
            .getReference(it.uid)
    }

fun runWithDocumentSnapshot(documentUID: String, f: (DocumentSnapshot) -> Unit) {
    ReferenceCollection.userDocReference
        ?.collection("documents")
        ?.document(documentUID)
        ?.get()?.addOnSuccessListener(f)
}

suspend fun getFirebaseImages(documentUri: String): List<Image>{
    val images =
        ReferenceCollection
            .userDocReference
            ?.collection("documents")
            ?.document(documentUri)
            ?.get()?.await()?.get("images")
    if (images !is List<*>) return listOf()

    return images.map{it.toString()}.map {
        Image(
            uuid = UUID.fromString(it),
            uri = firebaseStorage?.child(it)?.downloadUrl?.await().toString()
        )
    }

}
suspend fun withFirebaseImages(documentUri: String, f: (List<Image>) -> Unit){
    f(getFirebaseImages(documentUri))
}

suspend fun forEachFirebaseImage(documentUri: String, f: (Image) -> Unit){
    withFirebaseImages(documentUri, {it.forEach { f(it) }})
}

fun convertQueryToDocumentWithTagsList(querySnapshot: QuerySnapshot): MutableList<DocumentWithTags> {
    return querySnapshot.mapIndexed { index, documentSnapshot: DocumentSnapshot ->
        val title = documentSnapshot.get("title")
        val tags = documentSnapshot.get("tags")
        val path = documentSnapshot.reference.id //use ID in place of uri

        val tagList: List<Tag> =
            if (tags is List<*>) tags.mapIndexed { i, tag -> Tag(i.toLong(), tag.toString()) }
            else emptyList()

            DocumentWithTags(
                Document(documentId = index.toLong(), title = title as String?, uri = path), tagList
            )
    }.toMutableList()
}

/**
 * Delete document and associated images given a valid document UID of the user.
 */
suspend fun deleteDocumentAndImages(documentUID: String) {
    //Document Path Format: users/{userUID}/documents/{documentUID}
    //Image Storage: {userUID}/{imageUUID}

    //delete images first, if something goes wrong the user can retry deleting the document
    val docRef =
        ReferenceCollection
            .userDocReference
            ?.collection("documents")
            ?.document(documentUID)
    if (docRef == null) return

    val documentSnapshot = docRef.get().await()

    val imageField = documentSnapshot.get("images")
    when (imageField) {
        null -> docRef.delete()
        is List<*> -> {
            imageField.map { it.toString() }.forEach { imageUID ->
                firebaseStorage?.child(imageUID)?.delete()?.await()
            }
            docRef.delete()
        }
    }
}

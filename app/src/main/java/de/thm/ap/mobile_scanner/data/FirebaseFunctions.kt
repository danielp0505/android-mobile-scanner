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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

fun runWithDocumentSnapshot(documentUID: String, f: (DocumentSnapshot) -> Unit) {
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

suspend fun getFirebaseImages(documentUri: String): List<Image>{
    val images =
        ReferenceCollection.userDocReference?.collection("documents")?.document(documentUri)?.get()
            ?.await()?.get("images")
    if (!(images is List<*>)) return listOf()

    return images.map {
        val uuid = UUID.fromString(it.toString())
        val uri = firebaseStorage?.child(uuid.toString())?.downloadUrl?.await()

        Image(uuid = UUID.fromString(it.toString()), uri = uri.toString())
    }

}
suspend  fun withFirebaseImages(documentUri: String, f: (List<Image>) -> Unit){
    f(getFirebaseImages(documentUri))
}

suspend fun forEachFirebaseImage(documentUri: String, f: (Image) -> Unit){
    withFirebaseImages(documentUri, {it.forEach { f(it) }})
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

fun deleteDocumentAndImages(UID: String, scope: CoroutineScope) {
    //Document Path Format: users/{userUID}/documents/{documentUID}
    //Image Storage: {userUID}/{imageUUID}
    val storage: StorageReference? =
    FirebaseAuth.getInstance().currentUser?.let {
        Firebase.storage
            .getReference(it.uid)
    }
    //delete images first, if something goes wrong the user can retry deleting the document
    ReferenceCollection.userDocReference?.collection("documents")?.document(UID).let { docRef ->
        docRef?.get()?.addOnSuccessListener { documentSnapshot ->
            val imageField = documentSnapshot.get("images")

            if(imageField == null){
                docRef.delete()
            } else
                if(imageField is List<*>) {
                    val imageUIDs: List<String> = imageField.map{it.toString()}
                    scope.launch(Dispatchers.IO) {
                        imageUIDs.forEach { imageUID ->
                            storage?.child(imageUID)?.delete()?.await()
                        }
                        docRef.delete()
                    }
            }
        }
    }
}
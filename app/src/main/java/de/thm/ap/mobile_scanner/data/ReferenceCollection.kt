package de.thm.ap.mobile_scanner.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.firebase.firestore.DocumentReference

/**
 * Object containing Firebase Firestore references used in multiple screens
 */

object ReferenceCollection {
    /**
     * References document in Firestore corresponding to the user's Firebase Authentication UID
     */
    var userDocReference: DocumentReference? by mutableStateOf(null)
}
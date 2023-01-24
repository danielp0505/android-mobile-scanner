package de.thm.ap.mobile_scanner.ui.screens

import android.app.Application
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import de.thm.ap.mobile_scanner.data.AppDatabase
import de.thm.ap.mobile_scanner.model.Document
import de.thm.ap.mobile_scanner.R
import de.thm.ap.mobile_scanner.data.forEachFirebaseImage
import de.thm.ap.mobile_scanner.data.runWithDocumentShapshot
import kotlinx.coroutines.launch

class DocumentViewScreenViewModel(app: Application) : AndroidViewModel(app) {
    val dao = AppDatabase.getDb(app).documentDao()
    var document by mutableStateOf(Document())
    var images = mutableStateListOf<Uri>()

    fun initDocument(documentUID: String) {
        runWithDocumentShapshot(documentUID){ documentSnapshot ->
            val title = documentSnapshot.get("title")
            document =
                when(title) {
                    is String ->
                        Document(title = title as String?, uri = documentSnapshot.reference.id)
                    else -> Document(uri = documentSnapshot.reference.id)
                }
            images = images.also { it.clear() }
            forEachFirebaseImage(documentSnapshot.reference.id) { uri ->
                images = images.also { it.add(uri); it.sort() }
            }
        }
    }
}

@Composable
fun DocumentViewScreen(
    documentId: String,
    navigateUp: () -> Unit,
    editDocument: (String) -> Unit
) {
    val vm: DocumentViewScreenViewModel = viewModel()
    vm.initDocument(documentId)

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(vm.document?.title ?: stringResource(id = R.string.unnamed_document)) },
            navigationIcon = {
                IconButton(onClick = { navigateUp() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = stringResource(id = R.string.navigation_back)
                    )
                }
            }, actions = {
                IconButton(onClick = {editDocument(documentId)}) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.edit_document)
                    )
                }
            }
        )
    }) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            itemsIndexed(vm.images) { i, it ->
                AsyncImage(
                    modifier = Modifier.fillMaxWidth(),
                    model = it,
                    contentDescription = "Page Nr. ${i}"
                )
            }
        }

    }
}

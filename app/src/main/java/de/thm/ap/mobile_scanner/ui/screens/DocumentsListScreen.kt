package de.thm.ap.mobile_scanner.ui.screens

import android.app.Application
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.transition.Transition
import android.util.Log
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.Coil
import coil.request.ImageRequest
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.google.firebase.firestore.ListenerRegistration
import de.thm.ap.mobile_scanner.R
import de.thm.ap.mobile_scanner.data.*
import de.thm.ap.mobile_scanner.model.Document
import de.thm.ap.mobile_scanner.model.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.jetbrains.annotations.Nullable
import java.io.File
import java.io.FileOutputStream


class DocumentsListViewModel(app: Application) : AndroidViewModel(app) {
    private val docDAO: DocumentDAO = AppDatabase.getDb(app.baseContext).documentDao()
    var isSearching by mutableStateOf(false)
    fun set(it: Boolean) {
        searchString = ""
        it
    }

    var searchString by mutableStateOf("")

    var contextualMode by mutableStateOf(false)
    var selectedDocuments: MutableList<Document> = mutableStateListOf<Document>()

    var snapshotListener: ListenerRegistration? = null
    var documents: List<DocumentDAO.DocumentWithTags> by mutableStateOf(emptyList())

    fun deleteDocument(document: Document) {
        document.uri?.let {
            deleteDocumentAndImages(it, viewModelScope)
        }
    }

    fun deleteSelection() {
        selectedDocuments.forEach { document ->
            deleteDocument(document)
        }
        exitContextualMode()
    }

    fun exitContextualMode() {
        contextualMode = false
        selectedDocuments.clear()
    }

    fun shareDocument(context: Context, documentUID: String) {
        ReferenceCollection.userDocReference
            ?.collection("documents")
            ?.document(documentUID)?.get()
            ?.addOnSuccessListener { documentSnapshot ->
                viewModelScope.launch(Dispatchers.IO) {
                    val imageUUIDs = documentSnapshot.get("images")
                    if (imageUUIDs is List<*>) {
                        val images: List<Uri?> = imageUUIDs.map { UUID ->
                            firebaseStorage?.child(UUID.toString())
                                ?.downloadUrl?.await()
                        }
                        Log.v("SHARE", images.toString())
                        /* Caching File locally to share
                        val cachedFiles: MutableList<File> = mutableListOf()
                        val i = 0
                        images.forEach { uri: Uri? ->
                            uri?.let{
                                val request = ImageRequest.Builder(context)
                                    .data(uri.toString())
                                    .build()
                                val drawable: Drawable? = Coil.imageLoader(context).execute(request).drawable
                                if(drawable is BitmapDrawable){
                                    val cacheFile = File(context.cacheDir, "$i.png")
                                    val fileOutputStream = FileOutputStream(cacheFile)
                                    val bitmap = drawable.bitmap
                                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
                                    fileOutputStream.flush()
                                    fileOutputStream.close()
                                    cachedFiles.add(cacheFile)
                                }
                            }
                        }
                        val localUri: List<Uri> = cachedFiles.map { cachedFile ->
                            FileProvider.getUriForFile(context,
                                "de.thm.fileprovider", cachedFile)
                        }
                        Log.v("SHARE", cachedFiles.toString())
                        Log.v("SHARE", localUri.toString())
                        */

                        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                            type = "image/*"
                            var clip: ClipData? = null
                            images.forEach {
                                if(it != null) {
                                    if (clip == null) {
                                        clip = ClipData.newRawUri("", it)
                                    } else {
                                        clip!!.addItem(ClipData.Item(it))
                                    }
                                }
                            }
                            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(images))

                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }.also {
                            context.startActivity(Intent.createChooser(it, "Dokument teilen"))
                        }
                    }
                }
            }
    }

    fun initQueries() {
        ReferenceCollection.userDocReference?.collection("documents")
            ?.get()
            ?.addOnSuccessListener { querySnapshot ->
                if (querySnapshot != null) {
                    documents = convertQueryToDocumentWithTagsList(querySnapshot)
                }
            }
        if (snapshotListener == null) {
            snapshotListener = ReferenceCollection.userDocReference
                ?.collection("documents")
                ?.addSnapshotListener { querySnapshot, error ->
                    if (error == null && querySnapshot != null) {
                        documents = convertQueryToDocumentWithTagsList(querySnapshot)
                    }
                }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DocumentListTopAppBar(
    openTagManagement: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val vm: DocumentsListViewModel = viewModel()
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    when {
        vm.contextualMode -> {
            TopAppBar(title = { Text(stringResource(id = R.string.app_name)) },
                backgroundColor = MaterialTheme.colors.background,
                navigationIcon = {
                    IconButton(onClick = { vm.exitContextualMode() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(id = R.string.navigation_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { vm.deleteSelection() }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(id = R.string.delete_documents)
                        )
                    }
                })
        }
        vm.isSearching -> {
            TopAppBar(backgroundColor = MaterialTheme.colors.background) {
                Row() {
                    IconButton(onClick = {
                        vm.isSearching = false
                        vm.searchString = ""

                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(
                                id = R.string.navigation_back
                            )
                        )
                    }
                    TextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        value = vm.searchString,
                        onValueChange = { vm.searchString = it },
                        colors = TextFieldDefaults.textFieldColors(
                            backgroundColor = MaterialTheme.colors.primarySurface,
                            cursorColor = MaterialTheme.colors.onBackground,
                            textColor = MaterialTheme.colors.onBackground,
                            focusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        placeholder = {Text(stringResource(id = R.string.search_placeholder))},
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            keyboardController?.hide()
                        })
                    )
                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                    }
                }
            }
        }
        else -> {
            TopAppBar(title = { Text(stringResource(id = R.string.app_name)) }, actions = {
                IconButton(onClick = {
                    vm.isSearching = true
                }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(id = R.string.search)
                    )
                }
                IconButton(onClick = {
                    openTagManagement()
                }) {
                    Icon(
                        imageVector = Icons.Rounded.AccountCircle,
                        contentDescription = stringResource(id = R.string.my_account)
                    )
                }
            })
        }
    }

}


@Composable
fun DocumentsListScreen(
    openDocument: (id: String) -> Unit,
    openUserScreen: () -> Unit,
    addDocument: () -> Unit,
    editDocument: (String) -> Unit,
) {
    val vm: DocumentsListViewModel = viewModel()
    val context = LocalContext.current

    if(ReferenceCollection.userDocReference != null){
        vm.initQueries()
    }
    val documentsWithTags = vm.documents

    Scaffold(topBar = { DocumentListTopAppBar(openUserScreen) }, floatingActionButton = {
        AddDocumentButton { addDocument() }
    }, bottomBar = {
        BottomAppBar(
        ) {
            Text(text = stringResource(id = R.string.number_of_documents) + ": " + documentsWithTags.size)
            Spacer(modifier = Modifier.weight(1f))
        }
    }) { innerPadding ->
        if (documentsWithTags.isEmpty()) {
            Text(
                text = stringResource(id = R.string.no_documents_found),
                style = MaterialTheme.typography.h6,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
            )
        } else {
            val lazyListState = rememberLazyListState()
            LazyColumn(contentPadding = innerPadding, state = lazyListState) {
                items(items = documentsWithTags.filter {
                    it.matches(vm.searchString) ||
                    it.document.title?.lowercase()?.contains(vm.searchString.lowercase()) == true
                }, key = { it.document.documentId!! }) { documentWithTags ->
                    DocumentListItem(
                        document = documentWithTags.document,
                        tags = documentWithTags.tags,
                        editDocument = {editDocument(documentWithTags.document.uri!!)},
						openDocument = { openDocument(documentWithTags.document.uri!!) },
                        shareDocument = {
                            documentWithTags.document.uri?.let{vm.shareDocument(context, it)}}
                    )
                    Divider(color = Color.Gray)
                }
            }
        }
    }
}

private fun DocumentDAO.DocumentWithTags.matches(searchString: String): Boolean{
    if (searchString.isEmpty()) return true
    if (document.title?.contains(searchString, ignoreCase = true) ?: false) return true
    if (tags.any { it.name?.contains(searchString, ignoreCase = true) ?:  false}) return true
    return false
}


@Composable
fun AddDocumentButton(onClick: () -> Unit) {
    FloatingActionButton(onClick = {onClick()}) {
        Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.create_document))
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
fun DocumentListItem(
    document: Document,
    tags: List<Tag>,
    editDocument: () -> Unit,
    openDocument: () -> Unit,
	shareDocument: () -> Unit
) {
    val vm: DocumentsListViewModel = viewModel()
    Row(
    ) {
        ListItem(modifier = Modifier
            .background(
                color = if (vm.selectedDocuments.contains(document)) MaterialTheme.colors.secondary
                else MaterialTheme.colors.background
            )
            .combinedClickable(
                onClick = {
                    when {
                        vm.contextualMode && !vm.selectedDocuments.contains(document) -> vm.selectedDocuments.add(
                            document
                        )
                        vm.contextualMode && vm.selectedDocuments.contains(document) -> vm.selectedDocuments.remove(
                            document
                        )
                        else -> openDocument()
                    }
                },
                onLongClick = {
                    vm.selectedDocuments.add(document)
                    vm.contextualMode = true
                },
            ), text = {
            val title: String =
                if (document.title == null) stringResource(id = R.string.unnamed_document)
                else document.title!!
            Text(
                text = title, maxLines = 1, overflow = TextOverflow.Clip,
                fontWeight = FontWeight.SemiBold
            )
        }, secondaryText = {
                LazyRow(
                    modifier = Modifier.padding(vertical = 4.dp),
                    content = {
                    items(items = tags, key = { it.tagId!! }) { tag ->
                        TagButton(tag = tag, onClick = {
                            vm.searchString = tag.name!!
                            vm.isSearching = true
                        })
                    }
                })
            },
            trailing = {
                Row() {
                    IconButton(onClick = { editDocument() }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(id = R.string.edit)
                        )
                    }
                    IconButton(onClick = { shareDocument() }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(id = R.string.share)
                        )
                    }
                }
            }
        )
    }
}

@Composable
fun TagButton(tag: Tag, onClick: () -> Unit) {
    Button(content = {
        Text(
            text = tag.name ?: stringResource(
                id = R.string.unknown
            ), fontSize = 11.sp
        )
    },
        onClick = { onClick() },
        shape = RoundedCornerShape(15.dp),
        modifier = Modifier.height(30.dp)
    )
}
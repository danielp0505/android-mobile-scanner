package de.thm.ap.mobile_scanner.ui.screens

import android.app.Application
import android.content.ClipData
import android.content.Context
import android.content.Intent
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
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import de.thm.ap.mobile_scanner.R
import de.thm.ap.mobile_scanner.data.*
import de.thm.ap.mobile_scanner.model.Document
import de.thm.ap.mobile_scanner.model.Tag
import kotlinx.coroutines.launch

class DocumentsListViewModel(app: Application) : AndroidViewModel(app) {
    var isSearching by mutableStateOf(false)
    fun set(it: Boolean) {
        searchString = ""
        it
    }

    var searchString by mutableStateOf("")

    var contextualMode by mutableStateOf(false)
    var selectedDocuments: MutableList<Document> = mutableStateListOf<Document>()

    val auth: FirebaseAuth = FirebaseAuth.getInstance()

    var snapshotListener: ListenerRegistration? = null
    var documents: List<DocumentWithTags> by mutableStateOf(emptyList())

    fun deleteDocument(document: Document) {
        document.uri?.let {
            viewModelScope.launch {
                deleteDocumentAndImages(it)
            }
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
        viewModelScope.launch {

            val images = getFirebaseImages(documentUID).map { it.uri?.toUri() }
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/*"
                var clip: ClipData? = null
                images.forEach {
                    if (clip == null) {
                        clip = ClipData.newRawUri("", it)
                    } else {
                        clip!!.addItem(ClipData.Item(it))
                    }
                }
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(images))

                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }.also {
                context.startActivity(Intent.createChooser(it, "Dokument teilen"))
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
                        //TODO
                        shareDocument = {vm.shareDocument(context, documentWithTags.document.uri.toString())}
                    )
                    Divider(color = Color.Gray)
                }
            }
        }
    }
}

data class DocumentWithTags(
    val document: Document,
    val tags: List<Tag>
){
    fun matches(searchString: String): Boolean{
        if (searchString.isEmpty()) return true
        if (document.title?.contains(searchString, ignoreCase = true) ?: false) return true
        if (tags.any { it.name?.contains(searchString, ignoreCase = true) ?:  false}) return true
        return false
    }

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
            .background(color = if (vm.selectedDocuments.contains(document)) MaterialTheme.colors.secondary
                                else MaterialTheme.colors.background)
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
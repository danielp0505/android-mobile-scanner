package de.thm.ap.mobile_scanner.ui.screens

import android.app.Application
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import de.thm.ap.mobile_scanner.R
import de.thm.ap.mobile_scanner.data.*
import de.thm.ap.mobile_scanner.model.Document
import de.thm.ap.mobile_scanner.model.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class DocumentsListViewModel(app: Application) : AndroidViewModel(app) {
    private val docDAO: DocumentDAO = AppDatabase.getDb(app.baseContext).documentDao()
    val documentsWithTags: LiveData<List<DocumentDAO.DocumentWithTags>> =
        docDAO.findAllDocumentsWithTagsSync()

    var contextualMode by mutableStateOf(false)
    var selectedDocuments: MutableList<Document> = mutableStateListOf<Document>()

    val auth: FirebaseAuth = FirebaseAuth.getInstance()

    var snapshotListener: ListenerRegistration? = null
    var firestoreDocs: List<DocumentDAO.DocumentWithTags> by mutableStateOf(emptyList())

    fun deleteDocument(document: Document) {
        document.uri?.let {
            deleteDocumentAndImages(it)
        }
        /*
        viewModelScope.launch(Dispatchers.IO) {
            docDAO.delete(document)
        }*/
    }

    fun deleteSelection() {
        selectedDocuments.forEach{document ->
            deleteDocument(document)
        }
        exitContextualMode()
        /* Local deletion
        viewModelScope.launch(Dispatchers.IO) {
            docDAO.deleteDocumentList(selectedDocuments)
            launch(Dispatchers.Main) {
                exitContextualMode()
            }

        }
         */
    }

    fun toggleWithSelection(document: Document) {
        if (!contextualMode) {
            contextualMode = true
        }
        if (selectedDocuments.removeIf { it.documentId == document.documentId }) {
            if (selectedDocuments.size == 0) {
                exitContextualMode()
            }
        } else {
            selectedDocuments.add(document)
        }
    }

    fun exitContextualMode() {
        contextualMode = false
        selectedDocuments.clear()
    }

    fun initQueries(){
        ReferenceCollection.userDocReference?.collection("documents")
            ?.get()
            ?.addOnSuccessListener { querySnapshot ->
                if (querySnapshot != null) {
                    firestoreDocs = convertQueryToDocumentWithTagsList(querySnapshot)
                }
            }
        if(snapshotListener == null) {
            snapshotListener = ReferenceCollection.userDocReference
                ?.collection("documents")
                ?.addSnapshotListener { querySnapshot, error ->
                    if (error == null && querySnapshot != null) {
                        firestoreDocs = convertQueryToDocumentWithTagsList(querySnapshot)
                    }
                }
        }
    }

}

@Composable
fun DocumentsListScreen(
    openTagManagement: () -> Unit,
    addDocument: () -> Unit,
    editDocument: (Long) -> Unit,
    login: () -> Unit,
    logout: () -> Unit,
) {
    val vm: DocumentsListViewModel = viewModel()
    if(ReferenceCollection.userDocReference != null){
        vm.initQueries()
    }
    //val documentsWithTags by vm.documentsWithTags.observeAsState(initial = emptyList())
    val documentsWithTags = vm.firestoreDocs

    Scaffold(
        topBar =
        {
            TopAppBar(
                title = { Text(stringResource(id = R.string.app_name)) },
                actions = {
                    IconButton(
                        onClick = {
                            openTagManagement()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(id = R.string.manage_tags)
                        )
                    }
                    if (vm.contextualMode) {
                        IconButton(
                            onClick = {}
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = stringResource(id = R.string.share_documents)
                            )
                        }
                        IconButton(
                            onClick = {
                                //todo: "Are you sure?" - popup
                                vm.deleteSelection()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(id = R.string.delete_documents)
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            AddDocumentButton({addDocument()})
        },
        bottomBar = {
            BottomAppBar(
            ) {
                Text(text = stringResource(id = R.string.number_of_documents) + ": " + documentsWithTags.size)
                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = {
                    if(vm.auth.currentUser == null){
                        login()
                    } else {
                        logout()
                    }
                }) {
                    var textId by remember{mutableStateOf(if (vm.auth.currentUser == null) R.string.login else R.string.logout)}
                    vm.auth.addAuthStateListener {
                        textId = if (vm.auth.currentUser == null) R.string.login else R.string.logout
                    }
                    Text(text = stringResource(id = textId))
                }
            }
        }
    ) { innerPadding ->
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
                items(
                    items = documentsWithTags,
                    key = { it.document.documentId!! }
                ) { documentWithTags ->
                    DocumentListItem(
                        document = documentWithTags.document,
                        tags = documentWithTags.tags,
                        viewModel = vm,
                        editDocument = {editDocument(documentWithTags.document.documentId!!)}
                    )
                    Divider(color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun AddDocumentButton(onClick: () -> Unit) {
    FloatingActionButton(onClick = {onClick()}) {
        Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.create_document))
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DocumentListItem(document: Document, tags: List<Tag>, viewModel: DocumentsListViewModel, editDocument: () -> Unit) {
    val swipeableState = rememberSwipeableState(initialValue = 0)
    val sizePx = with(LocalDensity.current) { -60.dp.toPx() }
    val anchors = mapOf(0f to 0, sizePx to 1)
    val coroutineScope = rememberCoroutineScope()
    Row(
        modifier = Modifier
            .swipeable(
                state = swipeableState,
                orientation = Orientation.Horizontal,
                anchors = anchors
            )
    ) {
        ListItem(
            modifier = Modifier
                .offset { IntOffset(swipeableState.offset.value.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { viewModel.toggleWithSelection(document) },
                        onPress = {
                            if (viewModel.contextualMode) {
                                viewModel.toggleWithSelection(document)
                            }
                        }
                    )
                }
                .background(color = if (viewModel.selectedDocuments.contains(document)) Color.Green else MaterialTheme.colors.background),
            text = {
                val title: String =
                    if (document.title == null) stringResource(id = R.string.unnamed_document) else document.title!!
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            },
            secondaryText = {
                LazyRow(content = {
                    items(items = tags, key = { it.tagId!! }) { tag ->
                        TagButton(tag = tag, onClick = null)
                    }
                })
            },
            trailing = {
                Row() {
                    IconButton(
                        onClick = {
                            viewModel.deleteDocument(document)
                            coroutineScope.launch(Dispatchers.Main) {
                                swipeableState.snapTo(0)
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(id = R.string.delete)
                        )
                    }
                   IconButton(onClick = {editDocument()}) {
                       Icon(
                           imageVector = Icons.Default.Edit,
                           contentDescription = stringResource(id = R.string.edit)
                       )
                   } 
                }
            }
        )
        //TODO: fix animation
        AnimatedVisibility(
            visible = (swipeableState.offset.value <= sizePx),
            enter = slideInHorizontally(initialOffsetX = { -1 }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { -1 }) + fadeOut()
        ) {
            IconButton(
                onClick = {
                    viewModel.deleteDocument(document)
                    coroutineScope.launch(Dispatchers.Main) {
                        swipeableState.snapTo(0)
                    }
                },
                modifier = Modifier.background(Color.Red)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(id = R.string.delete)
                )
            }
        }
    }
}

@Composable
fun TagButton(tag: Tag, onClick: Function<R>?) {
    Button(content = {
        Text(
            text = tag.name
                ?: stringResource(
                    id = R.string.unknown
                ),
            fontSize = 11.sp
        )
    }, onClick = { onClick },
        shape = RoundedCornerShape(15.dp),
        modifier = Modifier
            .height(28.dp)
    )
}
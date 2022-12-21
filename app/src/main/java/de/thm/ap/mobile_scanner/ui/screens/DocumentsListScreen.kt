package de.thm.ap.mobile_scanner.ui.screens

import android.app.Application
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import de.thm.ap.mobile_scanner.R
import de.thm.ap.mobile_scanner.data.AppDatabase
import de.thm.ap.mobile_scanner.data.DocumentDAO
import de.thm.ap.mobile_scanner.model.Document
import de.thm.ap.mobile_scanner.model.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class DocumentsListViewModel(app: Application) : AndroidViewModel(app) {
    private val docDAO: DocumentDAO = AppDatabase.getDb(app.baseContext).documentDao()
    val documents: LiveData<List<Document>> = docDAO.findAllDocumentsSync()

    var contextualMode by mutableStateOf(false)
    var selectedDocuments: MutableList<Document> = mutableStateListOf()

    fun createTestData() {
        viewModelScope.launch(Dispatchers.IO) {
            val rrID: Long = docDAO.persist(Document(title = "Restaurant Receipt"))
            val tfID: Long = docDAO.persist(Document(title = "Tax Forms"))
            val kksfID: Long = docDAO.persist(Document(title = "The Krusty Krab Secret Formula"))
            val fbiID: Long = docDAO.persist(Document(title = "FBI - Leaked Documents"))
            val embID: Long = docDAO.persist(Document(title = "Embarrassing Purchase"))
            val brthID: Long = docDAO.persist(Document(title = "Birth certificate"))
            val nonameID: Long = docDAO.persist(Document())

            val fdID: Long = docDAO.persist(Tag(name = "Food & Drink"))
            val finID: Long = docDAO.persist(Tag(name = "Finances"))
            val perID: Long = docDAO.persist(Tag(name = "Personal Secret"))

            docDAO.persist(rrID, fdID)
            docDAO.persist(rrID, finID)
            docDAO.persist(tfID, finID)
            docDAO.persist(kksfID, fdID)
            docDAO.persist(fbiID, perID)
            docDAO.persist(embID, perID)
            docDAO.persist(embID, finID)
            docDAO.persist(brthID, perID)
            docDAO.persist(nonameID, perID)
        }
    }

    fun deleteDocument(document: Document) {
        viewModelScope.launch(Dispatchers.IO) {
            docDAO.delete(document)
        }
    }
}

@Composable
fun DocumentsListScreen() {
    val vm: DocumentsListViewModel = viewModel()
    val documents by vm.documents.observeAsState(initial = emptyList())
    Scaffold(
        topBar =
        {
            TopAppBar(
                title = { Text(stringResource(id = R.string.app_name)) },
                actions = {
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
                            vm.createTestData()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Create,
                            contentDescription = "Create Test Data"
                        )
                    }
                    IconButton(
                        onClick = {

                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(id = R.string.delete_documents)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (documents.isEmpty()) {
            Text(
                text = stringResource(id = R.string.no_documents_found),
                style = MaterialTheme.typography.h6,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
            )
        } else {
            LazyColumn(contentPadding = innerPadding) {
                items(documents) { document ->
                    DocumentListItem(document = document, vm)
                    Divider(color = Color.Gray)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DocumentListItem(document: Document, viewModel: DocumentsListViewModel) {
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
                .offset { IntOffset(swipeableState.offset.value.roundToInt(), 0) },
            text = {
                val title: String =
                    if (document.title == null) stringResource(id = R.string.unnamed_document) else document.title!!
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            },
            trailing = {
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
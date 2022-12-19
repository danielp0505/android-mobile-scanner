package de.thm.ap.mobile_scanner.ui.screens

import android.app.Application
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import de.thm.ap.mobile_scanner.data.AppDatabase
import de.thm.ap.mobile_scanner.data.DocumentDAO
import de.thm.ap.mobile_scanner.model.Document
import de.thm.ap.mobile_scanner.R
import de.thm.ap.mobile_scanner.model.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DocumentsListViewModel(app: Application) : AndroidViewModel(app) {
    private val docDAO: DocumentDAO = AppDatabase.getDb(app.baseContext).documentDao()
    val documents: LiveData<List<Document>> = docDAO.findAllDocumentsSync()

    var contextualMode by mutableStateOf(false)
    var selectedDocuments: MutableList<Document> = mutableStateListOf()

    fun createTestData(){
        viewModelScope.launch(Dispatchers.IO) {
            val rrID: Long = docDAO.persist(Document(title = "Restaurant Receipt"))
            val tfID: Long = docDAO.persist(Document(title = "Tax Forms"))
            val kksfID: Long = docDAO.persist(Document(title = "The Krusty Krab Secret Formula"))

            val fdID: Long = docDAO.persist(Tag(name = "Food & Drink"))
            val finID: Long = docDAO.persist(Tag(name = "Finances"))

            docDAO.persist(rrID, fdID)
            docDAO.persist(rrID, finID)
            docDAO.persist(tfID, finID)
            docDAO.persist(kksfID, fdID)
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
                    ){
                        Icon(imageVector = Icons.Default.Share,
                        contentDescription = stringResource(id = R.string.share_documents))
                    }
                    IconButton(
                        onClick = {
                            vm.createTestData()
                        }
                    ){
                        Icon(imageVector = Icons.Default.Create,
                            contentDescription = "Create Test Data")
                    }
                    IconButton(
                        onClick = {

                        }
                    ){
                        Icon(imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(id = R.string.delete_documents))
                    }
                }
            )
        }
    ){ innerPadding ->
        if(documents.isEmpty()) {
            Text(
                text = stringResource(id = R.string.no_documents_found),
                style = MaterialTheme.typography.h6,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
            )
        } else {
            LazyColumn(contentPadding = innerPadding){
                items(documents){ document ->
                    DocumentListItem(document = document)
                    Divider()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DocumentListItem(document: Document){
    ListItem() {
        val title: String = if (document.title == null) stringResource(id = R.string.unnamed_document) else document.title!!
        Text(text = title,
        maxLines = 1,
        overflow = TextOverflow.Clip)
    }
}
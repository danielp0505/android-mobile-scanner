package de.thm.ap.mobile_scanner.ui.screens

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import de.thm.ap.mobile_scanner.R
import de.thm.ap.mobile_scanner.data.AppDatabase
import de.thm.ap.mobile_scanner.data.DocumentDAO
import de.thm.ap.mobile_scanner.model.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

class TagManagementViewModel(app: Application) : AndroidViewModel(app) {
    private val docDAO: DocumentDAO = AppDatabase.getDb(app.baseContext).documentDao()

    var isEditMode: Boolean by mutableStateOf(false)
    val tags: LiveData<List<Tag>> = docDAO.findAllTagsSync()
    var tagName: String by mutableStateOf(String())
    var selectedTag: Tag by mutableStateOf(Tag())
    var showTagDeleteDialog by mutableStateOf(false)

    fun toggleEditMode(tag: Tag) {
        if (isEditMode && tag == selectedTag) {
            selectedTag = Tag()
            isEditMode = false
        } else {
            isEditMode = true
            selectedTag = tag
            tagName = tag.name.toString()
        }
    }

    fun persistTag(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            docDAO.persist(Tag(name = name))
        }
    }

    fun updateTag(tag: Tag) {
        viewModelScope.launch(Dispatchers.IO) {
            docDAO.update(tag)
        }
    }

    fun deleteTag(tag: Tag) {
        viewModelScope.launch(Dispatchers.IO) {
            docDAO.delete(tag)
            //damit es nach dem Delete nicht im Edit Mode stecken bleibt
            isEditMode=false
        }
    }
    fun updateOrpersist(){
        //wenn Eingabe leer soll kein Tag erstellt werden
        if(tagName!="" && isEditMode) {
            updateTag(Tag(selectedTag.tagId, tagName))
            //Veränderung damit nach dem Aktualisieren das Highlighten des Tags verschwindet
            isEditMode=false

        }  else if(tagName!=""&& !isEditMode) {
            persistTag(tagName)
        }

        //nach erstellen vom Tag wird Eingabefeld wieder geleert
        tagName = ""
    }

}

@Composable
fun TagManagementScreen(dismissTagManager: () -> Unit) {
    val vm: TagManagementViewModel = viewModel()
    val tags by vm.tags.observeAsState(initial = emptyList())
    Scaffold(
        topBar = {
            TopAppBar(title = {
                Text(text = stringResource(id = R.string.manage_tags))
            },
                navigationIcon = {
                    IconButton(onClick = dismissTagManager) {
                        Icon(Icons.Filled.ArrowBack, stringResource(id = R.string.navigation_back))
                    }
                })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            TextField(value = vm.tagName, onValueChange = { vm.tagName = it },

            )
            Button(onClick = {
                vm.updateOrpersist()

            }) {
                Text(text = stringResource(id = if (vm.isEditMode) R.string.update_tag else R.string.create_tag))
            }
            val lazyListState = rememberLazyListState()
            LazyColumn(contentPadding = innerPadding, state = lazyListState) {
                items(
                    items = tags,
                    key = { it.tagId!! }) { tag ->
                    TagListItem(tag = tag, selectedTag = vm.selectedTag, onSelection = {
                            selectedTag -> vm.toggleEditMode(selectedTag) },
                        onDelete = {selectedTag -> vm.deleteTag(selectedTag)

                        })
                    Divider(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
fun TagListItem(tag: Tag, selectedTag: Tag, onSelection: (tag: Tag) -> Unit, onDelete: (tag: Tag) -> Unit){
    val vm: TagManagementViewModel = viewModel()
    val elementPadding = 12.dp
    val rowBaseModifier = Modifier.fillMaxWidth()
    Row(horizontalArrangement = Arrangement.SpaceBetween,
        //damit nach dem löschen eines Tags das highlighten verschwindet
        modifier = if (tag.tagId == selectedTag.tagId && vm.isEditMode) rowBaseModifier.background(Color.Gray) else rowBaseModifier) {
        Button(onClick = { onSelection(tag) }, Modifier.padding(elementPadding).fillMaxWidth(0.8f)) {
            Text(text = tag.name ?: stringResource(id = R.string.unknown))
        }

        //if(vm.isEditMode){
        IconButton(onClick = { vm.showTagDeleteDialog=true }, modifier = Modifier.padding(elementPadding)) {
            Icon(imageVector = Icons.Filled.Delete, contentDescription = stringResource(id = R.string.delete) )
        }
        //}

        //Der Dialog beim drücken auf den Delete Knopf
        if(vm.showTagDeleteDialog){
            AlertDialog(
                onDismissRequest = {vm.showTagDeleteDialog=false},

                title = {
                    Text(text = "Löschen")
                },
                text = {
                    Text(
                        "Tag löschen?"
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDelete(tag)
                            vm.showTagDeleteDialog=false


                        }
                    ) {
                        Text("Ja")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {vm.showTagDeleteDialog=false}

                    ) {
                        Text("Nein")
                    }
                }
            )
        }
    }


}

package de.thm.ap.mobile_scanner.ui.screens

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavArgument
import androidx.navigation.NavController
import coil.compose.AsyncImage
import de.thm.ap.mobile_scanner.R
import de.thm.ap.mobile_scanner.data.AppDatabase
import de.thm.ap.mobile_scanner.data.DocumentDAO
import de.thm.ap.mobile_scanner.model.Document
import de.thm.ap.mobile_scanner.model.DocumentImageRelation
import de.thm.ap.mobile_scanner.model.Tag
import de.thm.ap.mobile_scanner.model.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.*


class DocumentEditScreenViewModel(app: Application, val initDocumentId: Long?) : AndroidViewModel(app) {
  val dao = AppDatabase.getDb(app).documentDao()
  val tags: LiveData<List<Tag>> = dao.findAllTagsSync()
  var documentName: String by mutableStateOf("")
  val images: MutableList<Uri> = mutableStateListOf()
  var selectedTags: MutableList<Tag> = mutableStateListOf()
  var isEditMode: Boolean = false

  fun saveDocument() {
    val documentName = if (documentName.isNullOrEmpty()) null else documentName

    if(isEditMode){
      viewModelScope.launch{
        dao.update(Document(documentId = initDocumentId, title = documentName))
      }
    } else {
      viewModelScope.launch(Dispatchers.IO){
        val documentId = dao.persist(Document(title = documentName))
        selectedTags.forEach { dao.persist(documentId, it.tagId!!) }
        var imageIDs: List<Long> = emptyList()
        imageIDs = images.map {dao.persist(Image(uri = it.toString()))} //List is correct in DB
        //todo: Image relations do not get saved correctly starting here
        imageIDs.forEach { dao.persist(DocumentImageRelation(documentId = documentId, imageId = it)) }
      }
    }
  }

  companion object{
    fun createEditDocumentFactory(initDocumentId: Long) = viewModelFactory{
      initializer {
        val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
        DocumentEditScreenViewModel(
          app = app!!,
          initDocumentId = initDocumentId
        )
      }
    }

    fun createNewDocumentFactory() = viewModelFactory {
      initializer {
        val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
        DocumentEditScreenViewModel(
          app = app!!,
          initDocumentId = null
        )
      }
    }
  }

  init {
      if(initDocumentId != null && initDocumentId != -1L){
        isEditMode = true
        viewModelScope.launch{
          val documentWithTags: DocumentDAO.DocumentWithTags =
            dao.getDocumentWithTags(initDocumentId)
          documentName = documentWithTags.document.title?: ""
          documentWithTags.tags.forEach { selectedTags.add(it) }
          val documentWithImages: DocumentDAO.DocumentWithImages =
            dao.getDocumentWithImages(initDocumentId)
          documentWithImages.images.forEach{ it.uri?.let { uriString: String ->
            images.add(uriString.toUri()) } }
        }
      }
  }
}

private fun takePicture(context: Context, uri: Uri) {
  Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
    addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    putExtra(
      MediaStore.EXTRA_OUTPUT,
      uri
    )
  }.also {
    context.startActivity(it)
  }

}

@Composable
fun DropDownItemMenuWithCheckbox(
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit
) {
  DropdownMenuItem(
    onClick = { onClick() }, modifier = modifier
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Checkbox(checked = selected, onCheckedChange = { onClick() })
      content()
    }
  }

}

@Composable
fun DocumentEditScreen(
  navController: NavController
) {
  val vm: DocumentEditScreenViewModel = viewModel()
  val tags by vm.tags.observeAsState()

  var tagsExpanded by remember { mutableStateOf(false) }

  Scaffold(
    modifier = Modifier.fillMaxWidth(),
    topBar = { TopAppBar(title = { Text(text = stringResource(id =
    if(vm.isEditMode) R.string.edit_document else R.string.create_document))
    })},
    content = { padding ->

      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
      ) {
        ImageArea(vm.images)
        OutlinedTextField(
          value = vm.documentName,
          onValueChange = { vm.documentName = it },
          singleLine = true,
          label = {
            Text(text = stringResource(id = R.string.document_title))
          },
          modifier = Modifier
            .padding(padding)
        )
        Box {
          OutlinedTextField(
            modifier = Modifier.clickable { tagsExpanded = true },
            enabled = false,
            value = vm.selectedTags.mapNotNull { it.name }.sorted().joinToString(),
            onValueChange = {},
            label = {
              Text(text = "Tags")
            },
            trailingIcon = {
              IconButton(onClick = { tagsExpanded = true }) {
                Icon(
                  imageVector = Icons.Default.ArrowDropDown,
                  contentDescription = stringResource(id = R.string.select_tags)
                )
              }
            }

          )
          DropdownMenu(expanded = tagsExpanded, onDismissRequest = { tagsExpanded = false }) {
            tags?.filter { it.name != null }?.forEach { tag ->
              DropDownItemMenuWithCheckbox(
                modifier = Modifier.padding(padding),
                selected = vm.selectedTags.contains(tag),
                onClick = {
//                  vm.selectedTags = vm.selectedTags.also { if (it.contains(tag)) it.remove(tag) else it.add(tag) }
                  vm.selectedTags.apply { if (contains(tag)) remove(tag) else add(tag) }
                }) {
                Text(text = tag.name!!)
              }
            }
          }
        }

      }


    },
    floatingActionButton = {

      FloatingActionButton(
        onClick = {
          vm.saveDocument()
          navController.navigateUp()
        },
      ) {
        Icon(
          imageVector = Icons.Default.Check,
          contentDescription = stringResource(id =
          if (vm.isEditMode) R.string.save_document else R.string.update_document)
        )
      }
    }
  )


}

@Composable
private fun ImageArea(
  images: MutableList<Uri>,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val uuid by remember { mutableStateOf(UUID.randomUUID()) }
  val baseDir = File(context.filesDir, uuid.toString())
  baseDir.mkdirs()
  Box(
    modifier = modifier
      .fillMaxWidth(1F)
      .fillMaxHeight(.3F)
  ) {
    LazyRow(modifier = Modifier.padding(16.dp)) {
      items(images.filter { uri -> File(baseDir, uri.lastPathSegment).exists()}) { uri ->
        AsyncImage(
          modifier = Modifier.padding(16.dp),
          model = uri,
          contentDescription = "Page ${images.indexOf(uri)}",
        )
      }
    }
    FloatingActionButton(
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(16.dp),
      onClick = {
        val file = File(baseDir, "${images.size}.jpg")
        val uri = FileProvider.getUriForFile(context, "de.thm.fileprovider", file)
        takePicture(context, uri)
        images.add(uri)
      },
    ) {
      Icon(
        imageVector = ImageVector.vectorResource(R.drawable.add_a_photo),
        contentDescription = "Add Picture",
      )
    }
  }
}






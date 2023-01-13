package de.thm.ap.mobile_scanner.ui.screens

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.annotation.CallSuper
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import de.thm.ap.mobile_scanner.R
import de.thm.ap.mobile_scanner.data.AppDatabase
import de.thm.ap.mobile_scanner.model.Document
import de.thm.ap.mobile_scanner.model.DocumentImageRelation
import de.thm.ap.mobile_scanner.model.Tag
import de.thm.ap.mobile_scanner.model.Image
import kotlinx.coroutines.launch
import java.io.File
import java.util.*


class DocumentEditScreenViewModel(app: Application) : AndroidViewModel(app) {
  val dao = AppDatabase.getDb(app).documentDao()
  var document: Document by mutableStateOf(Document())
  fun isEditMode() = document.documentId!=null
  val tags: LiveData<List<Tag>> = dao.findAllTagsSync()
  var images = mutableStateListOf<Uri>()
  var selectedTags: MutableList<Tag> = mutableStateListOf()

  fun initDocument(documentId: Long?){
    if (documentId == null) return
    viewModelScope.launch {
      document = dao.findDocumentById(documentId)
      images = dao.getDocumentWithImages(documentId).images.map { it.uri!!.toUri() }.toMutableStateList()
    }
  }

  fun saveDocument() {
    if (isEditMode()){
      viewModelScope.launch {
        dao.update(document)
        selectedTags.forEach { dao.persist(document.documentId!!, it.tagId!!) }

        //delete all images assosiated with this document and recreate them.
        dao.getDocumentWithImages(document.documentId!!).images.forEach {
          dao.delete(it)
          dao.delete(DocumentImageRelation(document.documentId!!, it.imageId!!))
        }
        val images_ids = images.map { dao.persist(Image(uri = it.toString())) }
        images_ids.forEach { dao.persist(DocumentImageRelation(document.documentId!!, it)) }
      }
    }else {
      viewModelScope.launch {
        val documentId = dao.persist(document)
        selectedTags.forEach { dao.persist(documentId, it.tagId!!) }
        val images_ids = images.map { dao.persist(Image(uri = it.toString())) }
        images_ids.forEach { dao.persist(DocumentImageRelation(documentId, it)) }
      }
    }
  }
}

open class TakePicture : ActivityResultContract<Uri, Uri?>() {
  var uri: Uri? = null

  @CallSuper
  override fun createIntent(context: Context, input: Uri): Intent {
    uri = input
    return Intent(MediaStore.ACTION_IMAGE_CAPTURE)
      .putExtra(MediaStore.EXTRA_OUTPUT, input)
  }

  final override fun getSynchronousResult(
    context: Context,
    input: Uri
  ): SynchronousResult<Uri?>? = null

  @Suppress("AutoBoxing")
  final override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
    return if (resultCode == Activity.RESULT_OK) uri else null
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
    navController: NavController,
    documentId: Long?,
) {
  val vm: DocumentEditScreenViewModel = viewModel()
  vm.initDocument(documentId)
  val tags by vm.tags.observeAsState()

  var tagsExpanded by remember { mutableStateOf(false) }

  Scaffold(
    modifier = Modifier.fillMaxWidth(),
    topBar = { TopAppBar(title = { Text(text = stringResource(id = if(vm.isEditMode()) R.string.edit_document else R.string.create_document)) }) },
    content = { padding ->

      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .fillMaxWidth()
          .padding(padding)
      ) {
        ImageArea(vm.images)
        OutlinedTextField(
          value = vm.document.title ?: "",
          onValueChange = { vm.document = vm.document.copy(title = it)},
          singleLine = true,
          label = {
            Text(text = stringResource(id = R.string.document_title))
          },
        )
        Box {
          OutlinedTextField(
            modifier = Modifier.clickable { tagsExpanded = true },
            enabled = false,
            value = vm.selectedTags.mapNotNull { it.name }.sorted().joinToString(),
            onValueChange = {},
            label = {
              Text(text = stringResource(id = R.string.tags))
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
          contentDescription = stringResource(id = R.string.save)
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
  val vm: DocumentEditScreenViewModel = viewModel()
  val getImageFromGalleryLauncher = rememberLauncherForActivityResult(contract = GetContent()) {
    if (it != null) images.add(it)
  }
  val getImageFromCameraLauncher = rememberLauncherForActivityResult(contract = TakePicture()) {
    if (it != null) images.add(it)
  }
  baseDir.mkdirs()
  Box(
    modifier = modifier
      .fillMaxWidth(1F)
      .fillMaxHeight(.3F)
  ) {
    LazyRow(modifier = Modifier.padding(16.dp)) {
      itemsIndexed(images) { index, uri ->
        Box() {
          AsyncImage(
            modifier = Modifier.padding(16.dp),
            model = uri,
            contentDescription = stringResource(id = R.string.page) + " ${images.indexOf(uri)}",
          )
          IconButton(
            onClick = {vm.images.remove(uri)},
            modifier = Modifier
              .size(32.dp)
              .align(Alignment.TopEnd)
          ) {
            Icon(
              Icons.Default.Delete,
              stringResource(id = R.string.delete_image)
            )
          }
        }
      }
    }
    Column(
      modifier = Modifier
        .padding(16.dp)
        .align(Alignment.BottomEnd)
    ) {

      FloatingActionButton(
        modifier = Modifier
          .scale(0.8f)
          .alpha(0.8f),
        elevation = FloatingActionButtonDefaults.elevation(),
        onClick = { getImageFromGalleryLauncher.launch("image/*") },
      ) {
        Icon(
          imageVector = ImageVector.vectorResource(R.drawable.add_photo_alternate),
          contentDescription = stringResource(id = R.string.add_image),
        )
      }
      FloatingActionButton(
        onClick = {
          val file = File(baseDir, "${images.size}.jpg")
          val uri = FileProvider.getUriForFile(context, "de.thm.fileprovider", file)
          getImageFromCameraLauncher.launch(uri)
        },
      ) {
        Icon(
          imageVector = ImageVector.vectorResource(R.drawable.add_a_photo),
          contentDescription = stringResource(id = R.string.take_photo),
        )
      }
    }
  }
}


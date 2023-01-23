package de.thm.ap.mobile_scanner.ui.screens

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.annotation.CallSuper
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import de.thm.ap.mobile_scanner.R
import de.thm.ap.mobile_scanner.data.AppDatabase
import de.thm.ap.mobile_scanner.data.ReferenceCollection
import de.thm.ap.mobile_scanner.model.Document
import de.thm.ap.mobile_scanner.model.DocumentImageRelation
import de.thm.ap.mobile_scanner.model.Image
import de.thm.ap.mobile_scanner.model.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.roundToInt


class DocumentEditScreenViewModel(app: Application) : AndroidViewModel(app) {
    val dao = AppDatabase.getDb(app).documentDao()
    var document: Document by mutableStateOf(Document())
    var isEditMode by mutableStateOf(false)
    val tags: LiveData<List<Tag>> = dao.findAllTagsSync()
    var images = mutableStateListOf<Uri>()
    var selectedTags: MutableList<Tag> = mutableStateListOf()

    val storage: StorageReference? =
        FirebaseAuth.getInstance().currentUser?.let {
            Firebase.storage
                .getReference(it.uid)
        }

    /*
    Local Init
    fun initDocument(documentId: Long?) {
        if (documentId == null) return
        viewModelScope.launch {
            document = dao.findDocumentById(documentId)
            images = dao.getDocumentWithImages(documentId).images.map { it.uri!!.toUri() }
                .toMutableStateList()
        }
    }
    */
    //this variable is necessary to avoid a race condition
    var uninitialized = true
    fun initDocument(documentUID: String?) {
        if (documentUID != null && images.isEmpty() && uninitialized) {
            uninitialized = false
            isEditMode = true
            ReferenceCollection.userDocReference
                ?.collection("documents")
                ?.document(documentUID)
                ?.get()?.addOnSuccessListener { documentSnapshot ->
                    document = Document(uri = documentSnapshot.reference.id)
                    val title = documentSnapshot.get("title")
                    if (title is String) document =
                        Document(title = title, uri = documentSnapshot.reference.id)
                    val tags = documentSnapshot.get("tags")

                    var i: Long = 0
                    val tagList: MutableList<Tag> =
                        if (tags is List<*>) tags.map { tag ->
                            Tag(
                                i++,
                                tag.toString()
                            )
                        } as MutableList<Tag>
                        else mutableListOf()
                    if (tagList.isNotEmpty()) selectedTags = tagList

                    storage?.child(documentSnapshot.reference.id)
                        ?.listAll()?.addOnSuccessListener { listResult ->
                            val temporaryList: MutableList<Uri> = mutableListOf()
                            listResult.items.forEach { storageReference ->
                                storageReference.downloadUrl.addOnSuccessListener { uri ->
                                    //temporary list to sort by uri name
                                    temporaryList.add(uri)
                                    if(temporaryList.size == listResult.items.size){
                                        temporaryList.sort()
                                        temporaryList.forEach{imageUri -> images.add(imageUri)}
                                    }
                                }
                            }
                        }
                }
        }
    }
    
    fun saveDocument() {
        if (isEditMode) {
            //Id is stored in uri
            document.uri?.let { id ->
                val folderRef = storage?.child(id)
                images.forEachIndexed{index, image ->
                    folderRef?.child(index.toString())?.putFile(image)
                }
                var updatedDoc: HashMap<String, Any?>?
                if (tags.value?.isEmpty() == true) {
                    updatedDoc = hashMapOf(
                        "title" to document.title
                    )
                } else {
                    val tagList: List<String?> = tags.value!!.map { tag: Tag -> tag.name }
                    updatedDoc = hashMapOf(
                        "title" to document.title,
                        "tags" to tagList
                    )
                }
                ReferenceCollection.userDocReference
                    ?.collection("documents")
                    ?.document(id)
                    ?.set(updatedDoc)
            }
            /* Local Update
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
         */
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                var newDoc: HashMap<String, Any?>?
                if (tags.value?.isEmpty() == true) {
                    newDoc = hashMapOf(
                        "title" to document.title
                    )
                } else {
                    val tagList: List<String?> = tags.value!!.map { tag: Tag -> tag.name }
                    newDoc = hashMapOf(
                        "title" to document.title,
                        "tags" to tagList
                    )
                }
                ReferenceCollection.userDocReference
                    ?.collection("documents")
                    ?.add(newDoc)
                    ?.addOnSuccessListener { documentReference ->
                        if (documentReference != null) {
                            val ref = storage?.child(documentReference.id)
                            var i: Long = 0
                            images.forEach { uri ->
                                ref?.child(i.toString())?.putFile(uri)
                                i++
                            }
                        }
                    }
                /* Local Persist
                val documentId = dao.persist(document)
                selectedTags.forEach { dao.persist(documentId, it.tagId!!) }
                val images_ids = images.map { dao.persist(Image(uri = it.toString())) }
                images_ids.forEach { dao.persist(DocumentImageRelation(documentId, it)) }
                 */
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
    //documentId: Long?,
    documentUID: String?
) {
    val vm: DocumentEditScreenViewModel = viewModel()
    vm.initDocument(documentUID)

    val tags by vm.tags.observeAsState()

    var tagsExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxWidth(),
        topBar = {
            TopAppBar(navigationIcon = {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Go back"
                    )
                }
            },
                title = { Text(text = stringResource(id = if (vm.isEditMode) R.string.edit_document else R.string.create_document)) })
        },
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
                    onValueChange = { vm.document = vm.document.copy(title = it) },
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
                    DropdownMenu(
                        expanded = tagsExpanded,
                        onDismissRequest = { tagsExpanded = false }) {
                        tags?.filter { it.name != null }?.forEach { tag ->
                            DropDownItemMenuWithCheckbox(
                                modifier = Modifier.padding(padding),
                                selected = vm.selectedTags.contains(tag),
                                onClick = {
//                  vm.selectedTags = vm.selectedTags.also { if (it.contains(tag)) it.remove(tag) else it.add(tag) }
                                    vm.selectedTags.apply {
                                        if (contains(tag)) remove(tag) else add(
                                            tag
                                        )
                                    }
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
fun ImageBox(
  uri: Uri?,
  contentDescription: String,
  onRelease: (Float, Float) -> Unit,
  onGloballyPositioned: (LayoutCoordinates) -> Unit,
) {
  val vm: DocumentEditScreenViewModel = viewModel()
  var offsetX by remember { mutableStateOf(0f) }
  var offsetY by remember { mutableStateOf(0f) }
  var isDraged by remember { mutableStateOf(false) }
  Box(modifier = Modifier
      .zIndex(if (isDraged) Float.MAX_VALUE else 0f)
      .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
      .onGloballyPositioned {
          onGloballyPositioned(it)
      }

  ) {
    AsyncImage(
      modifier = Modifier
          .padding(16.dp)
          .shadow(if (isDraged) 16.dp else 0.dp)
          .scale(if (isDraged) .95f else 1f)
          .alpha(if (isDraged) .7f else 1f)
          .pointerInput(Unit) {
              detectDragGesturesAfterLongPress(onDragStart = { _ ->
                  isDraged = true
              }, onDrag = { change, dragAmount ->
                  change.consume()
                  offsetX += dragAmount.x
                  offsetY += dragAmount.y
              }, onDragEnd = {
                  isDraged = false
                  onRelease(offsetX, offsetY)
                  offsetX = 0f
                  offsetY = 0f
              })
          },
      model = uri,
      contentDescription = contentDescription,
    )
    if (!isDraged) {
      IconButton(
        onClick = { vm.images.remove(uri) }, modifier = Modifier
              .size(32.dp)
              .align(Alignment.TopEnd)
      ) {
        Icon(
          Icons.Default.Delete, "Delete Image"
        )
      }
    }
  }
}

fun calcStepsToMove(index: Int, offset: Float, widths: List<Int>): Int {
  var stepsToMove = 0
  var remainingOffset = offset
  for (i in 0..widths.lastIndex) {
    when {
      //steps forward
      offset > 0 && i in index..widths.lastIndex -> {
        remainingOffset = remainingOffset - widths[i]
        if (remainingOffset > 0) {
          stepsToMove++
        }
      }
      //steps backward
      offset < 0 && i in 0..index -> {
        remainingOffset = remainingOffset + widths[i]
        if (remainingOffset < 0) {
          stepsToMove--
        }
      }
    }
  }
  return stepsToMove
}

@Composable
private fun ImageArea(
    images: MutableList<Uri>,
    modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val uuid by remember { mutableStateOf(UUID.randomUUID()) }
  val baseDir = File(context.filesDir, uuid.toString())
  var imagePositions = remember{arrayOfNulls<LayoutCoordinates>(100) }
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
              ImageBox(uri, "Page Nr. ${index}", onRelease = { x, y ->
                  val stepsToMoove = calcStepsToMove(index,
                      x,
                      imagePositions.slice(0..images.lastIndex)
                          .map { it?.size?.width ?: Int.MAX_VALUE })
                  val uri = images[index]
                  images.removeAt(index)
                  images.add(index + stepsToMoove, uri)
              }, onGloballyPositioned = { imagePositions[index] = it })
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


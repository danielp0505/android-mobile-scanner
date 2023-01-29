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
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import de.thm.ap.mobile_scanner.R
import de.thm.ap.mobile_scanner.data.ReferenceCollection
import de.thm.ap.mobile_scanner.data.forEachFirebaseImage
import de.thm.ap.mobile_scanner.data.runWithDocumentSnapshot
import de.thm.ap.mobile_scanner.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import kotlin.math.roundToInt


class DocumentEditScreenViewModel(app: Application) : AndroidViewModel(app) {

    var document: Document by mutableStateOf(Document())
    var isEditMode by mutableStateOf(false)
    var images = mutableStateListOf<Image>()
    var documentTags = mutableStateListOf<String>()

    var editTag: String by mutableStateOf(String())
    var replacementTag: String by mutableStateOf(String())
    var showTagUpdateDialog: Boolean by mutableStateOf(false)
    var newTag: String by mutableStateOf(String())

    val firebaseStorage: StorageReference? =
        FirebaseAuth.getInstance().currentUser?.let {
            Firebase.storage
                .getReference(it.uid)
        }

    //variable stores image UIDs present at initialization when editing a document
    var initialImageUUIDs: MutableList<UUID> = mutableListOf()

    //this variable is necessary to avoid a race condition
    var initialized = false
    fun initDocument(documentUID: String?) {
        if (documentUID == null) return
        if (images.isNotEmpty()) return
        if (initialized) return

        initialized = true
        isEditMode = true
        runWithDocumentSnapshot(documentUID){ documentSnapshot ->
            val title = documentSnapshot.get("title")
            document =
                when(title) {
                    is String ->
                        Document(title = title as String?, uri = documentSnapshot.reference.id)
                    else -> Document(uri = documentSnapshot.reference.id)
                }

            when (val tags = documentSnapshot.get("tags")) {
                is List<*> -> tags.forEach { documentTags.add(it.toString()) }
            }

            viewModelScope.launch {
                forEachFirebaseImage(documentSnapshot.reference.id) {
                    it.uuid?.let { uuid -> initialImageUUIDs.add(uuid) }
                    images.add(it)
                }

            }
        }
    }

    fun updateDocument(){
        //Id is stored in uri
        if(document.uri == null) return

        val folderRef = firebaseStorage

        //Image Upload
        val image_uuids = images.map{image ->
            if(image.uuid ==null){
                // Datei noch nicht im Firestore, muss hochladen
                image.uuid = UUID.randomUUID()
                folderRef?.child(image.uuid.toString())?.putFile(image.uri!!.toUri())
            }
            image.uuid
        }

        val unusedImages: MutableList<UUID?> = mutableListOf()
        unusedImages.addAll(initialImageUUIDs)
        unusedImages.removeAll(image_uuids)
        unusedImages.forEach { uuid ->
            folderRef?.child(uuid.toString())?.delete()
        }

        var updatedDoc: HashMap<String, Any?> = hashMapOf()
        if (!document.title.isNullOrEmpty()) {
            updatedDoc["title"] = document.title
        }
        if (documentTags.isNotEmpty()) {
            updatedDoc ["tags"] = documentTags as List<String>
        }
        if (image_uuids.isNotEmpty()) {
            updatedDoc ["images"] = image_uuids.map{it.toString()}
        }

        ReferenceCollection.userDocReference
            ?.collection("documents")
            ?.document(document.uri!!)
            ?.set(updatedDoc)
    }
    fun saveDocument() {
        if (images.isEmpty()) return
        if (isEditMode) return updateDocument()

        viewModelScope.launch(Dispatchers.IO) {
            var newDoc: HashMap<String, Any?> = hashMapOf()
            if (!documentTags.isEmpty()){
                newDoc["tags"] = documentTags as List<String>
            }
            if (!document.title.isNullOrEmpty()){
                newDoc["title"] = document.title
            }
            newDoc["images"] = images.map {
                val uuid = UUID.randomUUID().toString()
                firebaseStorage?.child(uuid)?.putFile(it.uri!!.toUri())
                uuid
            }
            ReferenceCollection.userDocReference
                ?.collection("documents")
                ?.document()
                ?.set(newDoc)
        }
    }

    fun addNewTag(){
        if (newTag.isEmpty()) return

        documentTags.add(newTag)
        newTag = ""
    }

    fun updateTag(){
        if (replacementTag.isEmpty() || replacementTag == editTag) return

        val originalIndex = documentTags.indexOf(editTag)
        documentTags[originalIndex] = replacementTag
        editTag = ""
        replacementTag = ""
        showTagUpdateDialog = false
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


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DocumentEditScreen(
    navController: NavController,
    documentUID: String?
) {
    val vm: DocumentEditScreenViewModel = viewModel()
    val tags = vm.documentTags
    val keyboardController = LocalSoftwareKeyboardController.current
    vm.initDocument(documentUID)
    val scaffoldState: ScaffoldState = rememberScaffoldState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.fillMaxWidth(),
        scaffoldState = scaffoldState,
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

                Divider(modifier = Modifier.padding(8.dp))

                OutlinedTextField(
                    keyboardOptions= KeyboardOptions(
                        keyboardType= KeyboardType.Email,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions =  KeyboardActions(
                        onDone = {keyboardController?.hide()}),
                    value = vm.document.title ?: "",
                    onValueChange = { vm.document = vm.document.copy(title = it) },
                    singleLine = true,
                    label = {
                        Text(text = stringResource(id = R.string.document_title))
                    })

                    Spacer(modifier = Modifier.padding(12.dp))

                fun addNewTag(){
                    if(vm.newTag.isNotEmpty()) vm.addNewTag()
                    else scope.launch(Dispatchers.Default) {
                        val snackbarResult = scaffoldState.snackbarHostState.showSnackbar(
                            message = context.getString(R.string.empty_tag_field_error)
                        )
                    }
                }

                if(vm.showTagUpdateDialog) UpdateTagDialog()
                OutlinedTextField(
                    keyboardOptions= KeyboardOptions(
                        keyboardType= KeyboardType.Email,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions =  KeyboardActions(
                        onDone = {
                            addNewTag()
                            keyboardController?.hide()
                        }),
                    value = vm.newTag,
                    onValueChange = { vm.newTag = it },
                    label = {
                        Text(text = stringResource(id = R.string.new_tag))
                    }
                )
                Button(onClick = {
                    addNewTag()
                }) {
                    Text(text = stringResource(id = R.string.add_tag))
                }


                val lazyListState = rememberLazyListState()
                LazyColumn(state = lazyListState) {
                    items(
                        items = tags,
                        key = { it }) { tag ->
                        TagListItem(tag = tag,
                            onSelection = {
                                vm.editTag = it
                                vm.replacementTag = it
                                vm.showTagUpdateDialog = true
                            },
                            onDelete = { vm.documentTags.remove(it)})
                        Divider(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        floatingActionButton = {

            FloatingActionButton(
                onClick = {
                    if(!vm.images.isEmpty()) {
                        vm.saveDocument()
                        navController.navigateUp()
                    }
                    else {
                        scope.launch(Dispatchers.Default) {
                            val snackbarResult = scaffoldState.snackbarHostState.showSnackbar(
                                message = context.getString(R.string.empty_document_error)
                            )
                        }
                    }
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
fun UpdateTagDialog(){
    val vm: DocumentEditScreenViewModel = viewModel()

    AlertDialog(onDismissRequest = { vm.showTagUpdateDialog = false },
    title = {Text(text = stringResource(id = R.string.update_tag))},
    text = {
        OutlinedTextField(
            keyboardOptions= KeyboardOptions(
                keyboardType= KeyboardType.Email,
                imeAction = ImeAction.Done
            ),
            keyboardActions =  KeyboardActions {
                vm.updateTag()
            },
            value = vm.replacementTag,
            onValueChange = { vm.replacementTag = it },
            label = {
                Text(text = stringResource(id = R.string.previous_name) + ": " + vm.editTag)
            }
        )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    vm.updateTag()
                }
            ) { Text(stringResource(id = R.string.update_tag)) }
        },
        dismissButton = {
            TextButton(
                onClick = { vm.showTagUpdateDialog = false }
            ) { Text(stringResource(id = R.string.cancel)) }
        })
}


@Composable
fun ImageBox(
  image: Image,
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
      model = image!!.uri,
      contentDescription = contentDescription,
    )
    if (!isDraged) {
      IconButton(
        onClick = { vm.images.remove(image) }, modifier = Modifier
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

private fun InputStream.copyTo(dest: OutputStream) {
    val buf = ByteArray(8192)
    var length: Int
    while (this.read(buf).also { length = it } != -1) {
        dest.write(buf, 0, length)
    }

}

private fun Uri.copyTo(context: Context, dest: Uri) {
    val contentResolver = context.contentResolver
    val source_stream = contentResolver.openInputStream(this)
    val dest_stream = contentResolver.openOutputStream(dest)
    if (source_stream == null || dest_stream == null) return
    source_stream.copyTo(dest_stream, 8192)
}

@Composable
private fun ImageArea(
    images: MutableList<Image>,
    modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
    val vm: DocumentEditScreenViewModel = viewModel()
    val baseDir = File(context.filesDir, UUID.randomUUID().toString())
    var imagePositions = remember { arrayOfNulls<LayoutCoordinates>(100) }
    val getImageFromGalleryLauncher = rememberLauncherForActivityResult(contract = GetContent()) {
        if (it == null) return@rememberLauncherForActivityResult

        val file = File(baseDir, "${images.size}.jpg")
        val uri = FileProvider.getUriForFile(context, "de.thm.fileprovider", file)
        it.copyTo(context, uri)
        images.add(Image(uri = uri.toString()))
    }
    val getImageFromCameraLauncher = rememberLauncherForActivityResult(contract = TakePicture()) {
        if (it != null) images.add(Image(uri = it.toString()))
  }
  baseDir.mkdirs()
  Box(
    modifier = modifier
        .fillMaxWidth(1F)
        .fillMaxHeight(.3F)
  ) {
      LazyRow(modifier = Modifier.padding(16.dp)) {
          itemsIndexed(images) { index, image ->
              ImageBox(image, "Page Nr. ${index}", onRelease = { x, y ->
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


@Composable
fun TagListItem(tag: String, onSelection: (tag: String) -> Unit, onDelete: (tag: String) -> Unit){
    val elementPadding = 12.dp
    val rowBaseModifier = Modifier.fillMaxWidth()
    Row(horizontalArrangement = Arrangement.SpaceBetween,
        modifier = rowBaseModifier) {
        Button(onClick = { onSelection(tag) },
            Modifier
                .padding(elementPadding)
                .fillMaxWidth(0.8f)) {
            Text(text = tag)
        }

        IconButton(onClick = { onDelete(tag) }, modifier = Modifier.padding(elementPadding)) {
            Icon(imageVector = Icons.Filled.Delete, contentDescription = stringResource(id = R.string.delete) )
        }
    }
}

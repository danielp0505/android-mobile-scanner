package de.thm.ap.mobile_scanner.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import coil.compose.AsyncImage
import de.thm.ap.mobile_scanner.R
import java.io.File
import java.util.*

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
  navController: NavController,
) {
  var docName by remember { mutableStateOf("") }
  val docTags = remember { mutableStateListOf<String>() }
  val images = remember { mutableStateListOf<Uri>() }
  val availableTags = arrayOf("Tag 1", "Tag 2", "Tag 3", "Tag 4")
  var tagsExpanded by remember { mutableStateOf(false) }

  Scaffold(
    modifier = Modifier.fillMaxWidth(),
    topBar = { TopAppBar(title = { Text("Create Document") }) },
    content = { padding ->

      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
      ) {
        ImageArea(images)
        OutlinedTextField(
          value = docName,
          onValueChange = { docName = it },
          singleLine = true,
          label = {
            Text(text = "Document Title")
          },
          modifier = Modifier
            .padding(padding)
        )
        Box {
          OutlinedTextField(
            modifier = Modifier.clickable { tagsExpanded = true },
            enabled = false,
            value = docTags.sorted().joinToString(),
            onValueChange = {},
            label = {
              Text(text = "Tags")
            },
            trailingIcon = {
              IconButton(onClick = { tagsExpanded = true }) {
                Icon(
                  imageVector = Icons.Default.ArrowDropDown,
                  contentDescription = "Select Tags"
                )
              }
            }

          )
          DropdownMenu(expanded = tagsExpanded, onDismissRequest = { tagsExpanded = false }) {
            availableTags.forEach { tag ->
              DropDownItemMenuWithCheckbox(
                modifier = Modifier.padding(padding),
                selected = docTags.contains(tag),
                onClick = {
                  docTags.apply { if (contains(tag)) remove(tag) else add(tag) }
                }) {
                Text(text = tag)
              }
            }
          }
        }

      }


    },
    floatingActionButton = {

      FloatingActionButton(
        onClick = {
          navController.navigateUp()
        },
      ) {
        Icon(
          imageVector = Icons.Default.Check,
          contentDescription = "Save Document"
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






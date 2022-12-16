package de.thm.ap.mobile_scanner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

class DocumentOverviewScreen {


  @Composable
  fun MainScreen(onAddScanClick: () -> Unit) {

    Scaffold(
      topBar = {
        TopAppBar(
          title = { Text("Document List") },
          actions = {
            IconButton(onClick = { TODO() }) {
              Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search Scan"
              )
            }
          }
        )

      },
      content = { padding ->
        LazyColumn(
          modifier = Modifier
            .padding(padding)
            .background(color = Color.LightGray)
            .fillMaxSize()
        ) {

        }
      },

      floatingActionButton = {
        FloatingActionButton(
          onClick = { onAddScanClick() },
          )
        {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "New Scan"
          )
        }

      }

    )
  }

}

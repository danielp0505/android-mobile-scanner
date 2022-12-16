package de.thm.ap.mobile_scanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import de.thm.ap.mobile_scanner.ui.theme.MobilescannerTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      MobilescannerTheme {
        // A surface container using the 'background' color from the theme
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colors.background
        ) {
          val navController = rememberNavController()
          NavHost(navController = navController, startDestination = "ScanListMain") {
            composable("ScanListMain") {
              DocumentOverviewScreen().MainScreen(
                onAddScanClick = { navController.navigate("DocumentScreen") },

                )
            }
            composable("DocumentScreen") {
              DocumentEditScreen().DocumentEditScreen(navController)
            }

          }
        }
      }
    }
  }
}

@Composable
fun Greeting(name: String) {
  Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
  MobilescannerTheme {
    Greeting("Android")
  }
}
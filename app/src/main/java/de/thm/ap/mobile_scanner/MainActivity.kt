package de.thm.ap.mobile_scanner

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth
import de.thm.ap.mobile_scanner.ui.screens.DocumentEditScreen
import de.thm.ap.mobile_scanner.ui.screens.DocumentsListScreen
import de.thm.ap.mobile_scanner.ui.screens.TagManagementScreen
import de.thm.ap.mobile_scanner.ui.theme.MobilescannerTheme


class MainActivity : ComponentActivity() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val signInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract()
    ) { result: FirebaseAuthUIAuthenticationResult? ->
        Log.d("AUTH", "USER: " + auth.currentUser)
    }

     private fun startSignIn() {
         Log.d("AUTH", "LOGIN START")
        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setTheme(R.style.Theme_Mobilescanner)
            .setAvailableProviders(
                listOf(
                    AuthUI.IdpConfig.GoogleBuilder().build()
                )
            )
            .build()
        signInLauncher.launch(signInIntent)
    }
    private fun signOut(){
        Log.d("AUTH", "LOGOUT START")
        AuthUI.getInstance()
            .signOut(this)
            .addOnCompleteListener {
                Log.d("AUTH", "USER: " + auth.currentUser)
            }
    }

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
                    NavHost(navController = navController, startDestination = "documentsList") {
                        composable("documentsList") { backStackEntry: NavBackStackEntry ->
                            DocumentsListScreen(
                                openTagManagement = {
                                    navController.navigate("tagManagement")
                                },
                                addDocument = {navController.navigate("documentEditScreen")},
                                editDocument = { documentId: Long ->
                                    navController.navigate("documentEditScreen/${documentId}")},
                                login = { startSignIn() },
                                logout = { signOut() }
                            )
                        }
                        composable("tagManagement") { backStackEntry: NavBackStackEntry ->
                            TagManagementScreen(dismissTagManager = {
                                navController.popBackStack()
                            })
                        }
                        composable("documentEditScreen") { backStackEntry: NavBackStackEntry ->
                            DocumentEditScreen(navController = navController, null)
                        }
                        composable(
                            "documentEditScreen/{documentId}",
                            arguments = listOf(navArgument("documentId"){type = NavType.LongType})
                        ) { backStackEntry: NavBackStackEntry ->
                            val id = backStackEntry.arguments?.getLong("documentId")
                            DocumentEditScreen(navController = navController, id)
                        }
                    }
                }
            }
        }
    }
}

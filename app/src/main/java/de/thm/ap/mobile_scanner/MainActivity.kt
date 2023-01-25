package de.thm.ap.mobile_scanner

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import de.thm.ap.mobile_scanner.data.ReferenceCollection
import de.thm.ap.mobile_scanner.ui.screens.*
import de.thm.ap.mobile_scanner.ui.theme.MobilescannerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore_db: FirebaseFirestore = Firebase.firestore

    private val signInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract()
    ) { result: FirebaseAuthUIAuthenticationResult? ->
        Log.d("AUTH", "USER: " + auth.currentUser)
        handleUserDocument()
    }
    /**
     * Function needs to be run after sign in and at startup
     */
    private fun handleUserDocument(){
        this.lifecycleScope.launch(Dispatchers.IO) {
            if (auth.currentUser == null) return@launch

            val user = hashMapOf(
                "uid" to auth.currentUser!!.uid,
                "name" to auth.currentUser!!.displayName
            )
            firestore_db
                .collection("users")
                .whereEqualTo("uid", auth.currentUser!!.uid)
                .get()
                .addOnSuccessListener { querySnapshot: QuerySnapshot ->
                    when {
                        // user doesn't exist
                        querySnapshot.isEmpty -> {
                            firestore_db.collection("users").add(user)
                                .addOnSuccessListener {
                                    ReferenceCollection.userDocReference = it
                                }
                        }
                        //user exists
                        else -> {
                            querySnapshot.forEach { documentSnapshot: QueryDocumentSnapshot ->
                                auth.currentUser?.uid?.let{
                                    if (documentSnapshot.get("uid") == it){
                                        ReferenceCollection.userDocReference = documentSnapshot.reference
                                    }
                                }
                            }
                        }
                    }
                }
        }
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
        handleUserDocument()

        setContent {
            MobilescannerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController = navController,
                        startDestination = if(Firebase.auth.currentUser == null) "welcome"
                                            else "documentsList"
                    )
                    {
                        composable("documentsList") { backStackEntry: NavBackStackEntry ->
                            DocumentsListScreen(
                                openDocument = { navController.navigate("documentViewScreen/${it}")},
                                openUserScreen = {
                                    navController.navigate("userScreen")
                                },
                                addDocument = {
                                        navController.navigate("documentEditScreen")
                                              },
                                editDocument = { documentUID: String ->
                                    navController.navigate("documentEditScreen/${documentUID}")},
                                login = { startSignIn() },
                                logout = { signOut() }
                            )
                        }
                        composable("documentEditScreen") { backStackEntry: NavBackStackEntry ->
                            DocumentEditScreen(navController = navController, null)
                        }
                        composable("userScreen") { backStackEntry: NavBackStackEntry ->
                            UserScreen(goBack = { navController.popBackStack() },
                            logout = {
                                    Firebase.auth.addAuthStateListener {
                                        if(Firebase.auth.currentUser == null) {
                                            navController.navigate("welcome")
                                        }
                                    }
                                    signOut()
                                }
                            )
                        }
                        composable("welcome") { backStackEntry: NavBackStackEntry ->
                            WelcomeScreen (
                                login = { startSignIn() },
                                loginSuccess = { navController.navigate("documentsList")})
                        }
                        composable(
                            "documentEditScreen/{documentUID}",
                            arguments = listOf(navArgument("documentUID"){type = NavType.StringType})
                        ) { backStackEntry: NavBackStackEntry ->
                            val uid = backStackEntry.arguments?.getString("documentUID")
                            DocumentEditScreen(navController = navController, uid)
                        }
                        composable(
                            "documentViewScreen/{documentId}",
                            arguments = listOf(navArgument("documentId"){type = NavType.StringType})
                        ) { backStackEntry: NavBackStackEntry ->
                            val id = backStackEntry.arguments?.getString("documentId")!!
                            DocumentViewScreen(
                                id,
                                { navController.navigateUp() },
                                editDocument = { documentId ->
                                    navController.navigate("documentEditScreen/${documentId}")
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

package de.thm.ap.mobile_scanner.ui.screens

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import de.thm.ap.mobile_scanner.R

class UserScreenViewModel(app: Application) : AndroidViewModel(app) {
    val username: String? = Firebase.auth.currentUser?.displayName
}

@Composable
fun UserScreen(
    goBack: () -> Unit,
    logout: () -> Unit){
    val vm: UserScreenViewModel = viewModel()
    Scaffold(
        topBar = {
            TopAppBar(navigationIcon = {
                IconButton(onClick = goBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = stringResource(id = R.string.navigation_back)
                    )
                }
            },
                title = { Text(text = stringResource(id = R.string.my_account)) })
        }
    ){ padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxWidth(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(30.dp))
            Icon(
                imageVector = Icons.Rounded.AccountCircle,
                contentDescription = "",
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(120.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = vm.username ?: stringResource(R.string.unknown_name),
                fontWeight = FontWeight.SemiBold,
                fontSize = 30.sp
                )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = logout,
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(50.dp)
            ) {
                Text(text = stringResource(R.string.logout))
            }
        }
    }
}
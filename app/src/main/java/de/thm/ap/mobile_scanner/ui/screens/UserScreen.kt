package de.thm.ap.mobile_scanner.ui.screens

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.AndroidViewModel
import de.thm.ap.mobile_scanner.R

class UserScreenViewModel(app: Application) : AndroidViewModel(app) {

}

@Composable
fun UserScreen(
    goBack: () -> Unit,
    logout: () -> Unit){
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
    ){
        Column(Modifier.padding(it)) {
            Button(onClick = logout) {
                Text(text = stringResource(R.string.logout))
            }
        }
    }
}
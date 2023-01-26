package de.thm.ap.mobile_scanner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import de.thm.ap.mobile_scanner.R

@Composable
fun WelcomeScreen(login: () -> Unit, loginSuccess: () -> Unit ){
    Scaffold(
    ) { innerPadding ->
        Column(
            modifier =Modifier.fillMaxSize(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize(0.8f)
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally

            ) {
                Text(
                    text = stringResource(id = R.string.welcome_heading),
                    style = TextStyle(
                        fontSize = 30.sp,
                        textAlign = TextAlign.Center
                    ),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(id = R.string.welcome_get_started),
                    style = TextStyle(
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.padding(7.dp)
                )
                Spacer(modifier = Modifier.height(60.dp))
                Button(
                    onClick = {
                        Firebase.auth.addAuthStateListener {
                            if (it.currentUser != null) loginSuccess()
                        }
                        login()
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(50.dp)
                ) {
                    Text(stringResource(id = R.string.login))
                }
            }
        }
    }
}
package de.thm.ap.mobile_scanner.ui.screens

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.ktx.Firebase
import de.thm.ap.mobile_scanner.R
import de.thm.ap.mobile_scanner.data.ReferenceCollection
import de.thm.ap.mobile_scanner.data.firebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

class UserScreenViewModel(app: Application) : AndroidViewModel(app) {
    val username: String? = Firebase.auth.currentUser?.displayName

    var showStats: Boolean by mutableStateOf(false)

    var numberOfDocuments: Long? by mutableStateOf(null)
    var totalPageCount: Long? by mutableStateOf(null)
    var usedStorageInBytes: Long by mutableStateOf(0)

    fun averagePageCount(): Double?{
        return if(numberOfDocuments != null && numberOfDocuments != 0L && totalPageCount != null){
                    totalPageCount!!.toDouble() / numberOfDocuments!!.toDouble()
                }
                else null
    }

    var statsNotCalled = true
    fun getUserStats() {
        if (statsNotCalled) {
            statsNotCalled = false
            viewModelScope.launch(Dispatchers.IO) {
                ReferenceCollection.userDocReference
                    ?.collection("documents")
                    ?.count()?.get(AggregateSource.SERVER)?.addOnSuccessListener {
                        numberOfDocuments = it.count
                    }
                firebaseStorage?.listAll()?.addOnSuccessListener {
                    totalPageCount = it.items.size.toLong()

                    it.items.forEach { reference ->
                        reference.metadata.addOnSuccessListener { metadata ->
                            usedStorageInBytes += metadata.sizeBytes
                        }
                    }
                }
            }
        }
    }
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
            val minimumSpacerHeight = 20.dp
            val buttonHeight = 50.dp
            val maxButtonWidth = 0.4f
            Spacer(modifier = Modifier.height(minimumSpacerHeight + 10.dp))
            Icon(
                imageVector = Icons.Rounded.AccountCircle,
                contentDescription = "",
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(120.dp)
            )
            Spacer(modifier = Modifier.height(minimumSpacerHeight))
            Text(
                text = vm.username ?: stringResource(R.string.unknown_name),
                fontWeight = FontWeight.SemiBold,
                fontSize = 30.sp
                )
            Spacer(modifier = Modifier.height(minimumSpacerHeight))
            Button(
                onClick = {
                          vm.showStats = true
                },
                modifier = Modifier
                    .fillMaxWidth(maxButtonWidth)
                    .height(buttonHeight)
            ) {
                Text(text = stringResource(R.string.user_stats))
            }
            UserStatisticsDialog(vm)

            Spacer(modifier = Modifier.height(minimumSpacerHeight))
            Button(
                onClick = logout,
                modifier = Modifier
                    .fillMaxWidth(maxButtonWidth)
                    .height(buttonHeight)
            ) {
                Text(text = stringResource(R.string.logout))
            }
        }
    }
}

fun byteCountToMegabyte(byteTotal: Long): Double{
    return byteTotal.toDouble() / 1024 / 1024
}

@Composable
fun UserStatisticsDialog(vm: UserScreenViewModel) {
    val showDialog: Boolean = vm.showStats
    val totalPageCount: Long? = vm.totalPageCount
    val documentCount: Long? = vm.numberOfDocuments
    val averagePageCount: Double? = vm.averagePageCount()
    val storageUsage = BigDecimal(byteCountToMegabyte(vm.usedStorageInBytes))
        .setScale(2, RoundingMode.HALF_EVEN)
    vm.getUserStats()
    if(showDialog){
        AlertDialog(
            onDismissRequest = { vm.showStats = false },
            buttons = {
                Row(
                    modifier = Modifier.padding(all = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { vm.showStats = false }
                    ) {
                        Text(stringResource(R.string.close))
                    }
                }
            },
            title = {
                Text(stringResource(id = R.string.user_stats))
                    },
            text = {
                when(documentCount){
                    (null) -> Text(stringResource(id = R.string.stats_creation))
                    (0L) -> Text(stringResource(id = R.string.stats_no_documents))
                    else -> {
                        Box {
                            Text(
                                stringResource(id = R.string.stats_documents) + ": $documentCount \n" +
                                        stringResource(id = R.string.stats_total_pages) + ": $totalPageCount\n" +
                                        stringResource(id = R.string.stats_average_pages) + ": $averagePageCount\n" +
                                        stringResource(id = R.string.stats_storage) + ": $storageUsage MB"
                            )
                        }
                    }
                }
            }
        )
    }
}
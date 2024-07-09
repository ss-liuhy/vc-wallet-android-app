package org.idp.wallet.verifiable_credentials_library.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import org.idp.wallet.verifiable_credentials_library.ui.component.LoadingScreen
import org.idp.wallet.verifiable_credentials_library.ui.viewmodel.VerifiableCredentialsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VcScreen(
    viewModel: VerifiableCredentialsViewModel,
    resolveQrCode: (format: String) -> Unit,
) {
  if (viewModel.loadingState.collectAsState().value) {
    LoadingScreen(color = MaterialTheme.colorScheme.primary)
    return
  }
  Scaffold(
      topBar = {
        CenterAlignedTopAppBar(
            title = {
              Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "Issue Vc", style = MaterialTheme.typography.displayMedium)
                  }
            })
      },
      content = { paddingValues ->
        Column(
            modifier = Modifier.fillMaxWidth().padding(paddingValues),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
              var format by remember { mutableStateOf("vc+sd-jwt") }
              val vciState = viewModel.vciState.collectAsState()
              Column(
                  modifier = Modifier.fillMaxWidth().padding(Dp(16.0F)),
                  horizontalAlignment = Alignment.CenterHorizontally) {
                    Row {
                      Button(
                          modifier = Modifier.padding(top = Dp(16.0F)),
                          onClick = {
                            viewModel.showDialog(
                                title = "confirm",
                                message = "Could you scan qr?",
                                onClickPositiveButton = { resolveQrCode(format) })
                          }) {
                            Text(text = "scan QR")
                          }
                    }
                  }
            }
      })
}

package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.BlackiApp
import com.example.ui.BlackiViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: BlackiViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                // BackHandler to return to Hub tab if in secondary tab
                BackHandler(enabled = uiState.selectedTab != 0 || uiState.previewFile != null) {
                    if (uiState.previewFile != null) {
                        viewModel.dismissFilePreview()
                    } else if (uiState.selectedTab != 0) {
                        viewModel.selectTab(0)
                    }
                }

                BlackiApp(
                    viewModel = viewModel,
                    activity = this@MainActivity
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshDeviceState()
    }
}

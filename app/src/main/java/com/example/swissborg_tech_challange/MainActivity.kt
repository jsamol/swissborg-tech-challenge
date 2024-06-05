@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.swissborg_tech_challange

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.swissborg_tech_challange.ui.screen.Dashboard
import com.example.swissborg_tech_challange.ui.theme.SwissborgtechchallangeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SwissborgtechchallangeTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val state by viewModel.state.collectAsState()

                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text(text = "Tech Challenge")
                                }
                            )
                        }
                    ) { padding ->
                        Dashboard(
                            modifier = Modifier.padding(padding),
                            state = state.dashboard,
                            refresh = { viewModel.refresh() },
                            filter = { viewModel.filter(it) },
                        )
                    }
                }
            }
        }
    }
}
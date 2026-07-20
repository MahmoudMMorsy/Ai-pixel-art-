package com.retro.pixelanimator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.retro.pixelanimator.ui.screens.PixelLabScreen
import com.retro.pixelanimator.ui.theme.PixelAnimatorTheme
import com.retro.pixelanimator.ui.viewmodel.PixelAnimatorViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: PixelAnimatorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PixelAnimatorTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PixelLabScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.MetrajeInstanteTheme
import com.example.ui.viewmodel.MeasurementViewModel
import com.example.util.LanguageManager
import com.example.util.LocalAppStrings

class MainActivity : ComponentActivity() {

    private val viewModel: MeasurementViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LanguageManager.init(this)
        enableEdgeToEdge()
        setContent {
            val currentStrings by LanguageManager.currentStrings.collectAsStateWithLifecycle()
            CompositionLocalProvider(LocalAppStrings provides currentStrings) {
                MetrajeInstanteTheme {
                    AppNavigation(viewModel = viewModel)
                }
            }
        }
    }
}

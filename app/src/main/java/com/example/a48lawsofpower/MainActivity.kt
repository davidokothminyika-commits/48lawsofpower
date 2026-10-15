package com.example.a48lawsofpower

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.a48lawsofpower.data.LawDatabase
import com.example.a48lawsofpower.legal.ConsentStatus
import com.example.a48lawsofpower.legal.ConsentStore
import com.example.a48lawsofpower.ui.LawsViewModel
import com.example.a48lawsofpower.ui.annotations.AnnotationsViewModel
import com.example.a48lawsofpower.ui.legal.LegalViewModel
import com.example.a48lawsofpower.ui.navigation.AppNavGraph
import com.example.a48lawsofpower.ui.profile.ProfileViewModel
import com.example.a48lawsofpower.ui.screens.ConsentGateScreen
import com.example.a48lawsofpower.ui.screens.LoadingState
import com.example.a48lawsofpower.ui.settings.SettingsStore
import com.example.a48lawsofpower.ui.settings.SettingsViewModel
import com.example.a48lawsofpower.ui.theme._48LawsOfPowerTheme

class MainActivity : ComponentActivity() {

    // Resolved once per Activity instead of on every recomposition of the content lambda.
    private val viewModel: LawsViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val database = LawDatabase.getDatabase(applicationContext)
                @Suppress("UNCHECKED_CAST")
                return LawsViewModel(database.lawDao()) as T
            }
        }
    }

    private val legalViewModel: LegalViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return LegalViewModel(ConsentStore(applicationContext)) as T
            }
        }
    }

    private val profileViewModel: ProfileViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val database = LawDatabase.getDatabase(applicationContext)
                @Suppress("UNCHECKED_CAST")
                return ProfileViewModel(database.progressDao()) as T
            }
        }
    }

    private val annotationsViewModel: AnnotationsViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val database = LawDatabase.getDatabase(applicationContext)
                @Suppress("UNCHECKED_CAST")
                return AnnotationsViewModel(database.annotationDao(), database.lawDao()) as T
            }
        }
    }

    private val settingsViewModel: SettingsViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return SettingsViewModel(SettingsStore(applicationContext)) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()

            _48LawsOfPowerTheme(themeMode = themeMode) {
                val consent by legalViewModel.consent.collectAsStateWithLifecycle()

                // The gate lives here rather than inside the nav graph so that nothing behind it is
                // ever composed: [LawsViewModel] opens the database the moment it is constructed, and
                // that must not happen for a reader who has not agreed yet.
                when (consent) {
                    ConsentStatus.Unknown ->
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.Center,
                        ) {
                            LoadingState()
                        }

                    ConsentStatus.Required ->
                        ConsentGateScreen(onAccept = legalViewModel::accept)

                    ConsentStatus.Granted ->
                        AppNavGraph(
                            viewModel = viewModel,
                            legalViewModel = legalViewModel,
                            annotationsViewModel = annotationsViewModel,
                            settingsViewModel = settingsViewModel,
                            profileViewModel = profileViewModel,
                        )
                }
            }
        }
    }
}
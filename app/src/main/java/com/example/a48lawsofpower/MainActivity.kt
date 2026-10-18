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
import com.example.a48lawsofpower.ui.speech.TtsReader
import com.example.a48lawsofpower.ui.legal.LegalViewModel
import com.example.a48lawsofpower.ui.navigation.AppNavGraph
import com.example.a48lawsofpower.ui.profile.ProfileViewModel
import com.example.a48lawsofpower.ui.progress.ReadingTrackerViewModel
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

    // Reading-time accounting. Activity-scoped so a rotation does not close the counting window and
    // write a fragmentary number of seconds that never adds up to a minute.
    private val readingTrackerViewModel: ReadingTrackerViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val database = LawDatabase.getDatabase(applicationContext)
                @Suppress("UNCHECKED_CAST")
                return ReadingTrackerViewModel(database.progressDao()) as T
            }
        }
    }

    /**
     * The foreground half of the reading-time gate, driven from the lifecycle rather than from
     * composition so the flush on the way out still happens when the composition is already gone.
     *
     * `onStop` rather than `onPause`: a dialog or the notification shade pauses the Activity
     * without the reader having looked away, and time spent squinting at a permission prompt is not
     * reading. Reading time stops while the app is off the screen, which is the whole point.
     */
    override fun onStart() {
        super.onStart()
        readingTrackerViewModel.setForeground(true)
    }

    override fun onStop() {
        readingTrackerViewModel.setForeground(false)
        super.onStop()
    }

    /**
     * Owns the speech engine for the whole Activity.
     *
     * Created once rather than per-composition: [android.speech.tts.TextToSpeech] spins up a
     * connection to the system engine, and re-creating it on every recomposition would restart
     * speech mid-sentence. Released in onDestroy because the engine holds a system resource.
     */
    private var ttsReader: TtsReader? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        ttsReader = TtsReader(applicationContext)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
                val hasSeenIntro by settingsViewModel.hasSeenIntro.collectAsStateWithLifecycle()

                _48LawsOfPowerTheme(themeMode = themeMode) {
                    val consent by legalViewModel.consent.collectAsStateWithLifecycle()

                    // The gate lives here rather than inside the nav graph so that nothing behind it is
                    // ever composed: [LawsViewModel] opens the database the moment it is constructed, and
                    // that must not happen for a reader who has not agreed yet.
                    when {
                        // Consent has to be settled, and so does the entry-screen flag - the nav
                        // graph's start destination is chosen from it, and guessing would either
                        // flash Get Started at a returning reader or skip it on a real first launch.
                        consent == ConsentStatus.Unknown || hasSeenIntro == null ->
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background),
                                contentAlignment = Alignment.Center,
                            ) {
                                LoadingState()
                            }

                        consent == ConsentStatus.Required ->
                            ConsentGateScreen(onAccept = legalViewModel::accept)

                        else ->
                            AppNavGraph(
                                viewModel = viewModel,
                                legalViewModel = legalViewModel,
                                annotationsViewModel = annotationsViewModel,
                                settingsViewModel = settingsViewModel,
                                profileViewModel = profileViewModel,
                                readingTrackerViewModel = readingTrackerViewModel,
                                startAtDossier = hasSeenIntro == true,
                                speech = ttsReader,
                            )
                    }
                }
        }
    }
}
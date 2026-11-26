package com.miguelaetxio.mia2f

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.miguelaetxio.mia2f.ui.profile.ProfileSetupScreen
import com.miguelaetxio.mia2f.ui.profile.ProfileViewModel
import com.miguelaetxio.mia2f.ui.settings.SettingsScreen
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single entry point. Shows the first-run profile screen until the
 * student has a name, then the home screen.
 * ---
 * Punto de entrada único. Muestra la pantalla de perfil de primer
 * arranque hasta que el alumno tiene nombre, y después la de inicio.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot()
                }
            }
        }
    }
}

@Composable
private fun AppRoot(
    profileViewModel: ProfileViewModel = hiltViewModel(),
) {
    val studentName by profileViewModel.studentName.collectAsState()
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val name = studentName
    when {
        name == null ->
            ProfileSetupScreen(onSave = profileViewModel::saveStudentName)
        showSettings -> SettingsScreen(onBack = { showSettings = false })
        else -> HomeScreen(
            studentName = name,
            onOpenSettings = { showSettings = true },
        )
    }
}

@Composable
private fun HomeScreen(
    studentName: String,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            text = "miA2F",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "Bonjour, $studentName !",
            style = MaterialTheme.typography.titleLarge,
        )
        Button(onClick = onOpenSettings) {
            Text("Ajustes")
        }
    }
}

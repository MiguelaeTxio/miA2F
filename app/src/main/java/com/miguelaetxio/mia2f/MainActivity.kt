package com.miguelaetxio.mia2f

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.miguelaetxio.mia2f.ui.profile.ProfileSetupScreen
import com.miguelaetxio.mia2f.ui.profile.ProfileViewModel
import com.miguelaetxio.mia2f.ui.course.CourseScreen
import com.miguelaetxio.mia2f.ui.course.CourseViewModel
import com.miguelaetxio.mia2f.ui.keytransfer.scanKeyQr
import com.miguelaetxio.mia2f.ui.settings.SettingsScreen
import com.miguelaetxio.mia2f.ui.settings.SettingsViewModel
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
    var showCourse by rememberSaveable { mutableStateOf(false) }
    val name = studentName
    when {
        name == null ->
            ProfileSetupScreen(onSave = profileViewModel::saveStudentName)
        showSettings -> SettingsScreen(onBack = { showSettings = false })
        showCourse -> CourseScreen(onBack = { showCourse = false })
        else -> HomeScreen(
            studentName = name,
            onOpenSettings = { showSettings = true },
            onOpenCourse = { showCourse = true },
        )
    }
}

@Composable
private fun HomeScreen(
    studentName: String,
    onOpenSettings: () -> Unit,
    onOpenCourse: () -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    courseViewModel: CourseViewModel = hiltViewModel(),
) {
    val keySuffix by settingsViewModel.keySuffix.collectAsState()
    val course by courseViewModel.course.collectAsState()
    val courseError by courseViewModel.refreshMessage.collectAsState()
    val context = LocalContext.current
    var scanError by rememberSaveable { mutableStateOf<String?>(null) }
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
        Text(
            text = course?.let {
                "${it.title} — versión ${it.version} del temario, " +
                    "${it.units.size} unidades."
            } ?: "Descargando el temario…",
            style = MaterialTheme.typography.bodyMedium,
        )
        courseError?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        if (keySuffix == null) {
            // Primer arranque en el segundo móvil: la clave llega por QR
            // desde el móvil que ya la tiene, sin entrar en AI Studio.
            // ---
            // First run on the second phone: the key arrives by QR from
            // the phone that already has it, with no AI Studio needed.
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "Falta un último paso: en el otro móvil, abre " +
                            "miA2F → Ajustes → «Pasar la clave a otro " +
                            "móvil», y aquí pulsa este botón y apunta la " +
                            "cámara al código que aparece.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(
                        onClick = {
                            scanError = null
                            scanKeyQr(
                                context = context,
                                onKey = settingsViewModel::saveKey,
                                onError = { scanError = it },
                            )
                        },
                    ) {
                        Text("Escanear clave desde otro móvil")
                    }
                    scanError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        Button(onClick = onOpenCourse, enabled = course != null) {
            Text("Temario")
        }
        Button(onClick = onOpenSettings) {
            Text("Ajustes")
        }
    }
}

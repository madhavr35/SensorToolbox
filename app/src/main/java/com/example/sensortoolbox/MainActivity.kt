package com.example.sensortoolbox

import android.os.Bundle
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.hardware.Sensor
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.sensortoolbox.ui.theme.SensorToolboxTheme
import com.example.sensortoolbox.services.SensorService
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SensorToolboxTheme {
                MainApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    DeveloperBanner(name = "Madhav Ramesh")
                },
                navigationIcon = {
                    if (currentRoute == "references") {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    }
                },
                actions = {
                    if (currentRoute == "home") {
                        IconButton(onClick = { navController.navigate("references") }) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "References"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                )
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                HomeScreen()
            }
            composable("references") {
                ReferencesScreen()
            }
        }
    }
}

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val readings = remember { mutableStateMapOf<Int, SensorService.Reading>() }
    var connected by remember { mutableStateOf(false) }
    var availableSensors by remember { mutableStateOf(emptySet<Int>()) }
    val sensorTypes = listOf(
        Sensor.TYPE_ACCELEROMETER,
        Sensor.TYPE_GYROSCOPE,
        Sensor.TYPE_MAGNETIC_FIELD,
        Sensor.TYPE_LIGHT,
        Sensor.TYPE_RELATIVE_HUMIDITY,
        Sensor.TYPE_AMBIENT_TEMPERATURE,
        Sensor.TYPE_PRESSURE
    )

    DisposableEffect(context) {
        var sensorService: SensorService? = null
        val listener: (SensorService.Reading) -> Unit = { reading ->
            readings[reading.type] = reading
        }
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                val service = (binder as SensorService.LocalBinder).getService()
                sensorService = service
                availableSensors = sensorTypes.filter(service::isSensorAvailable).toSet()
                readings.putAll(service.getReadings())
                service.addListener(listener)
                connected = true
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                sensorService = null
                connected = false
            }
        }
        val bound = context.bindService(
            Intent(context, SensorService::class.java),
            connection,
            Context.BIND_AUTO_CREATE
        )

        onDispose {
            sensorService?.removeListener(listener)
            if (bound) context.unbindService(connection)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = "Sensor readings",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.size(8.dp))
        if (!connected) {
            Text("Connecting to sensors...", style = MaterialTheme.typography.bodyMedium)
        }
        LazyColumn {
            items(sensorTypes) { type ->
                val reading = readings[type]
                val valueText = when {
                    !connected -> "Connecting..."
                    type !in availableSensors -> "Unavailable on this device"
                    reading == null -> "Waiting for first reading"
                    else -> reading.values.joinToString { value ->
                        "${String.format(Locale.getDefault(), "%.2f", value)} ${sensorUnit(type)}"
                    }
                }
                ListItem(
                    headlineContent = { Text(sensorName(type)) },
                    supportingContent = { Text(valueText) }
                )
                HorizontalDivider()
            }
        }
    }
}

private fun sensorName(type: Int): String = when (type) {
    Sensor.TYPE_ACCELEROMETER -> "Accelerometer"
    Sensor.TYPE_GYROSCOPE -> "Gyroscope"
    Sensor.TYPE_MAGNETIC_FIELD -> "Magnetic field"
    Sensor.TYPE_LIGHT -> "Light"
    Sensor.TYPE_RELATIVE_HUMIDITY -> "Relative humidity"
    Sensor.TYPE_AMBIENT_TEMPERATURE -> "Temperature"
    Sensor.TYPE_PRESSURE -> "Barometer"
    else -> "Sensor"
}

private fun sensorUnit(type: Int): String = when (type) {
    Sensor.TYPE_ACCELEROMETER -> "m/s^2"
    Sensor.TYPE_GYROSCOPE -> "rad/s"
    Sensor.TYPE_MAGNETIC_FIELD -> "uT"
    Sensor.TYPE_LIGHT -> "lx"
    Sensor.TYPE_RELATIVE_HUMIDITY -> "%"
    Sensor.TYPE_AMBIENT_TEMPERATURE -> "°C"
    Sensor.TYPE_PRESSURE -> "hPa"
    else -> ""
}

@Composable
fun ReferencesScreen() {
    val references = listOf(
        ReferenceItem("Android Developer Docs", "Official Android documentation and guides", "https://source.android.com/docs"),
        ReferenceItem("Android Programming Course", "Kotlin and Jetpack Compose", "https://youtu.be/bo_LP6QOUio"),
        ReferenceItem("Trail Sense Application", "An Android app that uses the phone's sensors to assist with wilderness treks or survival situations.", "https://github.com/kylecorry31/Trail-Sense"),
        ReferenceItem("Gemini for Android Studio", "Used for template generation", "https://gemini.google.com/"),
        ReferenceItem("GitHub Copilot for Visual Studio Code","Used for debugging","https://github.com/features/copilot")
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "References",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp),
            fontWeight = FontWeight.Bold
        )
        LazyColumn {
            items(references) { item ->
                ListItem(
                    headlineContent = { Text(item.title) },
                    supportingContent = { Text(item.description) },
                    trailingContent = {
                        Text(
                            text = "Link",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                )
                HorizontalDivider()
            }
        }
    }
}

data class ReferenceItem(val title: String, val description: String, val url: String)

@Composable
fun DeveloperBanner(name: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Image(
            painter = painterResource(id = R.drawable.ub_rgb_blue),
            contentDescription = "Developer Logo",
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainAppPreview() {
    SensorToolboxTheme {
        MainApp()
    }
}

@Preview(showBackground = true)
@Composable
fun ReferencesPreview() {
    SensorToolboxTheme {
        ReferencesScreen()
    }
}

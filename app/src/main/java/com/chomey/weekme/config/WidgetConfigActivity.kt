package com.chomey.weekme.config

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class WidgetConfigActivity : ComponentActivity() {

    private var hasPermission by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hasPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "WeekMe",
                            style = MaterialTheme.typography.headlineLarge,
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (hasPermission) {
                            Text(
                                text = "Calendar permission granted. Add the WeekMe widget to your home screen!",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        } else {
                            Text(
                                text = "WeekMe needs calendar access to display your events.",
                                style = MaterialTheme.typography.bodyLarge,
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(onClick = {
                                permissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                            }) {
                                Text("Grant Calendar Permission")
                            }
                        }
                    }
                }
            }
        }
    }
}

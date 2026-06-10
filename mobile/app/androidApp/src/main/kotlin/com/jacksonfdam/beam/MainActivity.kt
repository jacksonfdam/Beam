package com.jacksonfdam.beam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.jacksonfdam.beam.remote.ui.BeamRemoteApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            BeamRemoteApp()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    BeamRemoteApp()
}
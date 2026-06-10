package com.jacksonfdam.beam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.jacksonfdam.beam.remote.AndroidConnectionStore
import com.jacksonfdam.beam.remote.ui.BeamRemoteApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // The launcher uses the splash theme; switch to the normal app theme now.
        setTheme(R.style.Theme_Beam)

        val store = AndroidConnectionStore(applicationContext)
        setContent {
            BeamRemoteApp(store)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    BeamRemoteApp()
}

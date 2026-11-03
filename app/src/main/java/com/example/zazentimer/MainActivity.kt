package com.example.zazentimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.zazentimer.ui.ZazenTimerScreen
import com.example.zazentimer.ui.theme.ZazenTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ZazenTheme {
                ZazenTimerScreen()
            }
        }
    }
}

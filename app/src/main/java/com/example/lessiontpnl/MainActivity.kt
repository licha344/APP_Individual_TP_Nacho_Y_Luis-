package com.example.lessiontpnl

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.lessiontpnl.ui.MainScreen
import com.example.lessiontpnl.ui.theme.LessionTPNLTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LessionTPNLTheme {
                MainScreen()
            }
        }
    }
}

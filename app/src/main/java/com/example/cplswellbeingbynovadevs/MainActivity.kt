package com.example.cplswellbeingbynovadevs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.cplswellbeingbynovadevs.data.repository.WellbeingRepository
import com.example.cplswellbeingbynovadevs.navigation.AppNavGraph
import com.example.cplswellbeingbynovadevs.ui.theme.CplsWellbeingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = WellbeingRepository(applicationContext)

        setContent {
            CplsWellbeingTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavGraph(repository = repository)
                }
            }
        }
    }
}

package com.example.appserver

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.appserver.ui.EventLogScreen
import com.example.appserver.ui.HomeScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = "home") {
                composable("home") {
                    HomeScreen(
                        onLogsClick = { navController.navigate("eventLog") }
                    )
                }
                composable("eventLog") { EventLogScreen(onBackClick = { navController.popBackStack() }) }
            }
        }
    }
}

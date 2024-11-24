package com.myprojects.modules.runningtracker.ui

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.myprojects.modules.runningtracker.ui.viewmodel.MainViewmodel

@Composable
fun RunsComposable(navController: NavController, viewmodel: MainViewmodel) {
    val runsFlow by viewmodel.runsFlow.collectAsState()

    LaunchedEffect(Unit) {
        viewmodel.getRunsFlow()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        runsFlow.let {
            Log.d("------", "runs: ${it.size}")
            it.forEach {
                Row {
                    RunCard(it.id, it.timestamp)
                }
            }
        }
        Button(
            onClick = { navController.navigate(route = Routes.Tracking.route) }
        ) {
            Text("Get started...")
        }
    }
}

@Composable
fun RunCard(id: Int, timeStamp: Long) {
    Card(
        modifier = Modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(4.dp))
    ) {
        Text(
            modifier = Modifier.padding(8.dp),
            text = buildString {
                append(id)
                append(". ")
                append(timeStamp)
            }
        )
    }
}

sealed class Routes(val route: String) {
    object Login : Routes("Login")
    object Tracking : Routes("Tracking")
}
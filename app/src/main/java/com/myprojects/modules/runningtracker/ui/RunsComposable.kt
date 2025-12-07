package com.myprojects.modules.runningtracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.myprojects.modules.runningtracker.db.Run
import com.myprojects.modules.runningtracker.ui.viewmodel.MainViewmodel
import timber.log.Timber

@Composable
fun RunsComposable(navController: NavController, viewmodel: MainViewmodel) {
    val runsFlow by viewmodel.runsFlow.collectAsState()

    LaunchedEffect(Unit) {
        viewmodel.getRunsFlow()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                horizontal = 1.dp,
                vertical = 1.dp
            ),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            items(runsFlow) { run ->
            //    if (run.locationList.flatten().size > 100 || System.currentTimeMillis() - run.timestamp < 1000 * 60 * 60)
                    RunCard(navController, run)
            }
        }

        Column {
            Button(
                onClick = {
                    Timber.d("startRun")
                    viewmodel.startRun()
                    navController.navigate(route = Routes.Tracking.route)
                }
            ) {
                Text("New Run")
            }
        }
    }
}

@Composable
fun RunCard(navController: NavController, run: Run) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(2.dp)
            .clip(RoundedCornerShape(1.dp)),
        onClick = {
            navController.navigate(route = Routes.Route.withArgs(run.id.toString()))
        }
    ) {
        Text(
            modifier = Modifier.padding(6.dp),
            text = buildString {
                append(run.id)
                append(". ")
                append(run.start)
                append(" - ")
                append(run.end)
                append(" size = ")
                append(run.locationList.flatten().size)
            }
        )
    }
}
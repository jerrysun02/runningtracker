package com.myprojects.modules.runningtracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.myprojects.modules.runningtracker.db.Run
import com.myprojects.modules.runningtracker.ui.viewmodel.MainViewmodel
import com.myprojects.modules.runningtracker.util.calculateDistance
import com.myprojects.modules.runningtracker.util.formatTime
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RunsComposable(navController: NavController, viewmodel: MainViewmodel) {
    val runsFlow by viewmodel.runsFlow.collectAsState()

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(
                horizontal = 4.dp,
                vertical = 4.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(runsFlow) { run ->
                RunCard(navController, viewmodel, run)
            }
        }

        Column(modifier = Modifier.padding(top = 16.dp)) {
            Button(
                onClick = {
                    Timber.d("startRun")
                    viewmodel.startRun()
                    navController.navigate(route = Routes.Tracking.route)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("New Run")
            }
        }
    }
}

@Composable
fun RunCard(navController: NavController, viewmodel: MainViewmodel, run: Run) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp)),
        onClick = {
            navController.navigate(route = Routes.Route.withArgs(run.id.toString()))
        }
    ) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Date: ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(run.startedAt))}")
                Text("Distance: %.2f km".format(calculateDistance(run.locationList) / 1000f))
                Text("Duration: ${formatTime(run.durationInMillis)}")
            }
            IconButton(onClick = { viewmodel.deleteRun(run) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Run")
            }
        }
    }
}
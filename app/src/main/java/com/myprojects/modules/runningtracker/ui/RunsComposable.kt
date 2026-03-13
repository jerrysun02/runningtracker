package com.myprojects.modules.runningtracker.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.myprojects.modules.runningtracker.db.Run
import com.myprojects.modules.runningtracker.ui.viewmodel.TrackingViewmodel
import com.myprojects.modules.runningtracker.util.calculateDistance
import com.myprojects.modules.runningtracker.util.formatTime
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RunsComposable(navController: NavController, viewmodel: TrackingViewmodel) {
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(runsFlow) { run ->
                RunCard(navController, viewmodel, run)
            }
        }

        Column(modifier = Modifier.padding(vertical = 16.dp)) {
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
fun RunCard(navController: NavController, viewmodel: TrackingViewmodel, run: Run) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        onClick = {
            navController.navigate(route = Routes.Route.withArgs(run.id.toString()))
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = SimpleDateFormat("EEEE, MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(run.startedAt)),
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Distance", style = MaterialTheme.typography.labelMedium)
                        Text("%.2f km".format(calculateDistance(run.locationList) / 1000f), style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Duration", style = MaterialTheme.typography.labelMedium)
                        Text(formatTime(run.durationInMillis), style = MaterialTheme.typography.titleMedium)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Avg Speed: %.1f km/h".format(run.avgSpeedInKMH),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Image
            run.img?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Run Route",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } ?: Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
            }

            Spacer(modifier = Modifier.width(24.dp)) // Increased spacer to move image further left from the delete icon

            IconButton(onClick = { viewmodel.deleteRun(run) }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Run",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

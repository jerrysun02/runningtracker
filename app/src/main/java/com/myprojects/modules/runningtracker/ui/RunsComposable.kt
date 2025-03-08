package com.myprojects.modules.runningtracker.ui

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.google.android.gms.maps.model.LatLng
import com.myprojects.modules.runningtracker.db.Run
import com.myprojects.modules.runningtracker.ui.viewmodel.MainViewmodel
import com.vmadalin.easypermissions.EasyPermissions

@Composable
fun RunsComposable(navController: NavController, viewmodel: MainViewmodel) {
    val runsFlow by viewmodel.runsFlow.collectAsState()

//    fun showRoute(route: List<List<LatLng>>) {
//        navController.navigate(route = Routes.Route.route)
//    }

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
            runsFlow.forEach {
                item {
                    RunCard(navController, it)
                }
            }
        }

        Column {
            Button(
                onClick = {




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
            .padding(4.dp)
            .clip(RoundedCornerShape(1.dp)),
        onClick = {
  //          Log.d("------------", "card clicked...${run.id}")
            //showRoute(run.locationList)
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
            }
        )
        Text(
            text = run.locationList.toString()
        )
    }
}

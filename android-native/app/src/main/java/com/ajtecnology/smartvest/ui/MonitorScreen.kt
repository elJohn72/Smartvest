package com.ajtecnology.smartvest.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ajtecnology.smartvest.R
import com.ajtecnology.smartvest.theme.*

@Composable
fun MonitorScreen(
    viewModel: MonitorViewModel,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Paper)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.app_name),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Night
            )
            Button(
                onClick = onLogoutClick,
                colors = ButtonDefaults.buttonColors(containerColor = Night)
            ) {
                Text(stringResource(R.string.logout_button))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        when (val s = state) {
            is MonitorUiState.Loading -> {
                CircularProgressIndicator(color = Signal)
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(R.string.status_loading), color = MetaText)
            }
            is MonitorUiState.NoData -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Inset)
                ) {
                    Text(
                        text = stringResource(R.string.status_no_data),
                        modifier = Modifier.padding(24.dp),
                        color = BodyText
                    )
                }
            }
            is MonitorUiState.NoConnection -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = WarnSoft)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.status_no_connection),
                            fontWeight = FontWeight.Bold,
                            color = WarnOrange
                        )
                        Text(text = s.message, color = BodyText, fontSize = 12.sp)
                    }
                }
            }
            is MonitorUiState.Ready -> {
                val r = s.reading

                // Banner de Alerta SOS
                if (r.sosActive) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SosSoft)
                    ) {
                        Text(
                            text = stringResource(R.string.sos_active_alert),
                            color = SosRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Banner de datos desactualizados
                if (s.isStale) {
                    Text(
                        text = stringResource(R.string.status_stale),
                        color = CautionYellow,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Tarjeta Principal de Distancia
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = CardBg)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val dist = r.distanceCm
                        if (dist != null) {
                            Text(
                                text = "${dist.toInt()}",
                                fontSize = 64.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (dist <= 40) SosRed else if (dist <= 100) WarnOrange else ClearGreen
                            )
                            Text(
                                text = stringResource(R.string.distance_unit),
                                fontSize = 16.sp,
                                color = MetaText
                            )
                        } else {
                            Text("Sin lectura de sensor", color = MetaText)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Text(
                                text = "${stringResource(R.string.battery_label)} ${r.batteryLevel ?: "--"}%",
                                color = BodyText
                            )
                            Text(
                                text = if (r.gpsFix) stringResource(R.string.gps_fix) else stringResource(R.string.gps_no_fix),
                                color = if (r.gpsFix) ClearGreen else MetaText
                            )
                        }
                    }
                }
            }
        }
    }
}

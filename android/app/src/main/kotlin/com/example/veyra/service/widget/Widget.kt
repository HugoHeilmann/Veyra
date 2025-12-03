package com.example.veyra.service.widget

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.*
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.veyra.MainActivity
import com.example.veyra.R
import com.example.veyra.service.NotificationActionReceiver

class Widget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { WidgetContent() }
    }
}

@Composable
private fun WidgetContent() {
    val context = LocalContext.current

    val prefs = currentState<Preferences>()
    val title = prefs[WidgetPrefsKeys.TITLE] ?: "No music"
    val artist = prefs[WidgetPrefsKeys.ARTIST] ?: "Unknown artist"
    val album = prefs[WidgetPrefsKeys.ALBUM] ?: "Unknown album"
    val isPlaying = prefs[WidgetPrefsKeys.IS_PLAYING] ?: false

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(ColorProvider(Color.Black))
                .cornerRadius(24.dp)
                .padding(10.dp)
        ) {
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
            ) {

                // --- Ligne du haut : icône + infos texte ---
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = GlanceModifier.fillMaxWidth()
                ) {
                    val component = ComponentName(context, MainActivity::class.java)

                    Box(
                        modifier = GlanceModifier
                            .size(60.dp)
                            .cornerRadius(24.dp)
                            .background(ColorProvider(Color(0xFF262626)))
                            .clickable(actionStartActivity<MainActivity>()),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.music_note),
                            contentDescription = "Ouvrir Veyra",
                            modifier = GlanceModifier.size(24.dp)
                        )
                    }

                    Spacer(GlanceModifier.width(10.dp))

                    Column(
                        modifier = GlanceModifier.fillMaxWidth()
                    ) {
                        Text(
                            text = title,
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = 15.sp
                            )
                        )
                        Text(
                            text = artist,
                            style = TextStyle(
                                color = ColorProvider(Color(0xFFB0B0B0))),
                        )
                        Text(
                            text = album,
                            style = TextStyle(
                                color = ColorProvider(Color(0xFF808080))),
                        )
                    }
                }

                Spacer(GlanceModifier.height(30.dp))

                // --- Barre de contrôles ---
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .cornerRadius(24.dp)
                        .background(ColorProvider(Color(0xFF181818)))
                        .padding(vertical = 6.dp, horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = GlanceModifier.fillMaxWidth()
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_rewind_10_widget),
                            contentDescription = "Reculer de 10 secondes",
                            modifier = GlanceModifier
                                .size(22.dp)
                                .clickable(
                                    actionSendBroadcast(
                                        Intent(context, NotificationActionReceiver::class.java)
                                            .apply { action = "ACTION_REWIND_10" }
                                    )
                                )
                        )

                        Spacer(GlanceModifier.width(32.dp))

                        Image(
                            provider = ImageProvider(R.drawable.ic_previous_widget),
                            contentDescription = "Piste précédente",
                            modifier = GlanceModifier
                                .size(24.dp)
                                .clickable(
                                    actionSendBroadcast(
                                        Intent(context, NotificationActionReceiver::class.java)
                                            .apply { action = "ACTION_SKIP_PREV" }
                                    )
                                )
                        )

                        Spacer(GlanceModifier.width(32.dp))

                        Box(
                            modifier = GlanceModifier
                                .size(40.dp)
                                .background(ColorProvider(Color(0xFF252525))),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                provider = ImageProvider(
                                    if (isPlaying) R.drawable.ic_pause_widget
                                    else R.drawable.ic_play_widget
                                ),
                                contentDescription = if (isPlaying) "Pause" else "Lecture",
                                modifier = GlanceModifier
                                    .size(26.dp)
                                    .clickable(
                                        actionSendBroadcast(
                                            Intent(
                                                context, NotificationActionReceiver::class.java
                                            ).apply {
                                                action = "ACTION_PLAY_PAUSE"
                                            }
                                        )
                                    )
                            )
                        }

                        Spacer(GlanceModifier.width(32.dp))

                        Image(
                            provider = ImageProvider(R.drawable.ic_next_widget),
                            contentDescription = "Piste suivante",
                            modifier = GlanceModifier
                                .size(24.dp)
                                .clickable(
                                    actionSendBroadcast(
                                        Intent(context, NotificationActionReceiver::class.java)
                                            .apply { action = "ACTION_SKIP_NEXT" }
                                    )
                                )
                        )

                        Spacer(GlanceModifier.width(32.dp))

                        Image(
                            provider = ImageProvider(R.drawable.ic_forward_10_widget),
                            contentDescription = "Avancer de 10 secondes",
                            modifier = GlanceModifier
                                .size(22.dp)
                                .clickable(
                                    actionSendBroadcast(
                                        Intent(context, NotificationActionReceiver::class.java)
                                            .apply { action = "ACTION_FORWARD_10" }
                                    )
                                )
                        )
                    }
                }

                Spacer(GlanceModifier.height(4.dp))

                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .clickable(actionStartActivity<MainActivity>()),
                ) {}
            }
        }

        Spacer(GlanceModifier.height(8.dp))

        Text(
            text = "Veyra",
            style = TextStyle(
                color = ColorProvider(Color.White),
                fontSize = 12.sp
            )
        )
    }
}

package com.medtrack.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.medtrack.AppContainer
import com.medtrack.health.HealthStatus
import com.medtrack.reminders.DoseNotifier
import com.medtrack.ui.common.ScreenScaffold
import com.medtrack.ui.theme.LabelShape
import com.medtrack.ui.theme.medColors
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(c: AppContainer, isSetup: Boolean, onSetupDone: () -> Unit) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    var status by remember { mutableStateOf<SetupStatus?>(null) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }
    LaunchedEffect(refresh) {
        status = readSetupStatus(context, c.health)
        c.appScope.launch { c.refreshReminders() } // e.g. exact alarms were just allowed
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        // Permanently denied: the system shows no dialog, so send the user to the settings page.
        if (!granted) context.open(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
        refresh++
    }
    val healthPermissions = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { refresh++ }

    ScreenScaffold(title = if (isSetup) "Set up reminders" else "Settings", isTab = true) { padding ->
        val s = status ?: return@ScreenScaffold
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
            if (isSetup) {
                Text(
                    "MedTrack needs a few permissions so reminders ring on time, even when your phone is locked or asleep.",
                    Modifier.padding(horizontal = 16.dp),
                )
            }
            Row("Notifications", s.notifications) {
                if (channelBlocked(context)) {
                    context.open(
                        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            .putExtra(Settings.EXTRA_CHANNEL_ID, DoseNotifier.CHANNEL_ID),
                    )
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    context.open(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                }
            }
            Row("Exact alarms", s.exactAlarms) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.open(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, context.packageUri()))
                }
            }
            Row("Full-screen alarms", s.fullScreen) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    context.open(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, context.packageUri()))
                }
            }
            Row("Unrestricted battery", s.batteryUnrestricted) {
                context.open(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, context.packageUri()))
            }
            Text(
                "On Xiaomi, Oppo, Vivo and Samsung phones, also allow MedTrack to auto-start and run in the background in the phone's battery settings. After the phone restarts, reminders resume once you unlock it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.medColors.inkMuted,
                modifier = Modifier.padding(16.dp),
            )

            HorizontalDivider(color = MaterialTheme.medColors.foil)
            Text("Meal reminders (optional)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
            val (healthText, healthAction) = when (s.health) {
                HealthStatus.NOT_INSTALLED -> "Health Connect isn't installed." to ("Install" to { context.openHealthConnectStore() })
                HealthStatus.NEEDS_UPDATE -> "Health Connect needs an update." to ("Update" to { context.openHealthConnectStore() })
                HealthStatus.NO_BACKGROUND_READ ->
                    "This Health Connect version can't read meals in the background. Update it." to ("Update" to { context.openHealthConnectStore() })
                HealthStatus.NOT_CONNECTED -> "Not connected." to ("Connect" to { healthPermissions.launch(c.health.permissions) })
                HealthStatus.CONNECTED -> "Connected. MedTrack reads the meals you log." to null
            }
            ListItem(
                headlineContent = { Text("Health Connect", style = MaterialTheme.typography.titleMedium) },
                supportingContent = { Text(healthText) },
                trailingContent = { healthAction?.let { (label, action) -> TextButton(onClick = action) { Text(label) } } },
            )

            if (isSetup) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { c.prefs.setupDone = true; onSetupDone() }, modifier = Modifier.fillMaxWidth()) { Text("Done") }
                }
            }
        }
    }
}

@Composable
private fun Row(title: String, ok: Boolean, onFix: () -> Unit) {
    ListItem(
        headlineContent = { Text(title, style = MaterialTheme.typography.titleMedium) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                StatusTag(ok)
                if (!ok) Text("Needed for reliable reminders")
            }
        },
        trailingContent = { if (!ok) TextButton(onClick = onFix, modifier = Modifier.heightIn(min = 48.dp)) { Text("Allow") } },
    )
}

/**
 * "Allowed" in the taken colour, or "Needs attention" on an amber tag. Amber is too light for text on paper,
 * so the attention tag is dark text on an amber block instead.
 */
@Composable
private fun StatusTag(ok: Boolean) {
    val colors = MaterialTheme.medColors
    if (ok) {
        androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Check, contentDescription = null, tint = colors.taken, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Allowed", style = MaterialTheme.typography.labelLarge, color = colors.taken)
        }
    } else {
        Text(
            "Needs attention",
            style = MaterialTheme.typography.labelLarge,
            color = colors.onAmber,
            modifier = Modifier.background(colors.amber, LabelShape).padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

private fun Context.packageUri(): Uri = Uri.parse("package:$packageName")

private fun Context.open(intent: Intent) {
    try {
        startActivity(intent)
    } catch (e: android.content.ActivityNotFoundException) {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri()))
    }
}

private fun Context.openHealthConnectStore() =
    open(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.healthdata")))

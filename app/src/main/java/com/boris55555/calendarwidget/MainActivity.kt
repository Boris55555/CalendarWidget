package com.boris55555.calendarwidget

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import android.provider.CalendarContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.boris55555.calendarwidget.ui.theme.CalendarWidgetTheme
import java.util.Calendar

class MainActivity : ComponentActivity() {

    private var isPermissionGrantedState = mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        isPermissionGrantedState.value = isGranted
        if (isGranted) {
            updateWidget()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        isPermissionGrantedState.value = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

        setContent {
            CalendarWidgetTheme {
                val sharedPref = getSharedPreferences("WidgetPrefs", Context.MODE_PRIVATE)
                
                var dayColor by remember { mutableStateOf(sharedPref.getString("day_color", "#000000") ?: "#000000") }
                var eventColor by remember { mutableStateOf(sharedPref.getString("event_color", "#696969") ?: "#696969") }
                var itemSize by remember { mutableFloatStateOf(sharedPref.getFloat("item_size", 14f)) }
                var updateInterval by remember { mutableLongStateOf(sharedPref.getLong("update_interval", 86400000L)) }
                var eventCount by remember { mutableIntStateOf(sharedPref.getInt("event_count", 5)) }
                var selectedCalendarPackage by remember { mutableStateOf(sharedPref.getString("calendar_package", "") ?: "") }

                val isGranted by isPermissionGrantedState
                val calendarApps = remember { getCalendarApps() }

                fun autoSave() {
                    sharedPref.edit().apply {
                        putString("day_color", dayColor)
                        putString("event_color", eventColor)
                        putFloat("item_size", itemSize)
                        putLong("update_interval", updateInterval)
                        putInt("event_count", eventCount)
                        putString("calendar_package", selectedCalendarPackage)
                        apply()
                    }
                    scheduleWidgetUpdate(this@MainActivity, updateInterval)
                    updateWidget()
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .padding(16.dp)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(text = "Widget Settings", style = MaterialTheme.typography.headlineMedium)
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        PermissionStatusIndicator(isGranted) {
                            requestPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(text = "Colors", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        ColorInput(label = "Date Color (Hex)", value = dayColor) { 
                            dayColor = it
                            if (isValidHex(it)) autoSave()
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        ColorInput(label = "Event Text Color (Hex)", value = eventColor) { 
                            eventColor = it
                            if (isValidHex(it)) autoSave()
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(text = "Font Size (6 - 30 sp)", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        FontSizeSlider(
                            label = "Event List", 
                            value = itemSize, 
                            onValueChange = { itemSize = it },
                            onValueChangeFinished = { autoSave() }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(text = "Number of Events", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        EventCountSlider(
                            value = eventCount, 
                            onValueChange = { eventCount = it },
                            onValueChangeFinished = { autoSave() }
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(text = "Update Interval", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        UpdateFrequencySelector(updateInterval) { 
                            updateInterval = it
                            autoSave()
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { updateWidget() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Update Now")
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(text = "Open Calendar App", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        CalendarAppSelector(calendarApps, selectedCalendarPackage) { 
                            selectedCalendarPackage = it
                            autoSave()
                        }

                        Spacer(modifier = Modifier.height(40.dp))
                        Text(
                            text = "Settings are saved automatically.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }

    private fun getCalendarApps(): List<AppInfo> {
        val pm = packageManager
        val apps = mutableListOf<AppInfo>()
        
        val mainIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR)
        val info1 = pm.queryIntentActivities(mainIntent, 0)
        info1.forEach { apps.add(AppInfo(it.loadLabel(pm).toString(), it.activityInfo.packageName)) }

        val viewIntent = Intent(Intent.ACTION_VIEW).setData(CalendarContract.CONTENT_URI)
        val info2 = pm.queryIntentActivities(viewIntent, 0)
        info2.forEach { apps.add(AppInfo(it.loadLabel(pm).toString(), it.activityInfo.packageName)) }
        
        val eventIntent = Intent(Intent.ACTION_VIEW).setType("vnd.android.cursor.item/event")
        val info3 = pm.queryIntentActivities(eventIntent, 0)
        info3.forEach { apps.add(AppInfo(it.loadLabel(pm).toString(), it.activityInfo.packageName)) }

        return apps.distinctBy { it.packageName }.sortedBy { it.name }
    }

    private fun updateWidget() {
        val intent = Intent(this, CalendarWidgetProvider::class.java)
        intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        val ids = AppWidgetManager.getInstance(application)
            .getAppWidgetIds(ComponentName(application, CalendarWidgetProvider::class.java))
        if (ids.isNotEmpty()) {
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            sendBroadcast(intent)
            AppWidgetManager.getInstance(this).notifyAppWidgetViewDataChanged(ids, R.id.calendar_listview)
        }
    }

    private fun scheduleWidgetUpdate(context: Context, intervalMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, CalendarWidgetProvider::class.java).apply {
            action = "com.boris55555.calendarwidget.ACTION_AUTO_UPDATE"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)

        if (intervalMillis == 86400000L) {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        } else {
            alarmManager.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + intervalMillis,
                intervalMillis,
                pendingIntent
            )
        }
    }
}
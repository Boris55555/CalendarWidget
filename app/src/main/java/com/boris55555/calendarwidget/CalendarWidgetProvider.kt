package com.boris55555.calendarwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews

class CalendarWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "com.boris55555.calendarwidget.ACTION_AUTO_UPDATE") {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, CalendarWidgetProvider::class.java)
            )
            onUpdate(context, appWidgetManager, appWidgetIds)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.calendar_listview)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            
            val sharedPref = context.getSharedPreferences("WidgetPrefs", Context.MODE_PRIVATE)
            val calendarPackage = sharedPref.getString("calendar_package", "") ?: ""

            if (calendarPackage.isNotEmpty()) {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(calendarPackage)
                if (launchIntent != null) {
                    val pendingIntent = PendingIntent.getActivity(
                        context, 
                        appWidgetId, 
                        launchIntent, 
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    // Asetetaan klikkaus taustalle ja tyhjälle näkymälle
                    views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                    views.setOnClickPendingIntent(R.id.empty_view, pendingIntent)
                    
                    // Listan kohdalla käytetään templatea (tämä on välttämätöntä vakauden vuoksi)
                    views.setPendingIntentTemplate(R.id.calendar_listview, pendingIntent)
                }
            }

            // Asetetaan adapteri
            val intent = Intent(context, CalendarWidgetService::class.java)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            intent.data = Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME))
            views.setRemoteAdapter(R.id.calendar_listview, intent)

            views.setEmptyView(R.id.calendar_listview, R.id.empty_view)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
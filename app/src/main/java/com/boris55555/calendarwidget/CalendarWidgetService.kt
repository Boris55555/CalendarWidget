package com.boris55555.calendarwidget

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Color
import android.provider.CalendarContract
import android.util.TypedValue
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import java.text.SimpleDateFormat
import java.util.*

class CalendarWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return CalendarRemoteViewsFactory(this.applicationContext)
    }
}

class CalendarRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private var eventList: List<CalendarEvent> = listOf()
    private var dayColor: Int = Color.BLACK
    private var eventColor: Int = Color.DKGRAY
    private var itemFontSize: Float = 14f
    private var eInkMode: Boolean = false

    data class CalendarEvent(val day: String, val title: String)

    override fun onCreate() {}

    override fun onDataSetChanged() {
        val sharedPref = context.getSharedPreferences("WidgetPrefs", Context.MODE_PRIVATE)
        eInkMode = sharedPref.getBoolean("eink_mode", false)
        itemFontSize = sharedPref.getFloat("item_size", 14f)
        val eventCountLimit = sharedPref.getInt("event_count", 5)
        
        if (eInkMode) {
            dayColor = Color.BLACK
            eventColor = Color.BLACK
        } else {
            val dayColorHex = sharedPref.getString("day_color", "#000000") ?: "#000000"
            val eventColorHex = sharedPref.getString("event_color", "#696969") ?: "#696969"
            dayColor = try { Color.parseColor(dayColorHex) } catch (e: Exception) { Color.BLACK }
            eventColor = try { Color.parseColor(eventColorHex) } catch (e: Exception) { Color.DKGRAY }
        }

        val events = mutableListOf<CalendarEvent>()
        val now = Calendar.getInstance()
        val startMillis = now.timeInMillis

        val endCalendar = Calendar.getInstance()
        endCalendar.add(Calendar.YEAR, 1)
        val endMillis = endCalendar.timeInMillis

        val projection = arrayOf(
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.TITLE
        )

        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        android.content.ContentUris.appendId(builder, startMillis)
        android.content.ContentUris.appendId(builder, endMillis)

        val cursor: Cursor? = context.contentResolver.query(
            builder.build(),
            projection,
            null,
            null,
            CalendarContract.Instances.BEGIN + " ASC"
        )

        cursor?.use {
            val beginIndex = it.getColumnIndex(CalendarContract.Instances.BEGIN)
            val titleIndex = it.getColumnIndex(CalendarContract.Instances.TITLE)
            val dayFormat = SimpleDateFormat("dd.MM", Locale.getDefault())

            var count = 0
            while (it.moveToNext() && count < eventCountLimit) {
                val begin = it.getLong(beginIndex)
                val title = it.getString(titleIndex)
                val day = dayFormat.format(Date(begin))
                events.add(CalendarEvent(day, title))
                count++
            }
        }
        eventList = events
    }

    override fun onDestroy() {}

    override fun getCount(): Int = eventList.size

    override fun getViewAt(position: Int): RemoteViews {
        // Valitaan asettelu E-Ink-tilan mukaan
        val layoutId = if (eInkMode) R.layout.widget_item_eink else R.layout.widget_item
        val views = RemoteViews(context.packageName, layoutId)
        
        if (position < eventList.size) {
            val event = eventList[position]
            views.setTextViewText(R.id.event_day, event.day)
            views.setTextColor(R.id.event_day, dayColor)
            views.setTextViewTextSize(R.id.event_day, TypedValue.COMPLEX_UNIT_SP, itemFontSize)
            
            views.setTextViewText(R.id.event_title, event.title)
            views.setTextColor(R.id.event_title, eventColor)
            views.setTextViewTextSize(R.id.event_title, TypedValue.COMPLEX_UNIT_SP, itemFontSize)
            
            val fillInIntent = Intent()
            views.setOnClickFillInIntent(R.id.widget_item_root, fillInIntent)
        }
        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 2 // Nyt meillä on kaksi eri asettelua
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = true
}
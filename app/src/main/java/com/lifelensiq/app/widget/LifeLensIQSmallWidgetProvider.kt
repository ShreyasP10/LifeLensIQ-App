package com.lifelensiq.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.lifelensiq.app.R
import com.lifelensiq.app.sync.SyncScheduler

/** 2x2 compact home-screen widget: today's stats + sync button. */
class LifeLensIQSmallWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id ->
            WidgetRenderer.render(context, manager, id, R.layout.widget_lifelensiq_small)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            LifeLensIQWidgetProvider.ACTION_SYNC -> {
                SyncScheduler.enqueue(context)
                refreshAll(context)
            }
            LifeLensIQWidgetProvider.ACTION_REFRESH -> refreshAll(context)
        }
    }

    private fun refreshAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(
            ComponentName(context, LifeLensIQSmallWidgetProvider::class.java)
        )
        ids.forEach { id -> WidgetRenderer.render(context, manager, id, R.layout.widget_lifelensiq_small) }
    }
}
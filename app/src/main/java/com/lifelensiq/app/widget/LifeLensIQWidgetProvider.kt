package com.lifelensiq.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.lifelensiq.app.R
import com.lifelensiq.app.sync.SyncScheduler

/** 4x4 home-screen widget: today's stats + sync button. */
class LifeLensIQWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id ->
            WidgetRenderer.render(context, manager, id, R.layout.widget_lifelensiq)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_SYNC -> {
                SyncScheduler.enqueue(context)
                refreshAll(context, R.layout.widget_lifelensiq)
            }
            ACTION_REFRESH -> refreshAll(context, R.layout.widget_lifelensiq)
        }
    }

    private fun refreshAll(context: Context, layoutId: Int) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(
            ComponentName(context, LifeLensIQWidgetProvider::class.java)
        )
        ids.forEach { id -> WidgetRenderer.render(context, manager, id, layoutId) }
    }

    companion object {
        const val ACTION_SYNC = "com.lifelensiq.app.action.WIDGET_SYNC"
        const val ACTION_REFRESH = "com.lifelensiq.app.action.WIDGET_REFRESH"

        /** Ask every widget instance to re-render (called after app data changes). */
        fun refresh(context: Context) {
            context.sendBroadcast(
                Intent(context, LifeLensIQWidgetProvider::class.java).setAction(ACTION_REFRESH)
            )
            context.sendBroadcast(
                Intent(context, LifeLensIQSmallWidgetProvider::class.java).setAction(ACTION_REFRESH)
            )
        }
    }
}
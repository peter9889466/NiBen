package com.niben.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.niben.app.MainActivity
import com.niben.app.R
import com.niben.app.data.ContentCategory
import com.niben.app.data.NibenDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NibenAppWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisAppWidget = ComponentName(context.packageName, NibenAppWidget::class.java.name)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget)
            val specificId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

            if (specificId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                updateAppWidget(context, appWidgetManager, specificId)
            } else {
                for (id in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, id)
                }
            }
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.niben.app.widget.ACTION_REFRESH"

        private val CATEGORY_LABEL = mapOf(
            ContentCategory.HIRAGANA to "히라가나",
            ContentCategory.KATAKANA to "가타카나",
            ContentCategory.KANJI to "한자",
            ContentCategory.VOCAB to "단어",
            ContentCategory.SENTENCE to "문장"
        )

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                val dao = NibenDatabase.getInstance(context).contentDao()
                val item = dao.getRandomItem()

                val views = RemoteViews(context.packageName, R.layout.niben_app_widget)

                if (item != null) {
                    val catLabel = CATEGORY_LABEL[item.category] ?: item.category.name
                    views.setTextViewText(R.id.widget_category, "🇯🇵 $catLabel")
                    views.setTextViewText(R.id.widget_japanese_text, item.japaneseText)

                    if (item.reading.isNotEmpty() && item.reading != item.japaneseText) {
                        views.setViewVisibility(R.id.widget_reading, View.VISIBLE)
                        views.setTextViewText(R.id.widget_reading, item.reading)
                    } else {
                        views.setViewVisibility(R.id.widget_reading, View.GONE)
                    }

                    views.setTextViewText(R.id.widget_meaning, item.meaningKo)

                    if (item.level != null) {
                        views.setViewVisibility(R.id.widget_level, View.VISIBLE)
                        views.setTextViewText(R.id.widget_level, "N${6 - item.level}")
                    } else {
                        views.setViewVisibility(R.id.widget_level, View.GONE)
                    }
                } else {
                    views.setTextViewText(R.id.widget_category, "🇯🇵 NiBen")
                    views.setTextViewText(R.id.widget_japanese_text, "단어 불러오는 중...")
                    views.setViewVisibility(R.id.widget_reading, View.GONE)
                    views.setTextViewText(R.id.widget_meaning, "앱을 실행해 데이터를 준비하세요.")
                    views.setViewVisibility(R.id.widget_level, View.GONE)
                }

                // 1. 위젯 전체 탭 시 앱 메인 실행
                val appIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val appPendingIntent = PendingIntent.getActivity(
                    context,
                    appWidgetId,
                    appIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, appPendingIntent)

                // 2. 새로고침 버튼 탭 시 브로드캐스트로 단어 갱신
                val refreshIntent = Intent(context, NibenAppWidget::class.java).apply {
                    action = ACTION_REFRESH
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                val refreshPendingIntent = PendingIntent.getBroadcast(
                    context,
                    appWidgetId + 1000,
                    refreshIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_btn_refresh, refreshPendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
}

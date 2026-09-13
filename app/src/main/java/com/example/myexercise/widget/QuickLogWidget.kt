package com.example.myexercise.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myexercise.data.local.AppDatabase
import com.example.myexercise.data.repository.ExerciseRepository
import com.example.myexercise.ui.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class QuickLogWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(Color(0xFFFFFFFF))
                        .cornerRadius(16.dp)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔥 1タップ自重トレ",
                            style = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(Color(0xFF1E1E1E))
                            ),
                            modifier = GlanceModifier.defaultWeight().clickable(actionStartActivity<MainActivity>())
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(8.dp))

                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        WidgetActionButton(
                            title = "スクワット",
                            count = "+10回",
                            typeId = 1L,
                            typeName = "スクワット",
                            defaultCount = 10,
                            unit = "回",
                            color = Color(0xFFE8F5E9),
                            textColor = Color(0xFF2E7D32)
                        )
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        WidgetActionButton(
                            title = "腕立て伏せ",
                            count = "+10回",
                            typeId = 2L,
                            typeName = "腕立て伏せ",
                            defaultCount = 10,
                            unit = "回",
                            color = Color(0xFFE3F2FD),
                            textColor = Color(0xFF1565C0)
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(6.dp))

                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        WidgetActionButton(
                            title = "腹筋",
                            count = "+15回",
                            typeId = 3L,
                            typeName = "腹筋",
                            defaultCount = 15,
                            unit = "回",
                            color = Color(0xFFFFF3E0),
                            textColor = Color(0xFFE65100)
                        )
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        WidgetActionButton(
                            title = "ストレッチ",
                            count = "+60秒",
                            typeId = 4L,
                            typeName = "ストレッチ",
                            defaultCount = 60,
                            unit = "秒",
                            color = Color(0xFFF3E5F5),
                            textColor = Color(0xFF7B1FA2)
                        )
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun androidx.glance.layout.RowScope.WidgetActionButton(
    title: String,
    count: String,
    typeId: Long,
    typeName: String,
    defaultCount: Int,
    unit: String,
    color: Color,
    textColor: Color
) {
    val params = actionParametersOf(
        ActionCallbackParams.TYPE_ID to typeId,
        ActionCallbackParams.TYPE_NAME to typeName,
        ActionCallbackParams.DEFAULT_COUNT to defaultCount,
        ActionCallbackParams.UNIT to unit
    )

    Box(
        modifier = GlanceModifier
            .defaultWeight()
            .background(color)
            .cornerRadius(10.dp)
            .padding(vertical = 6.dp, horizontal = 4.dp)
            .clickable(actionRunCallback<LogExerciseCallback>(parameters = params)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(textColor)
                )
            )
            Text(
                text = count,
                style = TextStyle(
                    fontSize = 10.sp,
                    color = ColorProvider(textColor)
                )
            )
        }
    }
}

object ActionCallbackParams {
    val TYPE_ID = ActionParameters.Key<Long>("type_id")
    val TYPE_NAME = ActionParameters.Key<String>("type_name")
    val DEFAULT_COUNT = ActionParameters.Key<Int>("default_count")
    val UNIT = ActionParameters.Key<String>("unit")
}

class LogExerciseCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val typeId = parameters[ActionCallbackParams.TYPE_ID] ?: return
        val typeName = parameters[ActionCallbackParams.TYPE_NAME] ?: return
        val count = parameters[ActionCallbackParams.DEFAULT_COUNT] ?: 10
        val unit = parameters[ActionCallbackParams.UNIT] ?: "回"

        withContext(Dispatchers.IO) {
            val db = AppDatabase.getInstance(context)
            val repository = ExerciseRepository(db.exerciseDao())
            db.exerciseDao().logExerciseAndRecalculate(
                typeId = typeId,
                typeName = typeName,
                count = count,
                unit = unit,
                date = repository.getTodayString()
            )

            // Also write to Health Connect from widget
            val hc = com.example.myexercise.data.health.HealthConnectManager(context)
            if (hc.isAvailable() && hc.hasWritePermission()) {
                val duration = if (unit == "秒") count else 60
                val iconKey = when (typeId) {
                    1L -> "squat"
                    2L -> "pushup"
                    3L -> "situp"
                    4L -> "stretch"
                    else -> "other"
                }
                hc.writeExerciseSession(
                    title = "$typeName +$count$unit",
                    iconKey = iconKey,
                    durationSeconds = duration
                )
            }
        }

        QuickLogWidget().updateAll(context)
    }
}

class QuickLogWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickLogWidget()
}

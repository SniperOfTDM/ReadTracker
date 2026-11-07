package com.sniperoftdm.booktracker.ui.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.color.ColorProviders
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
import com.sniperoftdm.booktracker.R
import com.sniperoftdm.booktracker.data.local.BookTrackerDatabase
import com.sniperoftdm.booktracker.data.model.BookStatus
import com.sniperoftdm.booktracker.domain.StatsCalculator
import com.sniperoftdm.booktracker.ui.log.QuickLogActivity
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId

class BookTrackerWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = BookTrackerDatabase.getInstance(context)
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)

        val readingBooks = database.bookDao().observeByStatus(BookStatus.READING).first()
        val allSessions = database.sessionDao().observeAll().first()

        val sessionsByBook = allSessions.groupBy { it.bookId }
        val calculator = StatsCalculator()

        // Find the most recently logged reading book, or fallback to first reading book
        val targetBook = readingBooks.maxByOrNull { book ->
            val bookSessions = sessionsByBook[book.id] ?: emptyList()
            bookSessions.maxOfOrNull { it.timestamp } ?: book.createdAt
        }

        val allBooks = database.bookDao().observeAll().first()
        val globalDaily = calculator.globalPagesPerDay(allBooks, sessionsByBook, zone)
        val pagesToday = globalDaily[today] ?: 0

        val targetStats = targetBook?.let {
            calculator.computeBookStats(it, sessionsByBook[it.id] ?: emptyList(), today, zone)
        }

        val widgetBackground = ColorProvider(
            day = Color(0xFFF6EFE6),
            night = Color(0xFF2B2420)
        )
        val widgetInk = ColorProvider(
            day = Color(0xFF3A2F2A),
            night = Color(0xFFF1E8DE)
        )
        val widgetSecondary = ColorProvider(
            day = Color(0xFF7A6A60),
            night = Color(0xFFB9ABA0)
        )
        val widgetPrimary = ColorProvider(
            day = Color(0xFF2F6B5E),
            night = Color(0xFF4FA08D)
        )
        val widgetSage = ColorProvider(
            day = Color(0xFFBFD8C8),
            night = Color(0xFF8FC4AC)
        )

        provideContent {
            GlanceTheme {
                val bookIdKey = ActionParameters.Key<Long>(QuickLogActivity.EXTRA_BOOK_ID)
                val actionParams = if (targetBook != null) {
                    actionParametersOf(bookIdKey to targetBook.id)
                } else {
                    actionParametersOf()
                }
                val openQuickLog = actionStartActivity<QuickLogActivity>(actionParams)

                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(widgetBackground)
                        .cornerRadius(24.dp)
                        .padding(16.dp)
                        .clickable(openQuickLog),
                    contentAlignment = Alignment.Center
                ) {
                    if (targetBook == null || targetStats == null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = context.getString(R.string.widget_empty_title),
                                style = TextStyle(
                                    color = widgetInk,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = GlanceModifier.height(4.dp))
                            Text(
                                text = context.getString(R.string.widget_empty_subtitle),
                                style = TextStyle(
                                    color = widgetSecondary,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    } else {
                        Column(modifier = GlanceModifier.fillMaxSize()) {
                            Row(
                                modifier = GlanceModifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = GlanceModifier.defaultWeight()) {
                                    Text(
                                        text = targetBook.title,
                                        maxLines = 1,
                                        style = TextStyle(
                                            color = widgetInk,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = "${targetStats.currentPage} / ${targetBook.totalPages} p.",
                                        style = TextStyle(
                                            color = widgetSecondary,
                                            fontSize = 12.sp
                                        )
                                    )
                                }

                                Box(
                                    modifier = GlanceModifier
                                        .background(widgetPrimary)
                                        .cornerRadius(20.dp)
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                        .clickable(openQuickLog),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "+",
                                        style = TextStyle(
                                            color = ColorProvider(day = Color.White, night = Color(0xFF0F322B)),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = GlanceModifier.height(10.dp))

                            LinearProgressIndicator(
                                progress = targetStats.progressPercent,
                                color = widgetPrimary,
                                backgroundColor = widgetSage,
                                modifier = GlanceModifier.fillMaxWidth().height(8.dp).cornerRadius(4.dp)
                            )

                            Spacer(modifier = GlanceModifier.height(8.dp))

                            Text(
                                text = context.getString(R.string.widget_today_read, pagesToday),
                                style = TextStyle(
                                    color = widgetPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        suspend fun updateAll(context: Context) {
            try {
                BookTrackerWidget().updateAll(context)
            } catch (_: Exception) {}
        }
    }
}

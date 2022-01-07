package com.emmav.monzo.widget.feature.appwidget

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.emmav.monzo.widget.R
import com.emmav.monzo.widget.common.TypefaceSpan
import com.emmav.monzo.widget.common.toPx
import com.emmav.monzo.widget.data.appwidget.Widget
import com.emmav.monzo.widget.data.appwidget.WidgetRepository
import com.emmav.monzo.widget.feature.settings.SettingsActivity
import dagger.hilt.android.AndroidEntryPoint
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*
import javax.inject.Inject

const val EXTRA_WIDGET_TYPE_ID = "EXTRA_ID"

@AndroidEntryPoint
class WidgetProvider : AppWidgetProvider() {
    @Inject lateinit var widgetRepository: WidgetRepository

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateAllWidgets(context, appWidgetManager, widgetRepository)
    }

    @SuppressLint("CheckResult")
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        widgetRepository.deleteRemovedWidgets(widgetIds = appWidgetIds.toList()).blockingGet()
    }

    companion object {
        private const val ROBOTO_LIGHT = "sans-serif-light"
        private const val ROBOTO_REGULAR = "sans-serif"
        private val numberFormat = NumberFormat.getCurrencyInstance().apply {
            maximumFractionDigits = 0
        }

        @SuppressLint("CheckResult")
        fun updateAllWidgets(context: Context, appWidgetManager: AppWidgetManager, widgetRepository: WidgetRepository) {
            val thisWidget = ComponentName(context, WidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

            widgetRepository.deleteRemovedWidgets(widgetIds = allWidgetIds.toList()).blockingGet()

            for (i in allWidgetIds) {
                updateWidget(
                    context,
                    i,
                    appWidgetManager,
                    widgetRepository
                )
            }
        }

        fun updateWidget(
            context: Context,
            widgetId: Int,
            appWidgetManager: AppWidgetManager,
            widgetRepository: WidgetRepository
        ) {
            val intent = Intent(context, SettingsActivity::class.java)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)

            val it = widgetRepository.widgetById(id = widgetId).blockingGet()
            val textColour = ContextCompat.getColor(context, R.color.monzo_dark)

            if (it is Widget) {
                val currency = Currency.getInstance(it.currency)
                numberFormat.currency = currency

                val balance = numberFormat.format(BigDecimal(it.balance).scaleByPowerOfTen(-2).toBigInteger())
                val spannableString = createSpannableForBalance(context, currency.symbol, balance, textColour)

                intent.putExtra(EXTRA_WIDGET_TYPE_ID, it.widgetTypeId)
                updateWidget(
                    context = context,
                    pendingIntent = PendingIntent.getActivity(
                        context,
                        widgetId,
                        intent,
                        PendingIntent.FLAG_IMMUTABLE,
                    ),
                    amount = spannableString,
                    subtitle = it.toString(),
                    appWidgetManager = appWidgetManager,
                    appWidgetId = widgetId
                )
            }
        }

        private fun updateWidget(
            context: Context,
            pendingIntent: PendingIntent,
            amount: SpannableString,
            subtitle: String,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_balance)
            views.setOnClickPendingIntent(R.id.widgetViewGroup, pendingIntent)
            views.setTextViewText(R.id.widgetAmountTextView, amount)
            views.setTextViewText(R.id.widgetSubtitleTextView, subtitle)
            views.setInt(R.id.widgetBackgroundView, "setBackgroundResource", R.drawable.background_light)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun createSpannableForBalance(
            context: Context,
            currency: String,
            balance: String,
            textColour: Int
        ): SpannableString {
            // Some supremely crude scaling as balance gets larger
            val currencySize = when {
                balance.length < 2 -> 23f
                balance.length == 2 -> 18f
                balance.length == 3 -> 14f
                balance.length == 4 -> 12f
                else -> 9f
            }
            val integerPartSize = when {
                balance.length < 2 -> 35f
                balance.length == 2 -> 27f
                balance.length == 3 -> 23f
                balance.length == 4 -> 18f
                else -> 14f
            }

            val span = SpannableString(balance)
            applyToCurrency(span, currency, AbsoluteSizeSpan(currencySize.toPx(context)))
            applyToCurrency(span, currency, ForegroundColorSpan(textColour))
            applyToCurrency(span, currency, TypefaceSpan(Typeface.create(ROBOTO_LIGHT, Typeface.NORMAL)))

            applyToIntegerPart(span, currency, balance, AbsoluteSizeSpan(integerPartSize.toPx(context)))
            applyToIntegerPart(span, currency, balance, ForegroundColorSpan(textColour))
            applyToIntegerPart(span, currency, balance, TypefaceSpan(Typeface.create(ROBOTO_REGULAR, Typeface.NORMAL)))

            return span
        }

        private fun applyToCurrency(spannable: SpannableString, currency: String, span: Any) {
            spannable.setSpan(span, 0, currency.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        private fun applyToIntegerPart(spannable: SpannableString, currency: String, wholeSting: String, span: Any) {
            spannable.setSpan(span, currency.length, wholeSting.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        }
    }
}
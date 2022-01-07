package com.emmav.monzo.widget.feature.appwidget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.*
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.emmav.monzo.widget.R
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*

const val EXTRA_WIDGET_ID = "WIDGET_ID"
const val EXTRA_WIDGET_TYPE_ID = "EXTRA_ID"

const val KEY_CURRENCY = "currency"
const val KEY_BALANCE = "balance"
const val KEY_NAME = "name"
const val KEY_EMOJI = "emoji"

// TODO: Resizing, dynamic theming from system, add a preview layout
class BalanceWidget : GlanceAppWidget() {
    override var stateDefinition = PreferencesGlanceStateDefinition

    @OptIn(ExperimentalUnitApi::class)
    @Composable override fun Content() {
        val preferences = currentState<Preferences>()
        val currencyCode = preferences[stringPreferencesKey(KEY_CURRENCY)] ?: "GBP"
        val numberFormat = NumberFormat.getCurrencyInstance().apply {
            currency = Currency.getInstance(currencyCode)
            maximumFractionDigits = 0
        }
        val currencySymbol = Currency.getInstance(currencyCode).symbol

        val balanceInUnits = preferences[intPreferencesKey(KEY_BALANCE)] ?: -1
        val balance = numberFormat.format(BigDecimal(balanceInUnits).scaleByPowerOfTen(-2).toBigInteger())
            .removePrefix(prefix = currencySymbol)

        val accountName = preferences[stringPreferencesKey(KEY_NAME)] ?: ""
        val emoji = preferences[stringPreferencesKey(KEY_EMOJI)] ?: ""

        // Some supremely crude scaling as balance gets larger 😬
        val currencySize = when {
            balance.length < 3 -> 23f
            balance.length == 3 -> 18f
            balance.length == 4 -> 14f
            balance.length == 5 -> 12f
            else -> 9f
        }
        val integerPartSize = when {
            balance.length < 3 -> 35f
            balance.length == 3 -> 27f
            balance.length == 4 -> 23f
            balance.length == 5 -> 18f
            else -> 14f
        }

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(R.color.monzo_dark))
                .appWidgetBackground()
                .cornerRadius(4.dp)
                .padding(4.dp)
            // TODO: Uncomment when we can edit widgets again, atm we can't get the glanceId
            //.clickable(actionStartActivity<SettingsActivity>(actionParametersOf(widgetIdParam to "1")))
        ) {
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                Text(
                    text = currencySymbol,
                    style = TextStyle(
                        fontSize = TextUnit(value = currencySize, type = TextUnitType.Sp),
                        color = ColorProvider(R.color.monzo_light)
                    ),
                )
                Text(
                    text = balance,
                    style = TextStyle(
                        fontSize = TextUnit(value = integerPartSize, type = TextUnitType.Sp),
                        color = ColorProvider(R.color.monzo_light)
                    ),
                )
            }
            Text(
                text = "$emoji $accountName",
                modifier = GlanceModifier.fillMaxWidth(),
                style = TextStyle(
                    fontSize = TextUnit(value = 10f, type = TextUnitType.Sp),
                    color = ColorProvider(R.color.monzo_light),
                    textAlign = TextAlign.Center,
                ),
            )
        }
    }
}

class BalanceWidgetProvider : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = BalanceWidget()
}
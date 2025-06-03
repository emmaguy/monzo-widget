package com.emmav.monzowidget.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.emmav.monzowidget.App
import com.emmav.monzowidget.ui.Utils.formatBalance
import com.emmav.monzowidget.ui.Utils.title
import kotlinx.coroutines.flow.first
import java.util.Currency

class BalanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = App.Companion.instance.monzoRepository
        val account = repository.accountsWithPots().first().random()

        provideContent {
            var currencySymbol = Currency.getInstance(account.balance?.currencyCode).symbol
            val balance = account.balance?.formatBalance()
                ?.replace(currencySymbol, "")
                ?.replace("-", "")

            if ((account.balance?.amount?.signum() ?: 0) < 0) {
                currencySymbol = "-$currencySymbol"
            }

            BalanceContent(
                currencySymbol = currencySymbol,
                balance = balance ?: "0.00",
                emoji = account.countryCodeEmoji,
                accountName = account.title(),
            )
        }
    }

    @Composable
    private fun BalanceContent(
        currencySymbol: String,
        balance: String,
        emoji: String,
        accountName: String
    ) {
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
                .background(Color.White)
                .fillMaxWidth()
                .height(60.dp)
                .appWidgetBackground()
                .cornerRadius(8.dp)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(bottom = 1.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = currencySymbol,
                    style = TextStyle(fontSize = currencySize.sp),
                )
                Text(
                    text = balance,
                    style = TextStyle(fontSize = integerPartSize.sp),
                )
            }
            Text(
                text = "$emoji $accountName",
                modifier = GlanceModifier.fillMaxWidth(),
                style = TextStyle(fontSize = 10.sp, textAlign = TextAlign.Center),
            )
        }
    }
}

class BalanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BalanceWidget()
}
package com.emmav.monzo.widget.feature.settings

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.lifecycleScope
import com.emmav.monzo.widget.R
import com.emmav.monzo.widget.common.AppTheme
import com.emmav.monzo.widget.common.assistedViewModel
import com.emmav.monzo.widget.common.resolveText
import com.emmav.monzo.widget.feature.appwidget.*
import com.emmav.monzo.widget.feature.splash.SplashActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {
    private val widgetId by lazy { intent.extras?.getString(EXTRA_WIDGET_ID) }
    private val widgetTypeId by lazy { intent.extras?.getString(EXTRA_WIDGET_TYPE_ID) }

    @Inject lateinit var vmFactory: SettingsViewModel.Factory
    private val viewModel by assistedViewModel {
        vmFactory.create(widgetId, widgetTypeId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setResult(RESULT_CANCELED)
        setContent {
            AppTheme {
                Scaffold(topBar = {
                    TopAppBar(title = { Text(stringResource(R.string.settings_activity_title)) })
                }, content = {
                    val state by viewModel.state.observeAsState(SettingsViewModel.State())
                    if (state.error) {
                        startActivity(SplashActivity.buildIntent(LocalContext.current))
                        finish()
                    }
                    if (state.complete) {
                        finishWidgetSetup()
                    }

                    Content(state = state)
                })
            }
        }
    }

    private fun finishWidgetSetup() {
        val appWidgetId = intent.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        finish()

        val context = this
        lifecycleScope.launch {
            val glanceId = GlanceAppWidgetManager(context).getGlanceIds(BalanceWidget::class.java).last()
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) {
                it.toMutablePreferences().apply {
                    this[intPreferencesKey(KEY_BALANCE)] = 10_30
                    this[stringPreferencesKey(KEY_NAME)] = "Current account"
                    this[stringPreferencesKey(KEY_EMOJI)] = "💳"
                    this[stringPreferencesKey(KEY_CURRENCY)] = "EUR"
                }
            }
            BalanceWidget().update(context, glanceId)
        }
    }

    companion object {

        fun buildIntent(context: Context, widgetId: String, widgetTypeId: String): Intent {
            return Intent(context, SettingsActivity::class.java)
                .putExtra(EXTRA_WIDGET_ID, widgetId)
                .putExtra(EXTRA_WIDGET_TYPE_ID, widgetTypeId)
        }
    }
}

@Composable
private fun Content(
    state: SettingsViewModel.State,
) {
    when {
        state.loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        else -> {
            WidgetTypes(state.rows)
        }
    }
}

@Composable
private fun WidgetTypes(rows: List<Row>) {
    Card(
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(all = 16.dp)
    ) {
        Column {
            Text(
                text = stringResource(R.string.settings_title_widget_type),
                style = TextStyle(fontSize = 22.sp),
                modifier = Modifier.padding(all = 16.dp)
            )
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(rows) { row ->
                    Row(
                        modifier = Modifier
                            .fillParentMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    ) {
                        when (row) {
                            is Row.Header -> {
                                Text(
                                    text = LocalContext.current.resolveText(text = row.title),
                                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.W500),
                                )
                            }
                            is Row.Widget -> {
                                Row(
                                    modifier = Modifier
                                        .fillParentMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = row.title,
                                        style = TextStyle(fontSize = 20.sp),
                                    )
                                    RadioButton(selected = row.isSelected, onClick = { row.click(Unit) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

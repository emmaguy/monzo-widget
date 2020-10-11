package com.emmav.monzo.widget.feature.settings

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Box
import androidx.compose.foundation.ContentGravity
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumnFor
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ContextAmbient
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emmav.monzo.widget.R
import com.emmav.monzo.widget.common.AppTheme
import com.emmav.monzo.widget.common.resolveText
import com.emmav.monzo.widget.data.appwidget.WidgetRepository
import com.emmav.monzo.widget.feature.appwidget.EXTRA_WIDGET_TYPE_ID
import com.emmav.monzo.widget.feature.appwidget.WidgetProvider
import com.emmav.monzo.widget.feature.splash.SplashActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {
    private val appWidgetId by lazy {
        intent.extras!!.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
    }
    private val widgetTypeId by lazy { intent.extras?.getString(EXTRA_WIDGET_TYPE_ID, null) }

    @Inject lateinit var vmFactory: SettingsViewModel.AssistedFactory
    private val viewModel: SettingsViewModel by viewModels {
        SettingsViewModel.provideFactory(vmFactory, appWidgetId, widgetTypeId)
    }
    @Inject lateinit var widgetRepository: WidgetRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setResult(RESULT_CANCELED)
        setContent {
            AppTheme {
                Scaffold(topBar = {
                    TopAppBar(title = { Text(ContextAmbient.current.getString(R.string.settings_activity_title)) })
                }, bodyContent = {
                    val state by viewModel.state.observeAsState(SettingsViewModel.State())
                    if (state.error) {
                        startActivity(SplashActivity.buildIntent(ContextAmbient.current))
                        finish()
                    }
                    if (state.complete) {
                        finishWidgetSetup()
                    }

                    Content(
                        state = state
                    )
                })
            }
        }
    }

    private fun finishWidgetSetup() {
        setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        finish()
        // TODO: Is this needed?
        WidgetProvider.updateWidget(this, appWidgetId, AppWidgetManager.getInstance(this), widgetRepository)
    }

    companion object {

        fun buildIntent(context: Context, appWidgetId: Int, widgetTypeId: String): Intent {
            return Intent(context, SettingsActivity::class.java)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
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
            Box(modifier = Modifier.fillMaxSize(), gravity = ContentGravity.Center) {
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
        modifier = Modifier.fillMaxWidth().padding(all = 16.dp)
    ) {
        Column {
            Text(
                text = ContextAmbient.current.getString(R.string.settings_title_widget_type),
                style = TextStyle(fontSize = 22.sp),
                modifier = Modifier.padding(all = 16.dp)
            )
            LazyColumnFor(items = rows, modifier = Modifier.fillMaxWidth()) { row ->
                Row(modifier = Modifier.fillParentMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    when (row) {
                        is Row.Header -> {
                            Text(
                                text = ContextAmbient.current.resolveText(text = row.title),
                                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.W500),
                            )
                        }
                        is Row.Widget -> {
                            Row(
                                modifier = Modifier.fillParentMaxWidth().padding(horizontal = 16.dp),
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

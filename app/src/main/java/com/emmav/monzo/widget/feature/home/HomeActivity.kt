package com.emmav.monzo.widget.feature.home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emmav.monzo.widget.R
import com.emmav.monzo.widget.common.AppTheme
import com.emmav.monzo.widget.common.EmptyState
import com.emmav.monzo.widget.common.text
import com.emmav.monzo.widget.common.textRes
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                Scaffold(topBar = {
                    TopAppBar(title = { Text(stringResource(R.string.home_activity_title)) })
                }, content = {
                    val state by viewModel.state.observeAsState(HomeViewModel.State())
                    Content(
                        state = state,
                        onWidgetClicked = {
                            // TODO: Implement when we can get the widget id to edit it
//                            startActivity(
//                                SettingsActivity.buildIntent(
//                                    context = this,
//                                    widgetId = it.widgetId,
//                                    widgetTypeId = it.widgetTypeId,
//                                )
//                            )
                        }
                    )
                })
            }
        }
    }

    companion object {
        fun buildIntent(context: Context): Intent {
            return Intent(context, HomeActivity::class.java)
        }
    }
}

@Composable
private fun Content(
    state: HomeViewModel.State,
    onWidgetClicked: (WidgetRow) -> Unit
) {
    when {
        state.loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        state.widgets.isEmpty() -> {
            EmptyState(
                emoji = text("🔜"),
                title = textRes(R.string.home_empty_title),
                subtitle = textRes(R.string.home_empty_subtitle)
            )
        }
        else -> {
            Column {
                WidgetList(widgets = state.widgets, onWidgetClicked = onWidgetClicked)
                Settings()
            }
        }
    }
}

@Composable
private fun Settings() {
    Card(
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(all = 16.dp)
    ) {
        Column {
            Text(
                text = stringResource(R.string.home_title_settings),
                style = TextStyle(fontSize = 22.sp),
                modifier = Modifier.padding(all = 16.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.home_title_refresh_interval),
                        style = TextStyle(fontSize = 20.sp),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    Text(
                        text = stringResource(R.string.home_subtitle_refresh_interval),
                        style = TextStyle(
                            fontSize = 14.sp,
                            color = MaterialTheme.colors.onSecondary.copy(alpha = 0.8f)
                        ),
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetList(
    widgets: List<WidgetRow>,
    onWidgetClicked: (WidgetRow) -> Unit
) {
    Card(
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(all = 16.dp)
    ) {
        Column {
            Text(
                text = stringResource(R.string.home_title_existing_widgets),
                style = TextStyle(fontSize = 22.sp),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)
            )
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(widgets) { widget ->
                    Row(
                        modifier = Modifier
                            .fillParentMaxWidth()
                            .clickable(onClick = { onWidgetClicked.invoke(widget) })
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillParentMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                        ) {
                            Text(
                                text = widget.title,
                                style = TextStyle(fontSize = 20.sp),
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                            Text(
                                text = widget.amount,
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colors.onSecondary.copy(alpha = 0.8f)
                                ),
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun WidgetListPreview() {
    WidgetList(
        widgets = listOf(WidgetRow(title = "hi", amount = "£1.23", widgetId = "1", widgetTypeId = "id1")),
        onWidgetClicked = {}
    )
}
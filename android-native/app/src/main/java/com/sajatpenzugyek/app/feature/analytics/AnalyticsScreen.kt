package com.sajatpenzugyek.app.feature.analytics

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sajatpenzugyek.app.R
import com.sajatpenzugyek.app.core.theme.CategoryColorProvider
import com.sajatpenzugyek.app.core.theme.Emerald500
import com.sajatpenzugyek.app.core.theme.Rose500
import com.sajatpenzugyek.app.core.utils.CurrencyFormatter
import com.sajatpenzugyek.app.domain.model.CategoryBreakdown
import com.sajatpenzugyek.app.feature.dashboard.MonthlyTrendsChartCard
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.atan2
import kotlin.math.sqrt

fun getLocalizedCategoryName(categoryId: String?, defaultName: String, context: Context): String {
    return when (categoryId) {
        "food" -> context.getString(R.string.cat_food)
        "dining" -> context.getString(R.string.cat_dining)
        "transport" -> context.getString(R.string.cat_transport)
        "subscriptions" -> context.getString(R.string.cat_subscriptions)
        "housing" -> context.getString(R.string.cat_housing)
        "entertainment" -> context.getString(R.string.cat_entertainment)
        "savings" -> context.getString(R.string.cat_savings)
        "income" -> context.getString(R.string.cat_income)
        "health" -> context.getString(R.string.cat_health)
        "shopping" -> context.getString(R.string.cat_shopping)
        "transfers" -> context.getString(R.string.cat_transfers)
        "other" -> context.getString(R.string.cat_other)
        else -> defaultName
    }
}

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // Period filter chips
        item {
            PeriodFilterRow(
                currentPeriod = state.period,
                onSelectPeriod = { viewModel.selectPeriod(it) },
                customStartDate = state.customStartDate,
                customEndDate = state.customEndDate,
                onOpenCustomDialog = { viewModel.openCustomDateDialog() }
            )
        }

        // Headline Metrics Grid
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.analytics_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.net_balance), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = CurrencyFormatter.formatSigned(state.stats.balanceMinor, state.currency),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (state.stats.balanceMinor >= 0) Emerald500 else Rose500
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.avg_expense), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = CurrencyFormatter.format(state.stats.avgExpenseMinor, state.currency),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.largest_expense), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val maxName = state.stats.maxExpenseItem.description.ifBlank { "-" }
                        Text(
                            text = if (state.stats.maxExpenseItem.amountMinor > 0) "$maxName: ${CurrencyFormatter.format(state.stats.maxExpenseItem.amountMinor, state.currency)}" else "-",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.transfer_volume), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = CurrencyFormatter.format(state.stats.transfersMinor, state.currency),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Interactive Donut Chart & Category Breakdown
        item {
            SpendingDonutChartCard(
                breakdown = state.categoryBreakdown,
                totalExpenseMinor = state.stats.expenseMinor,
                selectedCategoryId = state.selectedCategoryId,
                currency = state.currency,
                onCategoryClick = { viewModel.toggleCategorySelection(it) }
            )
        }

        // Top Merchants Card
        if (state.topMerchants.isNotEmpty()) {
            item {
                TopMerchantsCard(
                    merchants = state.topMerchants,
                    currency = state.currency
                )
            }
        }

        // Monthly trends bar chart
        if (state.monthlyTrends.isNotEmpty()) {
            item {
                MonthlyTrendsChartCard(trends = state.monthlyTrends)
            }
        }

        item { Spacer(modifier = Modifier.height(88.dp)) }
    }

    // Custom Date Range Dialog
    if (state.showCustomDateDialog) {
        CustomDateRangeDialog(
            initialStart = state.customStartDate,
            initialEnd = state.customEndDate,
            hasError = state.dateValidationError,
            onDismiss = { viewModel.closeCustomDateDialog() },
            onApply = { start, end -> viewModel.applyCustomDateRange(start, end) }
        )
    }
}

@Composable
fun PeriodFilterRow(
    currentPeriod: AnalyticsPeriod,
    onSelectPeriod: (AnalyticsPeriod) -> Unit,
    customStartDate: LocalDate,
    customEndDate: LocalDate,
    onOpenCustomDialog: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = currentPeriod == AnalyticsPeriod.THIS_MONTH,
                onClick = { onSelectPeriod(AnalyticsPeriod.THIS_MONTH) },
                label = { Text(stringResource(R.string.period_this_month)) },
                colors = FilterChipDefaults.filterChipColors()
            )
            FilterChip(
                selected = currentPeriod == AnalyticsPeriod.LAST_MONTH,
                onClick = { onSelectPeriod(AnalyticsPeriod.LAST_MONTH) },
                label = { Text(stringResource(R.string.period_last_month)) }
            )
            FilterChip(
                selected = currentPeriod == AnalyticsPeriod.LAST_3_MONTHS,
                onClick = { onSelectPeriod(AnalyticsPeriod.LAST_3_MONTHS) },
                label = { Text(stringResource(R.string.period_3_months)) }
            )
            FilterChip(
                selected = currentPeriod == AnalyticsPeriod.LAST_6_MONTHS,
                onClick = { onSelectPeriod(AnalyticsPeriod.LAST_6_MONTHS) },
                label = { Text(stringResource(R.string.period_6_months)) }
            )
            FilterChip(
                selected = currentPeriod == AnalyticsPeriod.THIS_YEAR,
                onClick = { onSelectPeriod(AnalyticsPeriod.THIS_YEAR) },
                label = { Text(stringResource(R.string.period_this_year)) }
            )
            FilterChip(
                selected = currentPeriod == AnalyticsPeriod.LAST_YEAR,
                onClick = { onSelectPeriod(AnalyticsPeriod.LAST_YEAR) },
                label = { Text(stringResource(R.string.period_last_year)) }
            )
            FilterChip(
                selected = currentPeriod == AnalyticsPeriod.CUSTOM,
                onClick = { onSelectPeriod(AnalyticsPeriod.CUSTOM) },
                label = { Text(stringResource(R.string.period_custom)) }
            )
        }

        if (currentPeriod == AnalyticsPeriod.CUSTOM) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenCustomDialog() }
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${customStartDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}  –  ${customEndDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                IconButton(onClick = onOpenCustomDialog, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = stringResource(R.string.custom_date_range),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SpendingDonutChartCard(
    breakdown: List<CategoryBreakdown>,
    totalExpenseMinor: Long,
    selectedCategoryId: String?,
    currency: String,
    onCategoryClick: (String) -> Unit
) {
    val context = LocalContext.current
    val selectedItem = breakdown.find { it.categoryId == selectedCategoryId }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.spending_distribution),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (selectedCategoryId != null) {
                    Text(
                        text = stringResource(R.string.tap_to_filter),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (breakdown.isEmpty() || totalExpenseMinor <= 0L) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_analytics_data),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Interactive Compose Canvas Donut Chart
                val slices = remember(breakdown, totalExpenseMinor) {
                    var currentStart = -90f
                    breakdown.map { item ->
                        val sweep = (item.totalMinor.toFloat() / totalExpenseMinor.toFloat()) * 360f
                        val sliceData = Triple(item, currentStart, sweep)
                        currentStart += sweep
                        sliceData
                    }
                }

                Box(
                    modifier = Modifier.size(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(slices) {
                                detectTapGestures { offset ->
                                    val cx = size.width / 2f
                                    val cy = size.height / 2f
                                    val dx = offset.x - cx
                                    val dy = offset.y - cy
                                    val dist = sqrt(dx * dx + dy * dy)
                                    val outerRadius = size.width / 2f

                                    // Inner hole ~ 50% radius
                                    if (dist < outerRadius * 0.45f) {
                                        // Tap in center clears selection
                                        if (selectedCategoryId != null) {
                                            onCategoryClick(selectedCategoryId)
                                        }
                                    } else if (dist <= outerRadius * 1.1f) {
                                        // Calculate angle in degrees [0, 360) starting from -90° (top)
                                        var angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                        // Adjust to start at -90
                                        var touchAngle = (angleDeg + 90f + 360f) % 360f

                                        for ((cat, start, sweep) in slices) {
                                            val normalizedStart = (start + 90f + 360f) % 360f
                                            val sliceEnd = normalizedStart + sweep
                                            if (sliceEnd <= 360f) {
                                                if (touchAngle >= normalizedStart && touchAngle < sliceEnd) {
                                                    onCategoryClick(cat.categoryId)
                                                    break
                                                }
                                            } else {
                                                // Wraps around 360
                                                if (touchAngle >= normalizedStart || touchAngle < (sliceEnd % 360f)) {
                                                    onCategoryClick(cat.categoryId)
                                                    break
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                    ) {
                        val strokeWidth = 32.dp.toPx()
                        val diameter = size.minDimension - strokeWidth
                        val topLeftOffset = androidx.compose.ui.geometry.Offset(strokeWidth / 2f, strokeWidth / 2f)
                        val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)

                        slices.forEach { (cat, start, sweep) ->
                            val isSelected = cat.categoryId == selectedCategoryId
                            val color = CategoryColorProvider.getColor(cat.categoryId, cat.color)
                            val currentStroke = if (isSelected) strokeWidth * 1.25f else strokeWidth

                            drawArc(
                                color = color,
                                startAngle = start + 0.75f,
                                sweepAngle = (sweep - 1.5f).coerceAtLeast(0.5f),
                                useCenter = false,
                                topLeft = topLeftOffset,
                                size = arcSize,
                                style = Stroke(width = currentStroke, cap = StrokeCap.Round)
                            )
                        }
                    }

                    // Centered Hole Text
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(24.dp)
                            .clickable {
                                if (selectedCategoryId != null) onCategoryClick(selectedCategoryId)
                            }
                    ) {
                        if (selectedItem != null) {
                            val catColor = CategoryColorProvider.getColor(selectedItem.categoryId, selectedItem.color)
                            Text(
                                text = getLocalizedCategoryName(selectedItem.categoryId, selectedItem.categoryName, context),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = CurrencyFormatter.format(selectedItem.totalMinor, currency),
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${"%.1f".format(selectedItem.percentage)}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = catColor
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.expenses),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = CurrencyFormatter.format(totalExpenseMinor, currency),
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.tap_to_filter),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Category Breakdown list with tap selection
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    breakdown.forEach { item ->
                        val isSelected = item.categoryId == selectedCategoryId
                        val color = CategoryColorProvider.getColor(item.categoryId, item.color)
                        val name = getLocalizedCategoryName(item.categoryId, item.categoryName, context)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    else Color.Transparent
                                )
                                .clickable { onCategoryClick(item.categoryId) }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                    Text(
                                        text = "${item.transactionCount} tranzakció • ${"%.1f".format(item.percentage)}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Text(
                                text = CurrencyFormatter.format(item.totalMinor, currency),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TopMerchantsCard(
    merchants: List<com.sajatpenzugyek.app.feature.analytics.MerchantSpending>,
    currency: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.top_merchants),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            merchants.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${index + 1}.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(24.dp)
                        )
                        Column {
                            Text(
                                text = item.merchant,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${item.count} tranzakció",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = CurrencyFormatter.format(item.totalMinor, currency),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun CustomDateRangeDialog(
    initialStart: LocalDate,
    initialEnd: LocalDate,
    hasError: Boolean,
    onDismiss: () -> Unit,
    onApply: (LocalDate, LocalDate) -> Unit
) {
    var startStr by remember { mutableStateOf(initialStart.format(DateTimeFormatter.ISO_LOCAL_DATE)) }
    var endStr by remember { mutableStateOf(initialEnd.format(DateTimeFormatter.ISO_LOCAL_DATE)) }
    var parseError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.custom_date_range)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = startStr,
                    onValueChange = {
                        startStr = it
                        parseError = false
                    },
                    label = { Text(stringResource(R.string.start_date) + " (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = endStr,
                    onValueChange = {
                        endStr = it
                        parseError = false
                    },
                    label = { Text(stringResource(R.string.end_date) + " (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (hasError || parseError) {
                    Text(
                        text = stringResource(R.string.invalid_date_range),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    try {
                        val start = LocalDate.parse(startStr.trim())
                        val end = LocalDate.parse(endStr.trim())
                        onApply(start, end)
                    } catch (_: Exception) {
                        parseError = true
                    }
                }
            ) {
                Text(stringResource(R.string.apply))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

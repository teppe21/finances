package com.sajatpenzugyek.app.feature.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sajatpenzugyek.app.R
import com.sajatpenzugyek.app.core.theme.CategoryColorProvider
import com.sajatpenzugyek.app.domain.model.Category

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    viewModel: CategoriesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val keywordExistsMsg = stringResource(R.string.keyword_already_exists)
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(keywordExistsMsg)
            viewModel.clearErrorMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddCategory() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_category))
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            item {
                Text(
                    text = stringResource(R.string.categories),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            items(uiState.categories, key = { it.category.id }) { catWithRules ->
                val category = catWithRules.category
                val rules = catWithRules.rules
                val catColor = CategoryColorProvider.getColor(category)
                val displayName = category.getDisplayName(context)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(catColor)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (category.isDefault) stringResource(R.string.system_category) else stringResource(R.string.custom_category),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (!category.isDefault) {
                                Row {
                                    IconButton(
                                        onClick = { viewModel.openEditCategory(category) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = stringResource(R.string.edit_category),
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.requestDeleteCategory(category) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.delete_category),
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = stringResource(R.string.category_keywords),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rules.forEach { rule ->
                                InputChip(
                                    selected = false,
                                    onClick = {},
                                    label = { Text(rule.pattern, style = MaterialTheme.typography.labelSmall) },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove ${rule.pattern}",
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable { viewModel.deleteKeyword(rule.id) }
                                        )
                                    },
                                    colors = InputChipDefaults.inputChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                    )
                                )
                            }

                            SuggestionChip(
                                onClick = { viewModel.openAddKeyword(category) },
                                label = { Text("+ " + stringResource(R.string.add_keyword), style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(88.dp)) }
        }
    }

    // Dialog: Add Category
    if (uiState.showAddCategoryDialog) {
        var name by remember { mutableStateOf("") }
        var selectedColor by remember { mutableStateOf(CategoryColorProvider.PRESET_PALETTE.first()) }
        var initialKeyword by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { viewModel.closeAddCategory() },
            title = { Text(stringResource(R.string.add_category)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.category_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = stringResource(R.string.category_color),
                        style = MaterialTheme.typography.labelMedium
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CategoryColorProvider.PRESET_PALETTE.forEach { hexColor ->
                            val color = CategoryColorProvider.getColor(null, hexColor)
                            val isSelected = selectedColor.equals(hexColor, ignoreCase = true)

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = hexColor },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = initialKeyword,
                        onValueChange = { initialKeyword = it },
                        label = { Text(stringResource(R.string.keyword_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.addCategory(name, selectedColor, initialKeyword.ifBlank { null })
                        }
                    },
                    enabled = name.isNotBlank()
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.closeAddCategory() }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Dialog: Edit Category
    uiState.editingCategory?.let { category ->
        var name by remember(category) { mutableStateOf(category.name) }
        var selectedColor by remember(category) { mutableStateOf(category.color ?: CategoryColorProvider.PRESET_PALETTE.first()) }

        AlertDialog(
            onDismissRequest = { viewModel.closeEditCategory() },
            title = { Text(stringResource(R.string.edit_category)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.category_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = stringResource(R.string.category_color),
                        style = MaterialTheme.typography.labelMedium
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CategoryColorProvider.PRESET_PALETTE.forEach { hexColor ->
                            val color = CategoryColorProvider.getColor(null, hexColor)
                            val isSelected = selectedColor.equals(hexColor, ignoreCase = true)

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = hexColor },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.updateCategory(category.id, name, selectedColor)
                        }
                    },
                    enabled = name.isNotBlank()
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.closeEditCategory() }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Dialog: Delete Category Confirmation & Reassignment
    uiState.isDeletingCategory?.let { categoryToDelete ->
        val txCount = uiState.deleteTransactionCount
        val otherCategories = uiState.categories.map { it.category }.filter { it.id != categoryToDelete.id }
        var selectedReplacement by remember { mutableStateOf(otherCategories.firstOrNull()?.id ?: "other") }
        var dropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { viewModel.cancelDeleteCategory() },
            title = { Text(stringResource(R.string.delete_category)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (txCount > 0) {
                        Text(
                            text = stringResource(R.string.delete_category_in_use, txCount),
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        ExposedDropdownMenuBox(
                            expanded = dropdownExpanded,
                            onExpandedChange = { dropdownExpanded = it }
                        ) {
                            val selectedCat = otherCategories.find { it.id == selectedReplacement }
                            val repName = selectedCat?.getDisplayName(context) ?: selectedReplacement

                            OutlinedTextField(
                                value = repName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(R.string.replacement_category)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )

                            ExposedDropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false }
                            ) {
                                otherCategories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat.getDisplayName(context)) },
                                        onClick = {
                                            selectedReplacement = cat.id
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "${stringResource(R.string.delete_category)}: ${categoryToDelete.getDisplayName(context)}?",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val repId = if (txCount > 0) selectedReplacement else null
                        viewModel.confirmDeleteCategory(repId)
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(
                        if (txCount > 0) stringResource(R.string.reassign_and_delete) else stringResource(R.string.delete)
                    )
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.cancelDeleteCategory() }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Dialog: Add Keyword
    uiState.addingKeywordForCategory?.let { category ->
        var keyword by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { viewModel.closeAddKeyword() },
            title = { Text("${stringResource(R.string.add_keyword)}: ${category.getDisplayName(context)}") },
            text = {
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = { Text(stringResource(R.string.keyword_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (keyword.isNotBlank()) {
                            viewModel.addKeyword(category.id, keyword)
                        }
                    },
                    enabled = keyword.isNotBlank()
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.closeAddKeyword() }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

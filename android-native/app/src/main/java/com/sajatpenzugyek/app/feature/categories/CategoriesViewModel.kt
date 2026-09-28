package com.sajatpenzugyek.app.feature.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sajatpenzugyek.app.PenzugyekApp
import com.sajatpenzugyek.app.domain.model.Category
import com.sajatpenzugyek.app.domain.model.CategoryRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

data class CategoryWithRules(
    val category: Category,
    val rules: List<CategoryRule>
)

data class CategoriesUiState(
    val categories: List<CategoryWithRules> = emptyList(),
    val isDeletingCategory: Category? = null,
    val deleteTransactionCount: Int = 0,
    val showAddCategoryDialog: Boolean = false,
    val editingCategory: Category? = null,
    val addingKeywordForCategory: Category? = null,
    val errorMessage: String? = null
)

class CategoriesViewModel : ViewModel() {
    private val app = PenzugyekApp.instance
    private val catRepo = app.categoryRepository
    private val txDao = app.database.transactionDao()

    private val _uiStateInternal = MutableStateFlow(CategoriesUiState())

    val uiState: StateFlow<CategoriesUiState> = combine(
        catRepo.getAllCategoriesFlow(),
        catRepo.getAllRulesFlow(),
        _uiStateInternal
    ) { categories, rules, internalState ->
        val rulesByCat = rules.groupBy { it.categoryId }
        val categoryWithRules = categories.map { cat ->
            CategoryWithRules(
                category = cat,
                rules = rulesByCat[cat.id] ?: emptyList()
            )
        }
        internalState.copy(categories = categoryWithRules)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CategoriesUiState()
    )

    fun openAddCategory() {
        _uiStateInternal.value = _uiStateInternal.value.copy(showAddCategoryDialog = true)
    }

    fun closeAddCategory() {
        _uiStateInternal.value = _uiStateInternal.value.copy(showAddCategoryDialog = false)
    }

    fun openEditCategory(category: Category) {
        _uiStateInternal.value = _uiStateInternal.value.copy(editingCategory = category)
    }

    fun closeEditCategory() {
        _uiStateInternal.value = _uiStateInternal.value.copy(editingCategory = null)
    }

    fun openAddKeyword(category: Category) {
        _uiStateInternal.value = _uiStateInternal.value.copy(addingKeywordForCategory = category)
    }

    fun closeAddKeyword() {
        _uiStateInternal.value = _uiStateInternal.value.copy(addingKeywordForCategory = null)
    }

    fun addCategory(name: String, color: String, initialKeyword: String?) {
        viewModelScope.launch {
            val id = "cat_custom_${UUID.randomUUID().toString().take(8)}"
            val category = Category(
                id = id,
                name = name.trim(),
                icon = "tag",
                color = color,
                isIncome = false,
                isDefault = false,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
            catRepo.insertCategory(category)
            if (!initialKeyword.isNullOrBlank()) {
                catRepo.addKeywordRule(id, initialKeyword)
            }
            closeAddCategory()
        }
    }

    fun updateCategory(id: String, name: String, color: String) {
        viewModelScope.launch {
            val existing = catRepo.getAllCategories().find { it.id == id } ?: return@launch
            val updated = existing.copy(name = name.trim(), color = color, updatedAt = Instant.now())
            catRepo.updateCategory(updated)
            closeEditCategory()
        }
    }

    fun requestDeleteCategory(category: Category) {
        viewModelScope.launch {
            val count = catRepo.countTransactionsForCategory(category.id, txDao)
            _uiStateInternal.value = _uiStateInternal.value.copy(
                isDeletingCategory = category,
                deleteTransactionCount = count
            )
        }
    }

    fun cancelDeleteCategory() {
        _uiStateInternal.value = _uiStateInternal.value.copy(
            isDeletingCategory = null,
            deleteTransactionCount = 0
        )
    }

    fun confirmDeleteCategory(replacementCategoryId: String?) {
        viewModelScope.launch {
            val cat = _uiStateInternal.value.isDeletingCategory ?: return@launch
            if (replacementCategoryId != null && replacementCategoryId != cat.id) {
                catRepo.reassignCategory(cat.id, replacementCategoryId, txDao)
            }
            catRepo.deleteCategory(cat.id)
            cancelDeleteCategory()
        }
    }

    fun addKeyword(categoryId: String, keyword: String) {
        viewModelScope.launch {
            val success = catRepo.addKeywordRule(categoryId, keyword)
            if (!success) {
                _uiStateInternal.value = _uiStateInternal.value.copy(
                    errorMessage = "keyword_already_exists"
                )
            } else {
                closeAddKeyword()
            }
        }
    }

    fun deleteKeyword(ruleId: String) {
        viewModelScope.launch {
            catRepo.deleteRule(ruleId)
        }
    }

    fun clearErrorMessage() {
        _uiStateInternal.value = _uiStateInternal.value.copy(errorMessage = null)
    }
}

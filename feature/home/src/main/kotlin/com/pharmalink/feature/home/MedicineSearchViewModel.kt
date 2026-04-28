package com.pharmalink.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.domain.model.Medicine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class MedicineSearchViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(MedicineSearchUiState())
    val uiState: StateFlow<MedicineSearchUiState> = _uiState

    // Dummy medicines for UI demonstration
    private val allMedicines = listOf(
        Medicine(
            id = "1",
            name = "باراسيتامول",
            brand = "Panadol",
            strength = "500mg",
            price = 25.0,
            imageUrl = null
        ),
        Medicine(
            id = "2",
            name = "أموكسيسيلين",
            brand = "Amoxil",
            strength = "250mg",
            price = 45.0,
            imageUrl = null
        ),
        Medicine(
            id = "3",
            name = "إيبوبروفين",
            brand = "Advil",
            strength = "400mg",
            price = 35.0,
            imageUrl = null
        ),
        Medicine(
            id = "4",
            name = "سيتريزين",
            brand = "Zyrtec",
            strength = "10mg",
            price = 55.0,
            imageUrl = null
        ),
        Medicine(
            id = "5",
            name = "أوميبرازول",
            brand = "Losec",
            strength = "20mg",
            price = 65.0,
            imageUrl = null
        ),
        Medicine(
            id = "6",
            name = "ميتفورمين",
            brand = "Glucophage",
            strength = "500mg",
            price = 40.0,
            imageUrl = null
        ),
        Medicine(
            id = "7",
            name = "أتورفاستاتين",
            brand = "Lipitor",
            strength = "20mg",
            price = 85.0,
            imageUrl = null
        ),
        Medicine(
            id = "8",
            name = "فيتامين د",
            brand = "D-Viton",
            strength = "1000 IU",
            price = 30.0,
            imageUrl = null
        ),
    )

    init {
        // Search debounce
        _uiState
            .map { it.searchQuery }
            .debounce(300)
            .distinctUntilChanged()
            .onEach { query ->
                performSearch(query)
            }
            .launchIn(viewModelScope)

        loadMedicines()
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun loadMedicines() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // Simulate network delay
                kotlinx.coroutines.delay(500)
                _uiState.update {
                    it.copy(
                        medicines = allMedicines,
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "فشل تحميل الأدوية",
                    )
                }
            }
        }
    }

    private fun performSearch(query: String) {
        if (query.isBlank()) {
            _uiState.update { it.copy(medicines = allMedicines) }
            return
        }

        val filtered = allMedicines.filter { medicine ->
            medicine.name.contains(query, ignoreCase = true) ||
                medicine.brand.contains(query, ignoreCase = true) ||
                medicine.strength.contains(query, ignoreCase = true)
        }

        _uiState.update { it.copy(medicines = filtered) }
    }
}

data class MedicineSearchUiState(
    val searchQuery: String = "",
    val medicines: List<Medicine> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

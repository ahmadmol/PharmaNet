package com.pharmalink.feature.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.model.Medicine
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
@HiltViewModel
class MedicineSearchViewModel @Inject constructor(
    private val pharmaRepository: PharmaRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MedicineSearchUiState())
    val uiState: StateFlow<MedicineSearchUiState> = _uiState.asStateFlow()

    init {
        _uiState
            .map { it.searchQuery }
            .debounce(300)
            .distinctUntilChanged()
            .onEach(::performSearch)
            .launchIn(viewModelScope)

        loadMedicines()
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun loadMedicines() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            pharmaRepository.fetchMedicines()
                .onSuccess { medicines ->
                    val currentQuery = _uiState.value.searchQuery
                    _uiState.update {
                        it.copy(
                            allMedicines = medicines,
                            medicines = filterMedicines(medicines, currentQuery),
                            isLoading = false,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: context.getString(R.string.medicine_search_load_error),
                        )
                    }
                }
        }
    }

    private fun performSearch(query: String) {
        _uiState.update {
            it.copy(
                medicines = filterMedicines(
                    medicines = it.allMedicines,
                    query = query,
                ),
            )
        }
    }

    private fun filterMedicines(
        medicines: List<Medicine>,
        query: String,
    ): List<Medicine> {
        if (query.isBlank()) return medicines
        return medicines.filter { medicine ->
            medicine.name.contains(query, ignoreCase = true) ||
                medicine.brand.contains(query, ignoreCase = true) ||
                medicine.strength.contains(query, ignoreCase = true)
        }
    }
}

data class MedicineSearchUiState(
    val searchQuery: String = "",
    val allMedicines: List<Medicine> = emptyList(),
    val medicines: List<Medicine> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

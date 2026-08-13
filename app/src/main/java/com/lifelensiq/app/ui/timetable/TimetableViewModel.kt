package com.lifelensiq.app.ui.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifelensiq.app.domain.model.TimetableSlot
import com.lifelensiq.app.domain.repository.TimetableRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TimetableViewModel(private val repo: TimetableRepository) : ViewModel() {

    private val _all = MutableStateFlow<List<TimetableSlot>>(emptyList())
    val all: StateFlow<List<TimetableSlot>> = _all.asStateFlow()

    val slotsByDay: StateFlow<Map<String, List<TimetableSlot>>> =
        _all.map { slots -> slots.groupBy { it.day } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        viewModelScope.launch {
            repo.observeAll().collect { slots -> _all.value = slots }
        }
    }
}

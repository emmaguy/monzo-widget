package com.emmav.monzowidget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

class StartupViewModel(
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    val uiState: StateFlow<StartupUiState> = flow {
        val session = sessionRepository.getSession()
        if (session == null) {
            emit(StartupUiState.RequiresAuth)
        } else {
            // TODO handle expired session here?
            emit(StartupUiState.HasAuth)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, StartupUiState.Unknown)
}

sealed class StartupUiState {
    data object Unknown : StartupUiState()
    object RequiresAuth : StartupUiState()
    object HasAuth : StartupUiState()
}
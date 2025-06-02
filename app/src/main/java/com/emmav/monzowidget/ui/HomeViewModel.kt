package com.emmav.monzowidget.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emmav.monzowidget.data.monzo.Account
import com.emmav.monzowidget.data.monzo.MonzoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeViewModel(
    private val monzoRepository: MonzoRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        viewModelScope.launch {
            monzoRepository.refreshAccounts()
                .onSuccess { accounts ->
                    accounts.forEach { accountId ->
                        monzoRepository.refreshPots(accountId)
                        monzoRepository.refreshBalance(accountId)
                    }
                }
        }

        viewModelScope.launch {
            monzoRepository.accountsWithPots()
                .collectLatest { accounts ->
                    _uiState.value = HomeUiState.Loaded(accounts = accounts)
                }
        }
    }

    sealed class HomeUiState {
        data object Loading : HomeUiState()
        data class Loaded(val accounts: List<Account>) : HomeUiState()
        data class Error(val message: String) : HomeUiState()
    }
}

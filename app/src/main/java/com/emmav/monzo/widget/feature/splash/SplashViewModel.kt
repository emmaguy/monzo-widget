package com.emmav.monzo.widget.feature.splash

import com.emmav.monzo.widget.common.BaseViewModel
import com.emmav.monzo.widget.data.auth.ClientRepository
import com.emmav.monzo.widget.data.auth.LoginRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel class SplashViewModel @Inject constructor(
    clientRepository: ClientRepository,
    loginRepository: LoginRepository
) : BaseViewModel<SplashViewModel.State>(initialState = State()) {

    init {
        if (!clientRepository.clientConfigured) {
            setState { copy(appState = AppState.REQUIRES_CLIENT) }
        } else if (!loginRepository.hasToken) {
            setState { copy(appState = AppState.REQUIRES_TOKEN) }
        } else {
            setState { copy(appState = AppState.AUTHENTICATED) }
        }
    }

    data class State(val appState: AppState? = null)

    enum class AppState {
        REQUIRES_CLIENT,
        REQUIRES_TOKEN,
        AUTHENTICATED
    }
}
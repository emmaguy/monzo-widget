package com.emmav.monzo.widget.common

import androidx.activity.viewModels
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.*
import io.reactivex.disposables.CompositeDisposable

abstract class BaseViewModel<S : Any>(initialState: S) : ViewModel() {
    protected val disposables = CompositeDisposable()

    private val viewState = MutableLiveData<S>().apply {
        value = initialState
    }
    val state: LiveData<S> = viewState

    @MainThread
    protected fun setState(reducer: S.() -> S) {
        val currentState = viewState.value!!
        val newState = currentState.reducer()
        if (newState != currentState) {
            viewState.value = newState
        }
    }

    override fun onCleared() {
        super.onCleared()
        disposables.dispose()
    }
}

inline fun <reified T : ViewModel> AppCompatActivity.assistedViewModel(
    crossinline viewModelProducer: (SavedStateHandle) -> T
) = viewModels<T> {
    object : AbstractSavedStateViewModelFactory(this, intent?.extras) {
        override fun <T : ViewModel> create(key: String, modelClass: Class<T>, handle: SavedStateHandle) =
            viewModelProducer(handle) as T
    }
}
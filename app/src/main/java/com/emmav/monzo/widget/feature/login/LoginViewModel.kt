package com.emmav.monzo.widget.feature.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.emmav.monzo.widget.R
import com.emmav.monzo.widget.common.BaseViewModel
import com.emmav.monzo.widget.common.Text
import com.emmav.monzo.widget.common.text
import com.emmav.monzo.widget.common.textRes
import com.emmav.monzo.widget.data.auth.LoginRepository
import com.emmav.monzo.widget.feature.sync.SyncWorker
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import java.util.concurrent.TimeUnit

private const val SECONDS_TO_REDIRECT = 5L

class LoginViewModel @AssistedInject constructor(
    private val loginRepository: LoginRepository,
    private val workManager: WorkManager,
    @Assisted private val redirectUri: String
) : BaseViewModel<LoginViewModel.State>(initialState = State.Unknown()) {

    init {
        if (loginRepository.hasToken) {
            setPreSCAAndSync()
        } else {
            setState { State.Unauthenticated() }
        }
    }

    /**
     * We don't know if we're approved for SCA until we try to sync
     */
    private fun setPreSCAAndSync() {
        setState { State.RequiresStrongCustomerAuthentication() }

        // Check 2 seconds after invoked, then every 10
        disposables += Observable.interval(2, 10, TimeUnit.SECONDS)
            .flatMapSingle { loginRepository.testAuthentication() }
            .filter { isAuthenticated -> isAuthenticated }
            .take(1) // Once we're logged in, we no longer need to poll
            .ignoreElements()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                setState { State.Authenticated() }
                workManager.enqueue(OneTimeWorkRequest.Builder(SyncWorker::class.java).build())
            }
    }

    fun onLoginClicked() {
        disposables += Observable.interval(1, TimeUnit.SECONDS)
            .take(SECONDS_TO_REDIRECT + 1)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                setState {
                    val timeLeft = SECONDS_TO_REDIRECT - it.toInt()
                    State.RequestMagicLink(
                        title = textRes(R.string.login_redirecting_title, timeLeft),
                        url = if (timeLeft <= 0) {
                            "https://auth.monzo.com/?client_id=${loginRepository.clientId}" +
                                    "&redirect_uri=${redirectUri}" +
                                    "&response_type=code" +
                                    "&state=${loginRepository.startLogin()}"
                        } else null
                    )
                }
            }
    }

    fun onMagicLinkParamsReceived(code: String, state: String) {
        setState { State.Authenticating() }
        disposables += loginRepository.login(redirectUri, code, state)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { setPreSCAAndSync() }
    }

    sealed class State {
        abstract val showLoading: Boolean
        abstract val actionButton: Text
        abstract val emoji: Text
        abstract val title: Text
        abstract val subtitle: Text

        data class Unknown(
            override val showLoading: Boolean = true,
            override val actionButton: Text = Text.Empty,
            override val emoji: Text = Text.Empty,
            override val title: Text = Text.Empty,
            override val subtitle: Text = Text.Empty
        ) : State()

        /**
         * User is completely logged out.
         */
        data class Unauthenticated(
            override val showLoading: Boolean = false,
            override val actionButton: Text = textRes(R.string.login_unauthed_action),
            override val emoji: Text = text("👋🏿"),
            override val title: Text = textRes(R.string.login_unauthed_title),
            override val subtitle: Text = textRes(R.string.login_unauthed_subtitle)
        ) : State()

        /**
         * Start of the OAuth flow to authenticate. The app will redirect the user to the [url], which will trigger
         * the sending of a magic link email. Clicking on this email will redirect back [redirectUri] and open
         * [LoginActivity]. We'll then update state to be [Authenticating].
         */
        data class RequestMagicLink(
            override val showLoading: Boolean = false,
            override val actionButton: Text = Text.Empty,
            override val emoji: Text = text("⏩"),
            override val title: Text = textRes(R.string.login_redirecting_title),
            override val subtitle: Text = textRes(R.string.login_redirecting_subtitle),
            val url: String?
        ) : State()

        /**
         * We've been redirected back to the app, via the magic link the user clicked on.
         * Use the information from this link to login with and retrieve an access and refresh token.
         */
        data class Authenticating(
            override val showLoading: Boolean = true,
            override val actionButton: Text = Text.Empty,
            override val emoji: Text = text("🔒"),
            override val title: Text = textRes(R.string.login_logging_in_title),
            override val subtitle: Text = textRes(R.string.login_logging_in_subtitle)
        ) : State()

        /**
         * We've got an access token to authenticate our calls to the Monzo api with, but to access Accounts and
         * Balance information, we need to do a second step of authentication, called Strong Customer Authentication.
         * This can be thought about as 2FA.
         */
        data class RequiresStrongCustomerAuthentication(
            override val showLoading: Boolean = true,
            override val actionButton: Text = textRes(R.string.login_requires_sca_action),
            override val emoji: Text = text("🔐"),
            override val title: Text = textRes(R.string.login_requires_sca_title),
            override val subtitle: Text = textRes(R.string.login_requires_sca_subtitle),
        ) : State()

        /**
         * We made it! We can successfully sync our data with the Monzo api 🙌🏽.
         */
        data class Authenticated(
            override val showLoading: Boolean = false,
            override val actionButton: Text = textRes(R.string.login_logged_in_action),
            override val emoji: Text = text("🎉"),
            override val title: Text = textRes(R.string.login_logged_in_title),
            override val subtitle: Text = textRes(R.string.login_logged_in_subtitle),
        ) : State()
    }

    @AssistedInject.Factory
    interface AssistedFactory {
        fun create(redirectUri: String): LoginViewModel
    }

    companion object {
        fun provideFactory(
            assistedFactory: AssistedFactory,
            redirectUri: String
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return assistedFactory.create(redirectUri) as T
            }
        }
    }
}
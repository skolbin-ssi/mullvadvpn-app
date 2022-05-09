package net.mullvad.mullvadvpn.service.endpoint

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.collect
import net.mullvad.mullvadvpn.ipc.Event
import net.mullvad.mullvadvpn.ipc.Request
import net.mullvad.mullvadvpn.model.AccountCreationResult
import net.mullvad.mullvadvpn.model.AccountExpiry
import net.mullvad.mullvadvpn.model.AccountHistory
import net.mullvad.mullvadvpn.model.GetAccountDataResult
import net.mullvad.mullvadvpn.util.JobTracker
import net.mullvad.talpid.util.EventNotifier
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

class AccountCache(private val endpoint: ServiceEndpoint) {
    companion object {
        private val EXPIRY_FORMAT = DateTimeFormat.forPattern("YYYY-MM-dd HH:mm:ss z")

        private sealed class Command {
            object CreateAccount : Command()
            data class Login(val account: String) : Command()
            object Logout : Command()
        }
    }

    private val commandChannel = spawnActor()

    private val daemon
        get() = endpoint.intermittentDaemon

    val onAccountNumberChange = EventNotifier<String?>(null)
    val onAccountExpiryChange = EventNotifier<AccountExpiry>(AccountExpiry.NotAvailable)
    val onAccountHistoryChange = EventNotifier<AccountHistory>(AccountHistory.WithoutHistory)

    private val jobTracker = JobTracker()

    private var accountExpiry by onAccountExpiryChange.notifiable()
    private var accountHistory by onAccountHistoryChange.notifiable()

    init {
        jobTracker.newBackgroundJob("autoFetchAccountExpiry") {
            daemon.await().deviceStateUpdates.collect { deviceState ->
                accountExpiry = deviceState.token()
                    ?.let { fetchAccountExpiry(it) } ?: AccountExpiry.NotAvailable
            }
        }

        onAccountHistoryChange.subscribe(this) { history ->
            endpoint.sendEvent(Event.AccountHistoryEvent(history))
        }

        onAccountExpiryChange.subscribe(this) {
            endpoint.sendEvent(Event.AccountExpiryEvent(it))
        }

        endpoint.dispatcher.apply {
            registerHandler(Request.CreateAccount::class) { _ ->
                commandChannel.sendBlocking(Command.CreateAccount)
            }

            registerHandler(Request.Login::class) { request ->
                request.account?.let { account ->
                    commandChannel.sendBlocking(Command.Login(account))
                }
            }

            registerHandler(Request.Logout::class) { _ ->
                commandChannel.sendBlocking(Command.Logout)
            }

            registerHandler(Request.FetchAccountExpiry::class) { _ ->
                jobTracker.newBackgroundJob("fetchAccountExpiry") {
                    accountExpiry =
                        accountToken()?.let { fetchAccountExpiry(it) } ?: AccountExpiry.NotAvailable
                }
            }

            registerHandler(Request.FetchAccountHistory::class) { _ ->
                jobTracker.newBackgroundJob("fetchAccountHistory") {
                    accountHistory = fetchAccountHistory()
                }
            }

            registerHandler(Request.ClearAccountHistory::class) { _ ->
                jobTracker.newBackgroundJob("clearAccountHistory") {
                    clearAccountHistory()
                }
            }
        }
    }

    fun onDestroy() {
        endpoint.settingsListener.accountNumberNotifier.unsubscribe(this)
        jobTracker.cancelAllJobs()

        onAccountNumberChange.unsubscribeAll()
        onAccountExpiryChange.unsubscribeAll()
        onAccountHistoryChange.unsubscribeAll()

        commandChannel.close()
    }

    private suspend fun accountToken(): String? {
        return daemon.await().deviceStateUpdates.value.token()
    }

    private fun spawnActor() = GlobalScope.actor<Command>(Dispatchers.Default, Channel.UNLIMITED) {
        try {
            for (command in channel) {
                when (command) {
                    is Command.CreateAccount -> doCreateAccount()
                    is Command.Login -> doLogin(command.account)
                    is Command.Logout -> doLogout()
                }
            }
        } catch (exception: ClosedReceiveChannelException) {
            // Command channel was closed, stop the actor
        }
    }

    private suspend fun clearAccountHistory() {
        daemon.await().clearAccountHistory()
        accountHistory = fetchAccountHistory()
    }

    private suspend fun doCreateAccount() {
        daemon.await().createNewAccount()
            .let { newAccountNumber ->
                if (newAccountNumber != null) {
                    AccountCreationResult.Success(newAccountNumber)
                } else {
                    AccountCreationResult.Failure
                }
            }
            .also { result ->
                endpoint.sendEvent(Event.AccountCreationEvent(result))
            }
    }

    private suspend fun doLogin(account: String) {
        daemon.await().loginAccount(account).also { result ->
            endpoint.sendEvent(Event.LoginEvent(result))
        }
    }

    private suspend fun doLogout() {
        daemon.await().logoutAccount()
        accountHistory = fetchAccountHistory()
    }

    private suspend fun fetchAccountHistory(): AccountHistory {
        return daemon.await().getAccountHistory().let { result ->
            if (result != null) {
                AccountHistory.WithHistory(result)
            } else {
                AccountHistory.WithoutHistory
            }
        }
    }

    private suspend fun fetchAccountExpiry(accountToken: String): AccountExpiry {
        return fetchAccountData(accountToken).let { result ->
            if (result is GetAccountDataResult.Ok) {
                AccountExpiry.Available(result.parseExpiryDate())
            } else {
                AccountExpiry.NotAvailable
            }
        }
    }

    private suspend fun fetchAccountData(accountToken: String): GetAccountDataResult {
        return daemon.await().getAccountData(accountToken)
    }

    private fun GetAccountDataResult.Ok.parseExpiryDate(): DateTime {
        return DateTime.parse(this.accountData.expiry, EXPIRY_FORMAT)
    }
}

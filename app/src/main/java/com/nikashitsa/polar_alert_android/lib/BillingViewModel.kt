package com.nikashitsa.polar_alert_android.lib

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BillingViewModel @Inject constructor(
    private val billing: BillingRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _unlimitedAccess = MutableStateFlow(SettingsDefaults.UNLIMITED_ACCESS)
    val unlimitedAccess: StateFlow<Boolean> = _unlimitedAccess

    private val _price = MutableStateFlow(BillingRepository.FALLBACK_PRICE)
    val price: StateFlow<String> = _price

    /** True while Play is being talked to, so the paywall can show a loader. */
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    /** A short line to show under the description, or null when there is nothing to say. */
    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice

    init {
        observe(settings.unlimitedAccessFlow) { entitled ->
            _unlimitedAccess.value = entitled
            if (entitled) {
                _busy.value = false
                _notice.value = null
            }
        }
        observe(billing.productDetails) { details ->
            _price.value = details?.oneTimePurchaseOfferDetails?.formattedPrice
                ?: BillingRepository.FALLBACK_PRICE
        }
        observe(billing.events) { event ->
            _busy.value = false
            _notice.value = event.message
        }
        // Warm the connection so the price is ready by the time the sheet is opened.
        viewModelScope.launch { billing.loadProductDetails() }
    }

    fun purchase(activity: Activity?) {
        beginPlayCall()
//        billing.purchase(activity)
    }

    fun restore() {
        beginPlayCall()
        billing.restore()
    }

    /** Drops the last message and shows the loader until Play reports back. */
    private fun beginPlayCall() {
        _notice.value = null
        _busy.value = true
    }

    private fun <T> observe(flow: Flow<T>, onEach: (T) -> Unit) {
        viewModelScope.launch { flow.collect { onEach(it) } }
    }
}

/** What the paywall says about an outcome, or null when it should stay quiet. */
private val BillingEvent.message: String?
    get() = when (this) {
        // Cancelling is a deliberate choice, not something to comment on.
        BillingEvent.UserCancelled -> null
        BillingEvent.PurchasePending ->
            "Your purchase is pending. Access unlocks once payment completes."
        BillingEvent.NothingToRestore -> "No previous purchase found."
        BillingEvent.Unavailable -> "Google Play is not available right now."
        BillingEvent.Failed -> "Something went wrong. Please try again."
    }

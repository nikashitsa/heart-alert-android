package com.nikashitsa.polar_alert_android.lib

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds

/** Something the paywall may want to tell the user about. */
sealed interface BillingEvent {
    data object UserCancelled : BillingEvent
    data object PurchasePending : BillingEvent
    data object NothingToRestore : BillingEvent
    data object Unavailable : BillingEvent
    data object Failed : BillingEvent
}

/**
 * Owns the Google Play connection for the one-time "Unlimited access" product.
 *
 * Granting lives here rather than in a ViewModel on purpose: Play's purchase overlay is a
 * separate activity that can take this process down with it, and the paywall sheet can be
 * swiped away mid-purchase. The grant has to outlive both.
 */
@Singleton
class BillingRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:ApplicationScope private val scope: CoroutineScope,
    private val settings: SettingsRepository,
) {
    private val tag = "BillingRepository"

    private val _productDetails = MutableStateFlow<ProductDetails?>(null)
    val productDetails: StateFlow<ProductDetails?> = _productDetails.asStateFlow()

    private val _events = MutableSharedFlow<BillingEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<BillingEvent> = _events.asSharedFlow()

    private val connectMutex = Mutex()

    private val client: BillingClient by lazy {
        BillingClient.newBuilder(context)
            .setListener(::onPurchasesUpdated)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            )
            .build()
    }

    init {
        // On a cold start, pick up anything Play knows that this install does not: a purchase
        // that completed while the process was dead, or an acknowledgement that never landed.
        scope.launch {
            if (settings.unlimitedAccessFlow.first()) return@launch
            if (connect()) {
                restorePurchases(silent = true)
                loadProductDetails()
            }
        }
    }

    // --- What the paywall calls -------------------------------------------------------

    /** Opens Play's purchase sheet. */
    fun purchase(activity: Activity?) {
        if (activity == null) {
            _events.tryEmit(BillingEvent.Failed)
            return
        }
        scope.launch {
            val details = loadProductDetails()
            if (details == null) {
                _events.tryEmit(BillingEvent.Unavailable)
                return@launch
            }
            val params = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(details)
                            .build()
                    )
                )
                .build()

            val result = client.launchBillingFlow(activity, params)
            when {
                result.isOk -> Unit
                // Already bought on another install; just re-read it instead of erroring.
                result.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED ->
                    restorePurchases(silent = true)
                else -> {
                    result.logIfFailed("Launch purchase")
                    _events.tryEmit(BillingEvent.Failed)
                }
            }
        }
    }

    /** Re-reads what Play says the user owns, and says so if it owns nothing. */
    fun restore() {
        scope.launch { restorePurchases(silent = false) }
    }

    // --- Talking to Play --------------------------------------------------------------

    /** Connects if needed. Safe to call repeatedly and from several places at once. */
    suspend fun connect(): Boolean = connectMutex.withLock {
        if (client.isReady) return@withLock true

        val result = withTimeoutOrNull(CONNECT_TIMEOUT) {
            awaitCallback<BillingResult> { resume ->
                client.startConnection(object : BillingClientStateListener {
                    override fun onBillingSetupFinished(billingResult: BillingResult) =
                        resume(billingResult)

                    // Nothing to do: the library reconnects by itself.
                    override fun onBillingServiceDisconnected() = Unit
                })
            }
        }

        if (result == null || !result.isOk) {
            Log.e(tag, "Billing unavailable: ${result?.debugMessage ?: "connection timed out"}")
            _events.tryEmit(BillingEvent.Unavailable)
            return@withLock false
        }
        true
    }

    /** Reads the product so the paywall can show the real, localised price. */
    suspend fun loadProductDetails(): ProductDetails? {
        _productDetails.value?.let { return it }
        if (!connect()) return null

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()

        val details = awaitCallback<ProductDetails?> { resume ->
            client.queryProductDetailsAsync(params) { result, queryResult ->
                result.logIfFailed("Product query")
                resume(queryResult.productDetailsList.firstOrNull())
            }
        }
        _productDetails.value = details
        return details
    }

    /**
     * Grants access for anything Play says is already paid for, and retries acknowledgements
     * that never landed. When not [silent], reports back if Play knows of nothing at all.
     */
    suspend fun restorePurchases(silent: Boolean) {
        if (!connect()) {
            if (!silent) _events.tryEmit(BillingEvent.Unavailable)
            return
        }
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        val purchases = awaitCallback<List<Purchase>> { resume ->
            client.queryPurchasesAsync(params) { result, purchases ->
                result.logIfFailed("Purchase query")
                resume(purchases)
            }
        }

        val foundOurs = handlePurchases(purchases)
        // Only when Play knows of nothing at all. A pending purchase has already reported
        // itself, and telling that user "no purchase found" would contradict it.
        if (!silent && !foundOurs) _events.tryEmit(BillingEvent.NothingToRestore)
    }

    private fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when {
            result.isOk -> scope.launch { handlePurchases(purchases.orEmpty()) }

            result.responseCode == BillingClient.BillingResponseCode.USER_CANCELED ->
                _events.tryEmit(BillingEvent.UserCancelled)

            // The user owns it but this install had lost the flag. Re-read and grant.
            result.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED ->
                scope.launch { restorePurchases(silent = true) }

            else -> {
                result.logIfFailed("Purchase")
                _events.tryEmit(BillingEvent.Failed)
            }
        }
    }

    /** Returns whether any of [purchases] was our product, in any state. */
    private suspend fun handlePurchases(purchases: List<Purchase>): Boolean {
        val ours = purchases.filter { it.products.contains(PRODUCT_ID) }

        for (purchase in ours) {
            when (purchase.purchaseState) {
                Purchase.PurchaseState.PURCHASED -> {
                    if (!purchase.isAcknowledged) acknowledge(purchase)
                    // Granted even if the acknowledgement failed. They paid, and the ack is
                    // retried next launch. Charging someone and withholding access is worse.
                    settings.setUnlimitedAccess()
                }
                // Cash or carrier billing that has not cleared yet. No access until it does.
                Purchase.PurchaseState.PENDING -> _events.tryEmit(BillingEvent.PurchasePending)
                else -> Unit
            }
        }
        return ours.isNotEmpty()
    }

    /** Play refunds a purchase that is not acknowledged within three days. */
    private suspend fun acknowledge(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        awaitCallback<BillingResult> { resume ->
            client.acknowledgePurchase(params, resume)
        }.logIfFailed("Acknowledge")
    }

    // --- Small helpers ----------------------------------------------------------------

    /** Turns one of Play's callback APIs into a suspending call. */
    private suspend fun <T> awaitCallback(start: ((T) -> Unit) -> Unit): T =
        suspendCancellableCoroutine { continuation ->
            // Play may invoke a callback more than once; only the first result counts.
            start { value -> if (continuation.isActive) continuation.resume(value) }
        }

    private val BillingResult.isOk
        get() = responseCode == BillingClient.BillingResponseCode.OK

    private fun BillingResult.logIfFailed(what: String) {
        if (!isOk) Log.e(tag, "$what failed: $responseCode $debugMessage")
    }

    companion object {
        const val PRODUCT_ID = "unlimited_access"

        /** Shown until Play tells us the real, localised price. */
        const val FALLBACK_PRICE = "$4.99"

        private val CONNECT_TIMEOUT = 5.seconds
    }
}

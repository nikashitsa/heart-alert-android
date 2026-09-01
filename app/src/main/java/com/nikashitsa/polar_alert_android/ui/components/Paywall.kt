package com.nikashitsa.polar_alert_android.ui.components

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nikashitsa.polar_alert_android.lib.BillingRepository
import com.nikashitsa.polar_alert_android.lib.BillingViewModel
import com.nikashitsa.polar_alert_android.ui.theme.Colors
import com.nikashitsa.polar_alert_android.ui.theme.Fonts
import com.nikashitsa.polar_alert_android.ui.theme.HeartAlertTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Paywall(
    sheetState: SheetState,
    activity: Activity?,
    billing: BillingViewModel = hiltViewModel(),
    onDismissRequest: () -> Unit = {},
    onContinue: () -> Unit = {},
) {
    val unlimitedAccess by billing.unlimitedAccess.collectAsState()
    val price by billing.price.collectAsState()
    val busy by billing.busy.collectAsState()
    val notice by billing.notice.collectAsState()

    PaywallContent(
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        onContinue = onContinue,
        purchased = unlimitedAccess,
        price = price,
        busy = busy,
        notice = notice,
        purchase = { billing.purchase(activity) },
        restore = billing::restore,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallContent(
    sheetState: SheetState,
    onDismissRequest: () -> Unit = {},
    onContinue: () -> Unit = {},
    // Unlike DevicePickerContent's initialState, this is read on every recomposition rather
    // than seeded into remembered state: the sheet has to flip to Success the moment the
    // purchase lands, which may be while the user is looking at it.
    purchased: Boolean = false,
    price: String = BillingRepository.FALLBACK_PRICE,
    busy: Boolean = false,
    notice: String? = null,
    purchase: () -> Unit = {},
    restore: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Colors.Gray
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (purchased) {
                PaywallBody(
                    title = "Success",
                    description = "Unlimited access is now available.",
                )
                AppButton("Continue") {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) onContinue()
                    }
                }
            } else {
                PaywallBody(
                    title = "Unlimited access",
                    description = "Your free sessions are complete.\nKeep monitoring with unlimited access.",
                    notice = notice,
                )
                if (busy) {
                    AppLoader(Modifier.height(104.dp))
                } else {
                    AppButton("One-time purchase $price") { purchase() }
                    Spacer(modifier = Modifier.height(4.dp))
                    AppButton("Restore purchase", colors = Colors.LinkButton) { restore() }
                }
            }
        }
    }
}

/** Title plus copy, filling the space between the drag handle and the buttons. */
@Composable
private fun ColumnScope.PaywallBody(
    title: String,
    description: String,
    notice: String? = null,
) {
    Text(text = title, style = Fonts.textLgBold)
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = description, style = Fonts.textMd, textAlign = TextAlign.Start)
        if (notice != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = notice,
                style = Fonts.textMd,
                color = Colors.Red,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun previewSheetState() = SheetState(
    skipPartiallyExpanded = true,
    initialValue = SheetValue.Expanded,
    density = LocalDensity.current,
)

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun PaywallOfferPreview() {
    HeartAlertTheme {
        PaywallContent(sheetState = previewSheetState())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun PaywallSuccessPreview() {
    HeartAlertTheme {
        PaywallContent(sheetState = previewSheetState(), purchased = true)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun PaywallUnavailablePreview() {
    HeartAlertTheme {
        PaywallContent(
            sheetState = previewSheetState(),
            notice = "Google Play is not available right now.",
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun PaywallBusyPreview() {
    HeartAlertTheme {
        PaywallContent(sheetState = previewSheetState(), busy = true)
    }
}

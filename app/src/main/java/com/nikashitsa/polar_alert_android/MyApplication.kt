package com.nikashitsa.polar_alert_android

import android.app.Application
import com.nikashitsa.polar_alert_android.lib.BillingRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MyApplication: Application() {
    @Inject lateinit var billing: BillingRepository
}

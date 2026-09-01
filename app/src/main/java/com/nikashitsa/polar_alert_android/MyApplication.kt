package com.nikashitsa.polar_alert_android

import android.app.Application
import com.nikashitsa.polar_alert_android.lib.BillingRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MyApplication: Application() {

    // Injected purely so it is constructed at launch. Its init block restores a purchase this
    // install may have missed and retries any acknowledgement that never reached Play.
    @Inject lateinit var billing: BillingRepository
}

package com.yannuo.dgcanteen.common

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yannuo.dgcanteen.activitys.CalculateActivity

class PeriodicVerificationReceiver: BroadcastReceiver() {
    override fun onReceive(con: Context?, intent: Intent?) {
        val calculateActivity = con as? CalculateActivity
        calculateActivity?.initVerify()
    }
}
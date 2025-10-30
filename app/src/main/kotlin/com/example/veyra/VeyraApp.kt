package com.example.veyra

import android.app.Application
import com.google.android.gms.cast.framework.CastContext

class VeyraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CastContext.getSharedInstance(this)
    }
}

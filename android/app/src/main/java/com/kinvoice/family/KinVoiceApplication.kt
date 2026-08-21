package com.kinvoice.family

import android.app.Application

class KinVoiceApplication : Application() {
    val repository by lazy { KinVoiceRepository(this, KinVoiceDatabase.get(this).dao()) }
}

package com.niben.app

import android.app.Application
import com.niben.app.data.ContentSeeder
import com.niben.app.data.NibenDatabase
import com.niben.app.notification.QuizNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NibenApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        QuizNotifier.createChannel(this)
        val db = NibenDatabase.getInstance(this)
        applicationScope.launch {
            ContentSeeder.seedIfEmpty(this@NibenApplication, db.contentDao())
        }
    }
}

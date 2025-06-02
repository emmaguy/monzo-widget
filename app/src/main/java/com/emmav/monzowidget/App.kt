package com.emmav.monzowidget

import android.app.Application
import com.emmav.monzowidget.data.DataModule
import com.emmav.monzowidget.data.monzo.MonzoRepository
import com.emmav.monzowidget.data.session.SessionRepository

class App : Application() {
    companion object {
        @JvmStatic
        lateinit var instance: App private set
    }

    private val db by lazy {
        DataModule.createDb(
            context = this,
        )
    }
    private val monzoApi by lazy {
        DataModule.create(
            context = this,
            sessionStorage = db.authStorage(),
            baseUrl = "https://api.monzo.com",
        )
    }

    val sessionRepository by lazy {
        SessionRepository(
            api = monzoApi,
            db = db.authStorage(),
        )
    }
    val monzoRepository by lazy {
        MonzoRepository(
            api = monzoApi,
            storage = db.monzoStorage(),
        )
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
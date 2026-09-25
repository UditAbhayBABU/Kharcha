package com.example

import android.app.Application
import com.example.data.local.KharchaDatabase
import com.example.data.repository.AuthRepository
import com.example.data.repository.KharchaRepository
import com.google.firebase.FirebaseApp

class KharchaApplication : Application() {

    lateinit var database: KharchaDatabase
        private set

    lateinit var kharchaRepository: KharchaRepository
        private set

    lateinit var authRepository: AuthRepository
        private set

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)

        database = KharchaDatabase.getInstance(this)
        kharchaRepository = KharchaRepository(this, database.kharchaDao())
        authRepository = AuthRepository(this)
    }

    companion object {
        lateinit var instance: KharchaApplication
            private set
    }

    init {
        instance = this
    }
}

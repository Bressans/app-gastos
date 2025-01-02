package com.example.myapplication

import android.app.Application
import com.google.firebase.database.FirebaseDatabase

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Ativar a persistência offline do Firebase
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
    }
}

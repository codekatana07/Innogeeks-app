package edu.kiet.innogeeks

import android.app.Application
import com.google.firebase.database.FirebaseDatabase

class FirebaseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Enable offline persistence
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
    }
}
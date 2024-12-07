package edu.kiet.innogeeks

import android.os.AsyncTask

class MyAsyncTask(private val recipientToken: String) : AsyncTask<Void, Void, Void>() {

    @Deprecated("Deprecated in Java")
    override fun doInBackground(vararg params: Void?): Void? {
        sendNotification(recipientToken)
        return null
    }

    private fun sendNotification(recipientToken: String) {
        try {
            // Your notification sending code here
            // ...
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

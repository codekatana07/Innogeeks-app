package edu.kiet.innogeeks

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.messaging.FirebaseMessaging

object FCMTokenManager {

fun storeFCMToken() {
    // Get the current Firebase user
    val user: FirebaseUser? = FirebaseAuth.getInstance().currentUser

    user?.let {
        // Get the user ID
        val userID = it.uid
        println(userID)

        // Get the FCM token
        FirebaseMessaging.getInstance().token
                .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result

                // Store the FCM token in Realtime Database
                val tokensRef = FirebaseDatabase.getInstance("https://innogeeks2024-default-rtdb.firebaseio.com/").getReference("users")
                val query = tokensRef.orderByChild("userID").equalTo(userID)

                query.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        for (userSnapshot in dataSnapshot.children) {
                            val specificUserRef = userSnapshot.ref
                            specificUserRef.child("token").setValue(token)
                                    .addOnSuccessListener {
                                // Value updated successfully
                                // Do any additional operations here if needed
                            }
                                        .addOnFailureListener { exception ->
                                // Error occurred while updating the value
                                // Handle the error gracefully
                            }
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        // Handle the error gracefully
                    }
                })
            } else {
                Log.e("TAG", "Failed to get FCM token", task.exception)
            }
        }
    }
}
}

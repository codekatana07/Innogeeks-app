package edu.kiet.innogeeks

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import edu.kiet.innogeeks.model.UserData

object UserDataManager {
    private var userData: UserData? = null
    private val firestore = FirebaseFirestore.getInstance()
    private var userListener: ListenerRegistration? = null

    fun initialize(onComplete: (Boolean) -> Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            onComplete(false)
            return
        }

        // Always fetch fresh data when initializing
        fetchUserData(currentUser.uid) { success ->
            if (success) {
                setupChangeListener(currentUser.uid)
            }
            onComplete(success)
        }
    }

    private fun fetchUserData(uid: String, onComplete: (Boolean) -> Unit) {
        // First check admins
        firestore.collection("admins")
            .document(uid)
            .get()
            .addOnSuccessListener { adminDoc ->
                if (adminDoc.exists()) {
                    userData = createUserDataFromDocument(adminDoc, uid, "admin")
                    onComplete(true)
                    return@addOnSuccessListener
                }

                // Then check users
                firestore.collection("users")
                    .document(uid)
                    .get()
                    .addOnSuccessListener { userDoc ->
                        if (userDoc.exists()) {
                            userData = createUserDataFromDocument(userDoc, uid, "user")
                            onComplete(true)
                            return@addOnSuccessListener
                        }

                        // Finally check all domains
                        checkInDomains(uid, onComplete)
                    }
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }

    private fun checkInDomains(uid: String, onComplete: (Boolean) -> Unit) {
        firestore.collection("Domains")
            .get()
            .addOnSuccessListener { domains ->
                var checksCompleted = 0
                var userFound = false
                val totalChecks = domains.size() * 2 // For both coordinators and students

                if (totalChecks == 0) {
                    onComplete(false)
                    return@addOnSuccessListener
                }

                domains.forEach { domain ->
                    // Check coordinators
                    domain.reference.collection("Coordinators")
                        .document(uid)
                        .get()
                        .addOnSuccessListener { doc ->
                            checksCompleted++
                            if (doc.exists() && !userFound) {
                                userFound = true
                                userData = createUserDataFromDocument(doc, uid, "coordinator", domain.id)
                                onComplete(true)
                            } else if (checksCompleted == totalChecks && !userFound) {
                                onComplete(false)
                            }
                        }

                    // Check students
                    domain.reference.collection("Students")
                        .document(uid)
                        .get()
                        .addOnSuccessListener { doc ->
                            checksCompleted++
                            if (doc.exists() && !userFound) {
                                userFound = true
                                userData = createUserDataFromDocument(doc, uid, "student", domain.id)
                                onComplete(true)
                            } else if (checksCompleted == totalChecks && !userFound) {
                                onComplete(false)
                            }
                        }
                }
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }

    private fun createUserDataFromDocument(
        doc: DocumentSnapshot,
        uid: String,
        role: String,
        domainId: String? = null
    ): UserData {
        return UserData(
            uid = uid,
            email = doc.getString("email") ?: "",
            name = doc.getString("name") ?: "",
            role = role,
            domain = domainId ?: "none",
            libraryId = doc.getString("library-id") ?: ""
        )
    }

    private fun setupChangeListener(uid: String) {
        userListener?.remove()
        
        userData?.let { user ->
            userListener = when (user.role) {
                "admin" -> listenToDocument("admins", uid)
                "user" -> listenToDocument("users", uid)
                "coordinator", "student" -> listenToDomainUser(uid, user)
                else -> null
            }
        }
    }

    private fun listenToDocument(collection: String, uid: String): ListenerRegistration {
        return firestore.collection(collection)
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || !snapshot?.exists()!!) {
                    fetchUserData(uid) { }
                    return@addSnapshotListener
                }

                userData = createUserDataFromDocument(
                    doc = snapshot,
                    uid = uid,
                    role = collection.removeSuffix("s")
                )
            }
    }

    private fun listenToDomainUser(uid: String, user: UserData): ListenerRegistration {
        return firestore.collection("Domains")
            .document(user.domain)
            .collection("${user.role}s")
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || !snapshot?.exists()!!) {
                    fetchUserData(uid) { }
                    return@addSnapshotListener
                }

                userData = createUserDataFromDocument(
                    doc = snapshot,
                    uid = uid,
                    role = user.role,
                    domainId = user.domain
                )
            }
    }

    fun stopListening() {
        userListener?.remove()
        userListener = null
        userData = null  // Clear the cached data when stopping
    }

    fun getCurrentUser(): UserData? = userData
}

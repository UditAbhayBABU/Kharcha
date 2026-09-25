package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.UserProfile
import com.example.data.remote.FirestoreSyncManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

class AuthRepository(
    private val context: Context,
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestoreSync: FirestoreSyncManager = FirestoreSyncManager()
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("kharcha_prefs", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<FirebaseUser?>(firebaseAuth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _isAppUnlocked = MutableStateFlow<Boolean>(false)
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

    init {
        firebaseAuth.addAuthStateListener { auth ->
            _currentUser.value = auth.currentUser
        }
    }

    suspend fun signInWithEmail(email: String, pass: String): Result<FirebaseUser> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email.trim(), pass).await()
            val user = authResult.user ?: throw IllegalStateException("User nahi mila")
            loadUserProfile(user.uid)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(email: String, pass: String, name: String): Result<FirebaseUser> {
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email.trim(), pass).await()
            val user = authResult.user ?: throw IllegalStateException("User account nahi bana")

            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name.trim())
                .build()
            user.updateProfile(profileUpdates).await()

            val profile = UserProfile(
                uid = user.uid,
                email = user.email ?: "",
                displayName = name.trim(),
                role = "user"
            )
            firestoreSync.saveUserProfile(profile)
            _userProfile.value = profile
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogleCredential(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val user = authResult.user ?: throw IllegalStateException("Google sign-in fail hua")
            loadUserProfile(user.uid)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadUserProfile(userId: String) {
        val profile = firestoreSync.getUserProfile(userId)
        if (profile != null) {
            _userProfile.value = profile
        } else {
            val user = firebaseAuth.currentUser
            val newProfile = UserProfile(
                uid = userId,
                email = user?.email ?: "",
                displayName = user?.displayName ?: ""
            )
            firestoreSync.saveUserProfile(newProfile)
            _userProfile.value = newProfile
        }
    }

    suspend fun updateSheetsUrl(sheetsUrl: String, autoSync: Boolean) {
        val current = _userProfile.value ?: return
        val updated = current.copy(sheetsUrl = sheetsUrl, sheetsAutoSync = autoSync)
        _userProfile.value = updated
        firestoreSync.saveUserProfile(updated)
    }

    suspend fun updateBudgetFeatureEnabled(enabled: Boolean) {
        val current = _userProfile.value ?: return
        val updated = current.copy(isBudgetFeatureEnabled = enabled)
        _userProfile.value = updated
        firestoreSync.saveUserProfile(updated)
    }

    suspend fun updateExcelSyncPreference(time: String) {
        val current = _userProfile.value ?: return
        val updated = current.copy(excelPreferredSyncTime = time)
        _userProfile.value = updated
        firestoreSync.saveUserProfile(updated)
    }

    suspend fun recordExcelSyncCompleted() {
        val current = _userProfile.value ?: return
        val updated = current.copy(lastExcelSyncMillis = System.currentTimeMillis())
        _userProfile.value = updated
        firestoreSync.saveUserProfile(updated)
    }

    fun signOut() {
        firebaseAuth.signOut()
        _currentUser.value = null
        _userProfile.value = null
        _isAppUnlocked.value = false
    }

    // --- PIN LOCK SYSTEM ---
    // Note: Quick Add strictly bypasses PIN lock as required by core spec.
    fun isPinLockEnabled(): Boolean {
        return prefs.getBoolean("pin_lock_enabled", false)
    }

    fun setPin(pin: String) {
        val hash = hashPin(pin)
        prefs.edit()
            .putString("pin_hash", hash)
            .putBoolean("pin_lock_enabled", true)
            .apply()
        _isAppUnlocked.value = true
    }

    fun verifyPin(pin: String): Boolean {
        val storedHash = prefs.getString("pin_hash", "") ?: ""
        val inputHash = hashPin(pin)
        val valid = storedHash.isNotEmpty() && storedHash == inputHash
        if (valid) {
            _isAppUnlocked.value = true
        }
        return valid
    }

    fun disablePinLock() {
        prefs.edit()
            .remove("pin_hash")
            .putBoolean("pin_lock_enabled", false)
            .apply()
        _isAppUnlocked.value = true
    }

    fun unlockAppForSession() {
        _isAppUnlocked.value = true
    }

    fun lockApp() {
        _isAppUnlocked.value = false
    }

    private fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

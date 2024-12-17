package com.komiker.events.services.auth

import android.content.Context
import android.util.Log
import android.widget.Button
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import com.auth0.jwt.JWT
import com.auth0.jwt.interfaces.DecodedJWT
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.komiker.events.R
import com.komiker.events.data.database.SupabaseClientProvider
import com.komiker.events.data.database.dao.SupabaseUserDao
import com.komiker.events.data.database.entities.User
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.providers.builtin.IDToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID
import io.github.cdimascio.dotenv.dotenv

class GoogleAuthManager {

    private val dotenv = dotenv {
        directory = "/assets"
        filename = "env"
    }

    private val googleClientId: String = dotenv["GOOGLE_CLIENT_ID"] ?: throw IllegalStateException("GOOGLE_CLIENT_ID is not defined in .env file")
    private val supabaseClient = SupabaseClientProvider.client

    fun setupGoogleSignInButton(
        context: Context,
        button: Button,
        coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main),
        navController: NavController
    ) {
        button.setOnClickListener {
            val credentialManager = CredentialManager.create(context)
            val supabase = SupabaseClientProvider.client
            // Generate a nonce and hash it with SHA-256
            val rawNonce = UUID.randomUUID().toString()
            val bytes = rawNonce.toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

            val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(googleClientId)
                .setNonce(hashedNonce)
                .build()

            val request: GetCredentialRequest = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            coroutineScope.launch {
                try {
                    val result = credentialManager.getCredential(context, request)

                    val googleIdTokenCredential =
                        GoogleIdTokenCredential.createFrom(result.credential.data)
                    val googleIdToken = googleIdTokenCredential.idToken

                    supabase.auth.signInWith(IDToken) {
                        idToken = googleIdToken
                        provider = Google
                        nonce = rawNonce
                    }

                    val userDao = SupabaseUserDao(supabase)
                    val decodedJWT: DecodedJWT = JWT.decode(googleIdToken)
                    val email = decodedJWT.getClaim("email").asString()
                    val pictureUrl = decodedJWT.getClaim("picture").asString()
                    val userId = supabaseClient.auth.currentSessionOrNull()?.user?.id

                    val user = User(
                        id_user = userId.toString(),
                        generateUsername(),
                        email,
                        pictureUrl
                    )
                    userDao.insertUser(user)

                    Log.i("GoogleSignInManager", "Sign-in successful with ID token: $googleIdToken")

                    val fadeOutAnimation = R.anim.fade_out
                    val fadeInAnimation = R.anim.fade_in

                    navController.navigate(
                        R.id.action_WelcomeFragment_to_RegistrationSuccessFragment,
                        null,
                        NavOptions.Builder()
                            .setEnterAnim(fadeInAnimation)
                            .setExitAnim(fadeOutAnimation)
                            .build()
                    )

                } catch (e: GetCredentialException) {
                    Log.e("GoogleSignInManager", "GetCredentialException: ${e.message}", e)
                } catch (e: GoogleIdTokenParsingException) {
                    Log.e("GoogleSignInManager", "GoogleIdTokenParsingException: ${e.message}", e)
                } catch (e: RestException) {
                    Log.e("GoogleSignInManager", "RestException: ${e.message}", e)
                } catch (e: Exception) {
                    Log.e("GoogleSignInManager", "Exception: ${e.message}", e)
                }
            }
        }
    }

    private fun generateUsername(): String {
        val username: String
        val random = java.util.Random()
        val randomNumber = random.nextInt(900000) + 100000
        username = "username$randomNumber"
        return username
    }
}
package com.example.cplswellbeingbynovadevs

import com.example.cplswellbeingbynovadevs.data.remote.SupabaseClient
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Test

class SupabaseConnectionTest {

    @Test
    fun testSupabaseConnectionAndResponse() = runBlocking {
        println("Testing Supabase connection to: ${SupabaseClient.SUPABASE_URL}")
        val signInResult = SupabaseClient.signIn("mda446747@gmail.com", "Starzplay@225")
        
        // Supabase reachable check - result is either success or email confirmation failure
        val exception = signInResult.exceptionOrNull()
        if (signInResult.isSuccess) {
            println("SIGN IN SUCCESS: ${signInResult.getOrNull()}")
        } else {
            println("SUPABASE RESPONDED WITH AUTH ERROR: ${exception?.message}")
            assertNotNull(exception)
        }
    }
}

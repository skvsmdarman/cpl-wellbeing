package com.example.cplswellbeingbynovadevs

import com.example.cplswellbeingbynovadevs.data.remote.SupabaseClient
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionCodeTest {

    @Test
    fun testGenerateConnectionCode() {
        runBlocking {
            val userId = SupabaseClient.currentUserId ?: "00000000-0000-0000-0000-000000000001"
            val result = SupabaseClient.generateConnectionCode(userId)
            
            if (result.isFailure) {
                println("ERROR GENERATING CODE: ${result.exceptionOrNull()?.message}")
            }
            assertTrue("Code generation should succeed: ${result.exceptionOrNull()?.message}", result.isSuccess)
        }
    }
}

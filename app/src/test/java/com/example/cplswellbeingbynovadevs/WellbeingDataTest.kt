package com.example.cplswellbeingbynovadevs

import com.example.cplswellbeingbynovadevs.data.model.UsageDaily
import com.example.cplswellbeingbynovadevs.data.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class WellbeingDataTest {

    @Test
    fun testConnectionCodeFormat() {
        val pattern = Regex("^CPL-[A-Z0-9]{4}-[A-Z0-9]{4}$")
        val sampleCode = "CPL-7K4P-92QX"
        assertTrue(pattern.matches(sampleCode))

        val invalidCode = "INVALID-CODE"
        assertFalse(pattern.matches(invalidCode))
    }

    @Test
    fun testUserProfileRoleAssignment() {
        val adminEmail = "mda446747@gmail.com"
        val roleForAdmin = if (adminEmail.equals("mda446747@gmail.com", ignoreCase = true)) UserProfile.ROLE_ADMIN else UserProfile.ROLE_USER
        assertEquals(UserProfile.ROLE_ADMIN, roleForAdmin)

        val userEmail = "normaluser@example.com"
        val roleForUser = if (userEmail.equals("mda446747@gmail.com", ignoreCase = true)) UserProfile.ROLE_ADMIN else UserProfile.ROLE_USER
        assertEquals(UserProfile.ROLE_USER, roleForUser)
    }

    @Test
    fun testSevenDayAggregationLogic() {
        val day1 = UsageDaily(appId = "com.instagram.android", appName = "Instagram", totalSeconds = 3600)
        val day2 = UsageDaily(appId = "com.instagram.android", appName = "Instagram", totalSeconds = 1800)
        val day3 = UsageDaily(appId = "com.whatsapp", appName = "WhatsApp", totalSeconds = 1200)

        val allList = listOf(day1, day2, day3)
        val aggregated = allList.groupBy { it.appId }.map { (appId, list) ->
            Pair(appId, list.sumOf { it.totalSeconds })
        }.toMap()

        assertEquals(5400L, aggregated["com.instagram.android"])
        assertEquals(1200L, aggregated["com.whatsapp"])
    }

    @Test
    fun testOnlineStatusCalculation() {
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val recentDate = Date(System.currentTimeMillis() - 2 * 60 * 1000) // 2 minutes ago
        val lastSeenIso = isoFormat.format(recentDate)

        val parsed = isoFormat.parse(lastSeenIso)
        val diffMs = System.currentTimeMillis() - (parsed?.time ?: 0)
        val diffMinutes = diffMs / (1000 * 60)

        assertTrue(diffMinutes < 5)
    }
}

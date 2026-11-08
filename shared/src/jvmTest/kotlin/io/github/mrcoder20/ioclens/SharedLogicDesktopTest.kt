package io.github.mrcoder20.ioclens

import io.github.mrcoder20.ioclens.core.localization.AppLanguage
import io.github.mrcoder20.ioclens.core.localization.AppTheme
import io.github.mrcoder20.ioclens.core.storage.JvmFileKeyValueStorage
import io.github.mrcoder20.ioclens.data.repository.SettingsRepositoryImpl
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SharedLogicDesktopTest {

    @Test
    fun testJvmFileKeyValueStoragePersistenceAndRoundtrip() {
        val tempFile = File.createTempFile("ioclens_test_", ".properties")
        tempFile.deleteOnExit()

        val storage = JvmFileKeyValueStorage(tempFile)
        storage.setString("test_api_key", "secret-token-desktop-123")
        storage.setBoolean("privacy_guard", true)

        // Verify values via same instance
        assertEquals("secret-token-desktop-123", storage.getString("test_api_key", ""))
        assertTrue(storage.getBoolean("privacy_guard", false))

        // Re-load from storage file in fresh instance to prove file persistence
        val reloadedStorage = JvmFileKeyValueStorage(tempFile)
        assertEquals("secret-token-desktop-123", reloadedStorage.getString("test_api_key", ""))
        assertTrue(reloadedStorage.getBoolean("privacy_guard", false))

        tempFile.delete()
    }

    @Test
    fun testSettingsRepositoryDesktopRoundtrip() {
        val tempFile = File.createTempFile("ioclens_repo_", ".properties")
        tempFile.deleteOnExit()

        val storage = JvmFileKeyValueStorage(tempFile)
        val repo = SettingsRepositoryImpl(storage)

        // Set properties
        repo.setAppLanguage(AppLanguage.PERSIAN)
        repo.setAppTheme(AppTheme.DARK)
        repo.setPrivacyGuardEnabled(true)
        repo.updateApiKey("virustotal", "vt-desktop-key")
        repo.updateApiKey("abuseipdb", "abuse-desktop-key")
        repo.updateApiKey("alienvault", "alien-desktop-key")
        repo.updateApiKey("pulsedive", "pulse-desktop-key")
        repo.updateApiKey("ipqualityscore", "ipqs-desktop-key")

        // Validate state
        val state = repo.getSettings()
        assertEquals(AppLanguage.PERSIAN, state.appLanguage)
        assertEquals(AppTheme.DARK, state.appTheme)
        assertTrue(state.privacyGuardEnabled)
        assertEquals("vt-desktop-key", state.virusTotalApiKey)
        assertEquals("abuse-desktop-key", state.abuseIpDbApiKey)
        assertEquals("alien-desktop-key", state.alienVaultApiKey)
        assertEquals("pulse-desktop-key", state.pulsediveApiKey)
        assertEquals("ipqs-desktop-key", state.ipQualityScoreApiKey)

        // Re-instantiate repo from file to verify persistence
        val reloadedRepo = SettingsRepositoryImpl(JvmFileKeyValueStorage(tempFile))
        val reloadedState = reloadedRepo.getSettings()
        assertEquals(AppLanguage.PERSIAN, reloadedState.appLanguage)
        assertEquals(AppTheme.DARK, reloadedState.appTheme)
        assertEquals("vt-desktop-key", reloadedState.virusTotalApiKey)

        tempFile.delete()
    }
}

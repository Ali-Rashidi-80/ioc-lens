package io.github.mrcoder20.ioclens

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPosition
import io.github.mrcoder20.ioclens.core.localization.AppLanguage
import io.github.mrcoder20.ioclens.domain.security.SharedPayload
import java.awt.Insets
import java.awt.Rectangle
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopPolishVerificationTest {

    @Test
    fun testLargeScreenKeepsPreferredBounds() {
        val screenBounds = Rectangle(0, 0, 1920, 1080)
        val screenInsets = Insets(0, 0, 48, 0)

        val clamped = WindowBoundsClamper.computeClampedBounds(screenBounds, screenInsets)

        assertEquals(1040.dp, clamped.size.width)
        assertEquals(740.dp, clamped.size.height)

        val pos = clamped.position as WindowPosition.Absolute
        assertEquals(440.dp, pos.x)
        assertEquals(146.dp, pos.y)
    }

    @Test
    fun testLaptopScreenClampsHeightToPreventTaskbarOverlap() {
        val screenBounds = Rectangle(0, 0, 1366, 768)
        val screenInsets = Insets(0, 0, 48, 0) // Standard 48px bottom taskbar

        val clamped = WindowBoundsClamper.computeClampedBounds(screenBounds, screenInsets)

        assertEquals(1040.dp, clamped.size.width)
        // Usable height is 720. 720 - 32 safety margin = 688dp
        assertEquals(688.dp, clamped.size.height)

        val pos = clamped.position as WindowPosition.Absolute
        val bottomEdge = pos.y.value.toInt() + clamped.size.height.value.toInt()
        val taskbarStartY = screenBounds.height - screenInsets.bottom // 720

        assertTrue(
            bottomEdge <= taskbarStartY,
            "Window bottom edge ($bottomEdge) must not overlap taskbar start ($taskbarStartY)"
        )
    }

    @Test
    fun testSmallScreenClampsWidthAndHeight() {
        val screenBounds = Rectangle(0, 0, 1024, 768)
        val screenInsets = Insets(24, 0, 48, 0)

        val clamped = WindowBoundsClamper.computeClampedBounds(screenBounds, screenInsets)

        assertTrue(clamped.size.width.value <= 1024 - 32)
        assertTrue(clamped.size.height.value <= (768 - 24 - 48 - 32))
    }

    @Test
    fun testHeadlessFallbackReturnsDefaultWithoutCrashing() {
        val defaults = WindowBoundsClamper.defaultBounds()
        assertEquals(1040.dp, defaults.size.width)
        assertEquals(740.dp, defaults.size.height)

        val initial = WindowBoundsClamper.resolveInitialBounds()
        assertNotNull(initial)
        assertTrue(initial.size.width.value in 380f..1040f, "Initial width ${initial.size.width} out of range")
        assertTrue(initial.size.height.value in 520f..740f, "Initial height ${initial.size.height} out of range")
    }

    @Test
    fun testRegularFileDispatchesFilePathAndName() {
        val tempFile = File.createTempFile("test_sample", ".bin")
        try {
            var receivedPayload: SharedPayload? = null
            val handled = DesktopDropHandler.processDroppedFiles(listOf(tempFile), AppLanguage.ENGLISH) {
                receivedPayload = it
            }

            assertTrue(handled)
            val payload = checkNotNull(receivedPayload)
            assertEquals(tempFile.absolutePath, payload.filePath)
            assertEquals(tempFile.name, payload.fileName)
            assertFalse(payload.isTemporary)
            assertNull(payload.errorMessage)
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun testDirectoryDropDispatchesCleanErrorMessageWithoutHashing() {
        val tempDir = Files.createTempDirectory("malware_folder").toFile()
        try {
            var receivedPayload: SharedPayload? = null
            val handled = DesktopDropHandler.processDroppedFiles(listOf(tempDir), AppLanguage.ENGLISH) {
                receivedPayload = it
            }

            assertTrue(handled)
            val payload = checkNotNull(receivedPayload)
            assertNull(payload.filePath, "Directories must NOT set filePath")
            val errorMsg = checkNotNull(payload.errorMessage)
            assertTrue(errorMsg.contains("Cannot analyze folder '${tempDir.name}'"))
        } finally {
            tempDir.delete()
        }
    }

    @Test
    fun testDirectoryDropLocalizedPersianAndRussian() {
        val tempDir = Files.createTempDirectory("target_dir").toFile()
        try {
            var faPayload: SharedPayload? = null
            DesktopDropHandler.processDroppedFiles(listOf(tempDir), AppLanguage.PERSIAN) {
                faPayload = it
            }
            assertTrue(faPayload?.errorMessage?.contains("پوشه «${tempDir.name}» قابل تحلیل نیست") == true)

            var ruPayload: SharedPayload? = null
            DesktopDropHandler.processDroppedFiles(listOf(tempDir), AppLanguage.RUSSIAN) {
                ruPayload = it
            }
            assertTrue(ruPayload?.errorMessage?.contains("Каталог «${tempDir.name}» не поддерживается") == true)
        } finally {
            tempDir.delete()
        }
    }

    @Test
    fun testStartupArgDirectoryDispatchesErrorMessage() {
        val tempDir = Files.createTempDirectory("cli_dir").toFile()
        try {
            var receivedPayload: SharedPayload? = null
            DesktopDropHandler.processStartupArg(tempDir.absolutePath, AppLanguage.ENGLISH) {
                receivedPayload = it
            }
            assertNotNull(receivedPayload?.errorMessage)
            assertTrue(receivedPayload.errorMessage!!.contains("Cannot analyze folder '${tempDir.name}'"))
            assertNull(receivedPayload.filePath)
        } finally {
            tempDir.delete()
        }
    }

    @Test
    fun testUncAndRemoteFilePathsAreBlockedBeforeFilesystemIo() {
        assertTrue(DesktopDropHandler.isUnsafeUncOrRemotePath("\\\\attacker.example\\share\\payload.exe"))
        assertTrue(DesktopDropHandler.isUnsafeUncOrRemotePath("\\\\?\\UNC\\10.0.0.5\\share\\evil.dll"))
        assertTrue(DesktopDropHandler.isUnsafeUncOrRemotePath("file://evil.example/share/sample.bin"))
        assertFalse(DesktopDropHandler.isUnsafeUncOrRemotePath("file:///C:/Users/Public/sample.bin"))

        var payload: SharedPayload? = null
        DesktopDropHandler.processStartupArg("\\\\192.0.2.50\\smb\\leak.exe", AppLanguage.ENGLISH) {
            payload = it
        }
        val uncPayload = checkNotNull(payload)
        assertNotNull(uncPayload.errorMessage)
        assertNull(uncPayload.filePath)
        assertNull(uncPayload.textOrUrl)
    }

    @Test
    fun testCliFlagsAreIgnoredWithoutTriggeringScanPayload() {
        var payload: SharedPayload? = null
        DesktopDropHandler.processStartupArg("--minimized", AppLanguage.ENGLISH) {
            payload = it
        }
        assertNull(payload)

        DesktopDropHandler.processStartupArg("-psn_0_1234", AppLanguage.ENGLISH) {
            payload = it
        }
        assertNull(payload)
    }
}

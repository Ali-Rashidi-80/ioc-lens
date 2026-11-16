package io.github.mrcoder20.ioclens.presentation.vision

import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTargetDropEvent
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopDragAndDropCoordinatorTest {

    private lateinit var tempImageFile: File
    private lateinit var tempTextFile: File

    @BeforeTest
    fun setUp() {
        tempImageFile = File.createTempFile("sample_qr_", ".png")
        tempImageFile.writeBytes(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)) // PNG header
        tempTextFile = File.createTempFile("notes_", ".txt")
        tempTextFile.writeText("sample hash content")
        DesktopDragAndDropCoordinator.visionDropHandler = null
        DesktopDragAndDropCoordinator.visionImageDropHandler = null
    }

    @AfterTest
    fun tearDown() {
        tempImageFile.delete()
        tempTextFile.delete()
        DesktopDragAndDropCoordinator.visionDropHandler = null
        DesktopDragAndDropCoordinator.visionImageDropHandler = null
    }

    @Test
    fun testIsImageFileIdentifiesImageExtensions() {
        assertTrue(DesktopDragAndDropCoordinator.isImageFile(tempImageFile))
        assertFalse(DesktopDragAndDropCoordinator.isImageFile(tempTextFile))
        assertFalse(DesktopDragAndDropCoordinator.isImageFile(File("nonexistent.png")))
    }

    @Test
    fun testResolveDropActionNeverFails() {
        // Source offers only MOVE
        val action1 = DesktopDragAndDropCoordinator.resolveDropAction(DnDConstants.ACTION_MOVE)
        assertEquals(DnDConstants.ACTION_MOVE, action1)

        // Source offers COPY or MOVE
        val action2 = DesktopDragAndDropCoordinator.resolveDropAction(DnDConstants.ACTION_COPY_OR_MOVE)
        assertEquals(DnDConstants.ACTION_COPY, action2)

        // Source offers only LINK
        val action3 = DesktopDragAndDropCoordinator.resolveDropAction(DnDConstants.ACTION_LINK)
        assertEquals(DnDConstants.ACTION_LINK, action3)

        // Fallback default
        val action4 = DesktopDragAndDropCoordinator.resolveDropAction(0)
        assertEquals(DnDConstants.ACTION_COPY, action4)
    }

    @Test
    fun testParseDroppedTextToFile() {
        // Plain path
        val parsed1 = DesktopDragAndDropCoordinator.parseDroppedTextToFile(tempImageFile.absolutePath)
        assertNotNull(parsed1)
        assertEquals(tempImageFile.absolutePath, parsed1.absolutePath)

        // Quoted path
        val parsed2 = DesktopDragAndDropCoordinator.parseDroppedTextToFile("\"${tempImageFile.absolutePath}\"")
        assertNotNull(parsed2)
        assertEquals(tempImageFile.absolutePath, parsed2.absolutePath)

        // file:// URI
        val parsed3 = DesktopDragAndDropCoordinator.parseDroppedTextToFile(tempImageFile.toURI().toString())
        assertNotNull(parsed3)
        assertEquals(tempImageFile.canonicalPath, parsed3.canonicalPath)

        // Blank or comment lines
        assertNull(DesktopDragAndDropCoordinator.parseDroppedTextToFile("# Comment\n\n"))
        assertNull(DesktopDragAndDropCoordinator.parseDroppedTextToFile("not_a_real_path_12345.bin"))
    }

    @Test
    fun testVisionDropHandlerLifecycle() {
        assertFalse(DesktopDragAndDropCoordinator.isVisionActive())
        var droppedFile: File? = null
        DesktopDragAndDropCoordinator.visionDropHandler = { droppedFile = it }
        assertTrue(DesktopDragAndDropCoordinator.isVisionActive())

        DesktopDragAndDropCoordinator.visionDropHandler?.invoke(tempImageFile)
        assertEquals(tempImageFile, droppedFile)

        DesktopDragAndDropCoordinator.visionDropHandler = null
        assertFalse(DesktopDragAndDropCoordinator.isVisionActive())
    }
}

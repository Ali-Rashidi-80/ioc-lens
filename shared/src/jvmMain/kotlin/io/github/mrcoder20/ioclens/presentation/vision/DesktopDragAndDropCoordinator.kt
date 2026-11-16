package io.github.mrcoder20.ioclens.presentation.vision

import java.awt.Component
import java.awt.Image
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.awt.dnd.DropTargetEvent
import java.awt.dnd.DropTargetListener
import java.io.File

/**
 * Enterprise Desktop Drag-and-Drop Coordinator:
 * Coordinates native Windows OLE / macOS / Linux drag-and-drop transactions across
 * Compose Desktop windows, dialogs, and overlays.
 *
 * Prevents:
 * 1. OpenJDK JDK-8305479 / JDK-8305194 OLE thread deadlocks by registering exactly ONE DropTarget per Window.
 * 2. Unchecked acceptDrag IllegalArgumentException causing the 🚫 prohibited cursor.
 * 3. Mouse cursor and drag thumbnail lock-ups by immediately acknowledging dropComplete(true) before async decoding.
 */
object DesktopDragAndDropCoordinator {
    // Active IOC Vision scan listeners (registered when dialog is open)
    @Volatile
    var visionDropHandler: ((File) -> Unit)? = null
    @Volatile
    var visionImageDropHandler: ((Image) -> Unit)? = null

    fun isVisionActive(): Boolean = visionDropHandler != null

    private val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "webp", "bmp", "gif")

    fun isImageFile(file: File): Boolean {
        return file.isFile && file.extension.lowercase() in IMAGE_EXTENSIONS
    }

    /**
     * Resolves an acceptable drop action bitmap against the offered source actions.
     */
    fun resolveDropAction(sourceActions: Int, preferredAction: Int = DnDConstants.ACTION_COPY): Int {
        return when {
            (sourceActions and DnDConstants.ACTION_COPY) != 0 -> DnDConstants.ACTION_COPY
            (sourceActions and DnDConstants.ACTION_MOVE) != 0 -> DnDConstants.ACTION_MOVE
            (sourceActions and DnDConstants.ACTION_LINK) != 0 -> DnDConstants.ACTION_LINK
            (preferredAction and sourceActions) != 0 -> preferredAction
            else -> DnDConstants.ACTION_COPY
        }
    }

    /**
     * Parses dropped raw text, file URI, or URI list into a valid local File.
     */
    fun parseDroppedTextToFile(rawText: String): File? {
        if (rawText.isBlank()) return null
        for (rawLine in rawText.lines()) {
            val line = rawLine.trim()
            if (line.isBlank() || line.startsWith("#")) continue
            val candidate = runCatching {
                if (line.startsWith("file:", ignoreCase = true)) {
                    File(java.net.URI(line))
                } else {
                    File(line.removeSurrounding("\""))
                }
            }.getOrNull()
            if (candidate != null && candidate.isFile && candidate.exists()) {
                return candidate
            }
        }
        return null
    }

    /**
     * Builds a production-grade DropTargetListener that instantly frees the native OLE lock
     * and routes payloads deterministically to either IOC Vision or the default destination.
     */
    fun createSafeDropListener(
        onFilesDropped: (List<File>) -> Unit,
        onImageDropped: ((Image) -> Unit)? = null,
        onTextDropped: ((String) -> Unit)? = null
    ): DropTargetListener {
        return object : DropTargetListener {
            override fun dragEnter(dtde: DropTargetDragEvent) {
                try {
                    val action = resolveDropAction(dtde.sourceActions, dtde.dropAction)
                    dtde.acceptDrag(action)
                } catch (_: Throwable) {
                    // Never call rejectDrag() to avoid showing the prohibited cursor
                }
            }

            override fun dragOver(dtde: DropTargetDragEvent) {
                try {
                    val action = resolveDropAction(dtde.sourceActions, dtde.dropAction)
                    dtde.acceptDrag(action)
                } catch (_: Throwable) {}
            }

            override fun dropActionChanged(dtde: DropTargetDragEvent) {
                try {
                    val action = resolveDropAction(dtde.sourceActions, dtde.dropAction)
                    dtde.acceptDrag(action)
                } catch (_: Throwable) {}
            }

            override fun dragExit(dte: DropTargetEvent) {}

            override fun drop(evt: DropTargetDropEvent) {
                val action = resolveDropAction(evt.sourceActions, evt.dropAction)
                try {
                    evt.acceptDrop(action)
                } catch (_: Throwable) {
                    return
                }

                var handled = false
                try {
                    val transferable = evt.transferable

                    // 1. Files flavor (File Explorer / Desktop)
                    if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        val files = (transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<*>)
                            ?.filterIsInstance<File>()
                            ?.filter { it.exists() }
                            .orEmpty()
                        if (files.isNotEmpty()) {
                            handled = true
                            evt.dropComplete(true) // Release Windows OLE lock IMMEDIATELY
                            onFilesDropped(files)
                            return
                        }
                    }

                    // 2. Direct Image flavor (Snipping Tool, Paint, Web Browser images)
                    if (onImageDropped != null && transferable.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                        val img = transferable.getTransferData(DataFlavor.imageFlavor) as? Image
                        if (img != null) {
                            handled = true
                            evt.dropComplete(true) // Release Windows OLE lock IMMEDIATELY
                            onImageDropped.invoke(img)
                            return
                        }
                    }

                    // 3. String flavor (text, file path, file:// URI)
                    if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                        val text = (transferable.getTransferData(DataFlavor.stringFlavor) as? String)?.trim().orEmpty()
                        if (text.isNotBlank()) {
                            val candidateFile = parseDroppedTextToFile(text)
                            if (candidateFile != null) {
                                handled = true
                                evt.dropComplete(true) // Release Windows OLE lock IMMEDIATELY
                                onFilesDropped(listOf(candidateFile))
                                return
                            } else if (onTextDropped != null) {
                                handled = true
                                evt.dropComplete(true) // Release Windows OLE lock IMMEDIATELY
                                onTextDropped.invoke(text)
                                return
                            }
                        }
                    }

                    // 4. URI-List flavor (text/uri-list)
                    val uriListFlavor = runCatching { DataFlavor("text/uri-list;class=java.lang.String") }.getOrNull()
                    if (uriListFlavor != null && transferable.isDataFlavorSupported(uriListFlavor)) {
                        val text = (transferable.getTransferData(uriListFlavor) as? String)?.trim().orEmpty()
                        if (text.isNotBlank()) {
                            val candidateFile = parseDroppedTextToFile(text)
                            if (candidateFile != null) {
                                handled = true
                                evt.dropComplete(true) // Release Windows OLE lock IMMEDIATELY
                                onFilesDropped(listOf(candidateFile))
                                return
                            }
                        }
                    }
                } catch (t: Throwable) {
                    System.err.println("[IOCLens] DnD drop error: ${t.message}")
                } finally {
                    if (!handled) {
                        try { evt.dropComplete(false) } catch (_: Throwable) {}
                    }
                }
            }
        }
    }
}

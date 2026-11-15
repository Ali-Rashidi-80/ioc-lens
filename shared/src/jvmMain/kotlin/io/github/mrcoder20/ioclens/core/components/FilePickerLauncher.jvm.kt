package io.github.mrcoder20.ioclens.core.components

import androidx.compose.runtime.Composable
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.UIManager

private val isSinglePickerOpen = AtomicBoolean(false)
private val isBatchPickerOpen = AtomicBoolean(false)

@Composable
actual fun rememberFilePickerLauncher(
    onFileSelected: (filePath: String, fileName: String) -> Unit
): () -> Unit {
    return {
        if (isSinglePickerOpen.compareAndSet(false, true)) {
            SwingUtilities.invokeLater {
                try {
                    val activeFrame = Frame.getFrames().firstOrNull { it.isShowing && it.isFocused }
                        ?: Frame.getFrames().firstOrNull { it.isShowing }

                    var pickedPath: String? = null
                    var pickedName: String? = null
                    var nativeDialogAttempted = false

                    if (activeFrame != null) {
                        try {
                            nativeDialogAttempted = true
                            val dialog = FileDialog(activeFrame, "Select File to Scan", FileDialog.LOAD)
                            dialog.isVisible = true
                            val dir = dialog.directory
                            val file = dialog.file
                            if (dir != null && file != null) {
                                val f = File(dir, file)
                                if (f.exists()) {
                                    pickedPath = f.absolutePath
                                    pickedName = file
                                }
                            }
                        } catch (t: Throwable) {
                            nativeDialogAttempted = false
                            System.err.println("Native FileDialog failed, falling back to JFileChooser: ${t.message}")
                        }
                    }

                    // ONLY fallback to JFileChooser if native FileDialog threw an exception or could not be created
                    if (!nativeDialogAttempted) {
                        try {
                            runCatching { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()) }
                            val chooser = JFileChooser()
                            chooser.dialogTitle = "Select File to Scan"
                            chooser.fileSelectionMode = JFileChooser.FILES_ONLY
                            val result = chooser.showOpenDialog(activeFrame)
                            if (result == JFileChooser.APPROVE_OPTION && chooser.selectedFile != null) {
                                pickedPath = chooser.selectedFile.absolutePath
                                pickedName = chooser.selectedFile.name
                            }
                        } catch (t: Throwable) {
                            System.err.println("Swing JFileChooser fallback failed: ${t.message}")
                        }
                    }

                    if (pickedPath != null && pickedName != null) {
                        onFileSelected(pickedPath, pickedName)
                    }
                } catch (t: Throwable) {
                    System.err.println("FilePicker error suppressed: ${t.message}")
                } finally {
                    isSinglePickerOpen.set(false)
                }
            }
        }
    }
}

@Composable
actual fun rememberBatchFilePickerLauncher(
    onFilesSelected: (List<PickedFile>) -> Unit
): () -> Unit {
    return {
        if (isBatchPickerOpen.compareAndSet(false, true)) {
            SwingUtilities.invokeLater {
                try {
                    val activeFrame = Frame.getFrames().firstOrNull { it.isShowing && it.isFocused }
                        ?: Frame.getFrames().firstOrNull { it.isShowing }

                    val picked = mutableListOf<PickedFile>()
                    var nativeDialogAttempted = false

                    // Try native AWT FileDialog in multiple mode
                    if (activeFrame != null) {
                        try {
                            nativeDialogAttempted = true
                            val dialog = FileDialog(activeFrame, "Select Files to Scan", FileDialog.LOAD)
                            dialog.isMultipleMode = true
                            dialog.isVisible = true
                            val files = dialog.files
                            if (!files.isNullOrEmpty()) {
                                files.forEach { file ->
                                    if (file.isFile && file.exists()) {
                                        picked.add(PickedFile(file.absolutePath, file.name, file.length()))
                                    }
                                }
                            } else {
                                val dir = dialog.directory
                                val singleFile = dialog.file
                                if (dir != null && singleFile != null) {
                                    val f = File(dir, singleFile)
                                    if (f.isFile && f.exists()) {
                                        picked.add(PickedFile(f.absolutePath, f.name, f.length()))
                                    }
                                }
                            }
                        } catch (t: Throwable) {
                            nativeDialogAttempted = false
                            System.err.println("Native FileDialog multi-mode failed: ${t.message}")
                        }
                    }

                    // ONLY fallback to JFileChooser if native FileDialog threw an exception or was impossible
                    if (!nativeDialogAttempted) {
                        try {
                            runCatching { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()) }
                            val chooser = JFileChooser()
                            chooser.dialogTitle = "Select Files to Scan"
                            chooser.isMultiSelectionEnabled = true
                            chooser.fileSelectionMode = JFileChooser.FILES_ONLY
                            val result = chooser.showOpenDialog(activeFrame)
                            if (result == JFileChooser.APPROVE_OPTION && !chooser.selectedFiles.isNullOrEmpty()) {
                                chooser.selectedFiles.forEach { file ->
                                    if (file.isFile && file.exists()) {
                                        picked.add(PickedFile(file.absolutePath, file.name, file.length()))
                                    }
                                }
                            }
                        } catch (t: Throwable) {
                            System.err.println("Swing JFileChooser multi-selection failed: ${t.message}")
                        }
                    }

                    if (picked.isNotEmpty()) {
                        onFilesSelected(picked)
                    }
                } catch (t: Throwable) {
                    System.err.println("Batch FilePicker error: ${t.message}")
                } finally {
                    isBatchPickerOpen.set(false)
                }
            }
        }
    }
}

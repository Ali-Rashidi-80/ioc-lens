@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package io.github.mrcoder20.ioclens.core.concurrency

import platform.Foundation.NSRecursiveLock

actual class PlatformLock actual constructor() {
    private val delegate = NSRecursiveLock()
    actual fun lock() {
        delegate.lock()
    }

    actual fun unlock() {
        delegate.unlock()
    }
}

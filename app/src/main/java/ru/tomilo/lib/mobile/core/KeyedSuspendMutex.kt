package ru.tomilo.lib.mobile.core

import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.ConcurrentHashMap

/** A per-key suspend lock registry that drops idle keys instead of growing for the process lifetime. */
internal class KeyedSuspendMutex<K : Any> {
    private class Entry {
        val mutex = Mutex()

        // Updated only inside ConcurrentHashMap.compute for this key.
        var users = 0
    }

    private val entries = ConcurrentHashMap<K, Entry>()

    internal val activeKeyCount: Int
        get() = entries.size

    suspend fun <T> withLock(key: K, action: suspend () -> T): T {
        val entry = entries.compute(key) { _, current ->
            (current ?: Entry()).also { it.users++ }
        } ?: error("Could not register keyed mutex")

        try {
            entry.mutex.lock()
            try {
                return action()
            } finally {
                entry.mutex.unlock()
            }
        } finally {
            entries.compute(key) { _, current ->
                if (current !== entry) current
                else {
                    entry.users--
                    current.takeIf { entry.users > 0 }
                }
            }
        }
    }
}

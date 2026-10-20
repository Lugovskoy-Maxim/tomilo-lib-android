package ru.tomilo.lib.mobile.data.repo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal object CatalogRequestKeys {
    fun latestUpdates(page: Int, limit: Int, includeAdult: Boolean): String? =
        if (page == 1 && !includeAdult) "updates:$limit:adult=false" else null

    fun popular(limit: Int, includeAdult: Boolean?): String? =
        if (includeAdult == false) "popular:$limit:adult=false" else null

    fun randomTitles(limit: Int, includeAdult: Boolean): String? =
        if (!includeAdult && limit > 1) "random:$limit:adult=false" else null
}

/** Shares overlapping, identical public discovery requests without coupling them to one caller's cancellation. */
internal class CatalogRequestCoalescer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Mutex()
    private val inFlight = mutableMapOf<String, kotlinx.coroutines.Deferred<Any?>>()

    suspend fun <T> run(key: String?, request: suspend () -> T): T {
        if (key == null) return request()

        val deferred = lock.withLock {
            @Suppress("UNCHECKED_CAST")
            val existing = inFlight[key] as? kotlinx.coroutines.Deferred<T>
            if (existing?.isActive == true) {
                existing
            } else {
                scope.async(start = kotlinx.coroutines.CoroutineStart.LAZY) { request() }.also { created ->
                    inFlight[key] = created as kotlinx.coroutines.Deferred<Any?>
                    created.invokeOnCompletion {
                        scope.launch {
                            lock.withLock {
                                if (inFlight[key] === created) inFlight.remove(key)
                            }
                        }
                    }
                    created.start()
                }
            }
        }
        return deferred.await()
    }
}

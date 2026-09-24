package ru.tomilo.lib.mobile.data.download

import android.content.Context

/** Small durable checkpoint so a sticky service can recover after process recreation. */
internal class DownloadQueueStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun save(request: DownloadBatchRequest) {
        check(preferences.edit().putString(KEY_REQUEST, DownloadBatchRequestCodec.encode(request)).commit()) {
            "Не удалось сохранить очередь офлайн-загрузки"
        }
    }

    fun load(): DownloadBatchRequest? {
        val value = preferences.getString(KEY_REQUEST, null) ?: return null
        return DownloadBatchRequestCodec.decode(value) ?: run {
            clear()
            null
        }
    }

    fun clear() {
        preferences.edit().remove(KEY_REQUEST).commit()
    }

    private companion object {
        const val FILE_NAME = "tomilo_download_queue"
        const val KEY_REQUEST = "pending_batch"
    }
}

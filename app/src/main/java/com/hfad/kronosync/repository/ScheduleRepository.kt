package com.hfad.kronosync.repository

import com.hfad.kronosync.model.XmlScheduleDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScheduleRepository(private val dataSource: XmlScheduleDataSource) {
    fun fetchScheduleData(
        urlString: String,
        callback: (Result<List<XmlScheduleDataSource.EventItem>>) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // eftersom fetchScheduleData är en suspend-funktion, kalas den direkt här
                val result = dataSource.fetchScheduleData(urlString)
                callback(result)
            } catch (e: Exception) {
                callback(Result.failure(e))
            }
        }
    }
}

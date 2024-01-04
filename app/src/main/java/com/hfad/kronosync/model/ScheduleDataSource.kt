package com.hfad.kronosync.model

interface ScheduleDataSource {
    suspend fun fetchScheduleData(urlString: String): Result<List<XmlScheduleDataSource.EventItem>>
}
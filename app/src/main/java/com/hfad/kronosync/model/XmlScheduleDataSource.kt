@file:Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")

package com.hfad.kronosync.model

import android.text.Html
import android.text.Spanned
import android.text.SpannedString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.net.HttpURLConnection
import java.net.URL

class XmlScheduleDataSource : ScheduleDataSource {
    override suspend fun fetchScheduleData(urlString: String): Result<List<EventItem>> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(urlString)
                val urlConnection = url.openConnection() as HttpURLConnection
                urlConnection.requestMethod = "GET"
                urlConnection.connect()

                val xmlContent = urlConnection.inputStream.bufferedReader().use { it.readText() }
                urlConnection.disconnect()

                val parsedData = parseXmlContent(xmlContent)
                Result.success(parsedData)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun parseXmlContent(xmlContent: String): List<EventItem> {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val xmlPullParser = factory.newPullParser()
        xmlPullParser.setInput(xmlContent.reader())
        val courseMap = mutableMapOf<String, String>()
        val locationMap = mutableMapOf<String, String>()

        var eventType = xmlPullParser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && xmlPullParser.name == "forklaringsrader") {
                val type = xmlPullParser.getAttributeValue(null, "typ")
                if (type == "UTB_KURSINSTANS_GRUPPER") {
                    parseCourseInformation(xmlPullParser, courseMap)
                } else if (type == "RESURSER_LOKALER") {
                    parseLocationInformation(xmlPullParser, locationMap)
                }
            }
            eventType = xmlPullParser.next()
        }

        xmlPullParser.setInput(xmlContent.reader())


        return parseForScheduleItems(xmlPullParser, courseMap, locationMap)
    }

    private fun parseForScheduleItems(
        xmlPullParser: XmlPullParser,
        courseMap: Map<String, String>,
        locationMap: Map<String, String>
    ): List<EventItem> {
        val eventItems = mutableListOf<EventItem>()
        var eventType = xmlPullParser.eventType
        var eventItem = EventItem("", "", "", "", "", "", parsedDescription = SpannedString(""))
        var currentTag = ""
        var isInsideBokatDatum = false
        var isInsideResursNod = false
        var currentResursTypId = ""

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = xmlPullParser.name
                    when (currentTag) {
                        "bokatDatum" -> {
                            isInsideBokatDatum = true
                            eventItem = eventItem.copy(
                                startTime = xmlPullParser.getAttributeValue(null, "startTid") ?: "",
                                endTime = xmlPullParser.getAttributeValue(null, "slutTid") ?: ""
                            )
                        }

                        "resursNod" -> {
                            isInsideResursNod = true
                            currentResursTypId =
                                xmlPullParser.getAttributeValue(null, "resursTypId") ?: ""
                        }
                    }
                }

                XmlPullParser.TEXT -> {
                    if ((currentTag == "moment") && eventType == XmlPullParser.TEXT) {
                        val htmlContent = xmlPullParser.text
                        val styledText =
                            Html.fromHtml(htmlContent, Html.FROM_HTML_MODE_COMPACT)
                        eventItem = eventItem.copy(moment = styledText.toString())
                    }


                    if (isInsideResursNod && currentResursTypId == "UTB_KURSINSTANS_GRUPPER" && currentTag == "resursId") {
                        eventItem = eventItem.copy(courseId = xmlPullParser.text)
                    }
                    if (isInsideResursNod && currentResursTypId == "RESURSER_LOKALER" && currentTag == "resursId") {
                        eventItem = eventItem.copy(locationId = xmlPullParser.text)
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (xmlPullParser.name == "bokatDatum") {
                        isInsideBokatDatum = false
                    }
                    if (xmlPullParser.name == "resursNod") {
                        isInsideResursNod = false
                        currentResursTypId = ""
                    }
                    if (xmlPullParser.name == "schemaPost") {
                        eventItem.courseName = courseMap[eventItem.courseId] ?: "Okänd kurs"
                        eventItem.locationName = locationMap[eventItem.locationId] ?: "Okänd lokal"
                        eventItems.add(eventItem)
                        eventItem = EventItem(
                            "",
                            "",
                            "",
                            "",
                            "",
                            "",
                            parsedDescription = SpannedString("")
                        ) // startrar om på nytt
                    }
                }
            }
            eventType = xmlPullParser.next()
        }
        return eventItems
    }

    private fun parseCourseInformation(
        xmlPullParser: XmlPullParser,
        courseMap: MutableMap<String, String>
    ) {
        while (xmlPullParser.eventType != XmlPullParser.END_TAG || xmlPullParser.name != "forklaringsrader") {
            if (xmlPullParser.eventType == XmlPullParser.START_TAG && xmlPullParser.name == "rad") {
                var courseId = ""
                var courseName = ""
                while (xmlPullParser.eventType != XmlPullParser.END_TAG || xmlPullParser.name != "rad") {
                    if (xmlPullParser.eventType == XmlPullParser.START_TAG && xmlPullParser.name == "kolumn") {
                        val columnName = xmlPullParser.getAttributeValue(null, "rubrik")
                        xmlPullParser.next()
                        if (columnName == "Id") {
                            courseId = xmlPullParser.text
                        } else if (columnName == "KursNamn_SV") {
                            courseName = xmlPullParser.text
                        }
                    }
                    xmlPullParser.next()
                }
                courseMap[courseId] = courseName
            }
            xmlPullParser.next()
        }
    }

    private fun parseLocationInformation(
        xmlPullParser: XmlPullParser,
        locationMap: MutableMap<String, String>
    ) {
        while (xmlPullParser.eventType != XmlPullParser.END_TAG || xmlPullParser.name != "forklaringsrader") {
            if (xmlPullParser.eventType == XmlPullParser.START_TAG && xmlPullParser.name == "rad") {
                var locationId = ""
                var locationName = ""
                while (xmlPullParser.eventType != XmlPullParser.END_TAG || xmlPullParser.name != "rad") {
                    if (xmlPullParser.eventType == XmlPullParser.START_TAG && xmlPullParser.name == "kolumn") {
                        val columnName = xmlPullParser.getAttributeValue(null, "rubrik")
                        xmlPullParser.next()
                        if (columnName == "Id") {
                            locationId = xmlPullParser.text
                        } else if (columnName == "Lokalnamn") {
                            locationName = xmlPullParser.text
                        }
                    }
                    xmlPullParser.next()
                }
                locationMap[locationId] = locationName
            }
            xmlPullParser.next()
        }
    }


    data class EventItem(
        val startTime: String,
        val endTime: String,
        var moment: String,
        var courseId: String,
        var locationId: String,
        var courseName: String = "",
        var locationName: String = "",
        var parsedDescription: Spanned
    )
}







package org.sai.app.model

import org.springframework.kotlin.coroutine.annotation.Coroutine
import reactor.core.publisher.Flux

data class ProximityEventsIngestRequest(var userIdHash: String,
                                        val proximityEvents: List<ProximityEvent> = listOf())

data class ProximityEvent(val userIdHash: String,
                          val timestamp: Long = System.currentTimeMillis(),
                          val geoLocation: String,
                          val tags: List<String> = listOf(),
                          val channel: ChannelType = ChannelType.BluetoothSensing,
                          val canonicalLocationName: String = "",
                          val proximityType: ProximityType = ProximityType.In) {
    var count = 0

    init {
        count = if (proximityType == ProximityType.In || count == 0) 1 else -1
    }
}

enum class ChannelType {
    BluetoothSensing, Beacon, Gps
}

enum class ProximityType {
    In, Out
}

data class RealtimeCountCriteria(val timestamp: Long, val canonicalLocationName: String, val criteria: List<String> = emptyList())

interface ProximityEventsIngestService {
    @Coroutine("PROXIMITY_EVENTS_INGEST")
    suspend fun ingest(proximityEventsIngestRequest: ProximityEventsIngestRequest): Map<String, String>

    fun count(realtimeCountCriteria: RealtimeCountCriteria): Flux<Long>
}

interface RealtimeCountRepository {
    @Coroutine("PROXIMITY_EVENTS_COUNTER_INGEST")
    suspend fun save(proximityEvents: List<ProximityEvent>)

    fun count(realtimeCountCriteria: RealtimeCountCriteria): Flux<Long>
}

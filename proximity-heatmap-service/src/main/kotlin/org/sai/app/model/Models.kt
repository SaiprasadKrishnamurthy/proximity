package org.sai.app.model

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

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
data class HeatMapResponse(val range: String, val risks : Map<String,Long>)
data class CountResponse(val range: String, val risks: Long)


interface ProximityHeatMapService {
    fun heatmap(location: String): Mono<List<HeatMapResponse>>
    fun count(realtimeCountCriteria: RealtimeCountCriteria): Flux<CountResponse>
}



package org.sai.app.model

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

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



package org.sai.app.controller

import com.flopanda.ingest.interceptor.WebInterceptor
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.sai.app.model.ProximityEventsIngestRequest
import org.sai.app.model.ProximityEventsIngestService
import org.sai.app.model.RealtimeCountCriteria
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import java.time.Duration


/**
 * Ingests the events into the database.
 * @author Sai.
 */
@RestController
@RequestMapping("/api/v1")
@CrossOrigin
class IngestController(private val proximityEventsIngestService: ProximityEventsIngestService) {

    @PostMapping("/ingest", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun ingest(@RequestHeader(WebInterceptor.API_KEY, required = false) apiKey: String,
                       @RequestBody proximityEventsIngestRequest: ProximityEventsIngestRequest): Map<String, String> {
        submitAsync(proximityEventsIngestRequest)
        return mapOf("status" to "SUBMITTED")
    }

    private fun submitAsync(proximityEventsIngestRequest: ProximityEventsIngestRequest) = GlobalScope.launch {
        proximityEventsIngestService.ingest(proximityEventsIngestRequest)
    }

    @GetMapping("/count", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun count(@RequestParam("locationName") locationName: String,
              @RequestParam("refreshIntervalInSeconds", defaultValue = "5") refreshIntervalInSeconds: Long,
              @RequestParam("criteria", required = false, defaultValue = "") criteria: String) =
            Flux.interval(Duration.ofSeconds(refreshIntervalInSeconds))
                    .flatMap { proximityEventsIngestService.count(RealtimeCountCriteria(System.currentTimeMillis(), locationName, criteria.split(",").toList())) }


}
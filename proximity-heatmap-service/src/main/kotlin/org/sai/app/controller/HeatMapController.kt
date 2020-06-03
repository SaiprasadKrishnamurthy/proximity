package org.sai.app.controller

import com.flopanda.ingest.interceptor.WebInterceptor
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.sai.app.model.*
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
class HeatMapController(private val proximityHeatMapService: ProximityHeatMapService) {

    @GetMapping("/heatmap", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun heatmap(@RequestParam("location") location: String) =
            proximityHeatMapService.heatmap( location)

    @GetMapping("/count-per-location", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun count(@RequestParam("locationName") locationName: String,
              @RequestParam("criteria", required = false, defaultValue = "") criteria: String) =
         proximityHeatMapService.count(RealtimeCountCriteria(System.currentTimeMillis(), locationName, criteria.split(",").toList()))

}
package org.sai.app.controller

import com.flopanda.ingest.interceptor.WebInterceptor
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.sai.app.model.*
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration


/**
 * Ingests the events into the database.
 * @author Sai.
 */
@RestController
@RequestMapping("/api/v1")
@CrossOrigin
class FlightDetailsController(private val proximityFlightDetailsService: ProximityFlightDetailsService) {

    @GetMapping("/flight-details/{pnr}/{lastName}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun get(@PathVariable("pnr") pnr: String, @PathVariable("lastName") lastName: String): Mono<FlightDetails> {
        return proximityFlightDetailsService.flightDetails(pnr, lastName)
    }

}
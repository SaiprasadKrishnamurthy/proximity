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
class PersonDetailsController(private val proximityPersonDetailsService: ProximityPersonDetailsService) {

    @GetMapping("/person-details/{personId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun get(@PathVariable("personId") personId: String) =
            proximityPersonDetailsService.get(personId)

    @PostMapping("/person-details" , consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun save(@RequestBody personDetails:PersonDetails)=
        proximityPersonDetailsService.save(personDetails)

    @GetMapping("/person-details", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun findAll() = proximityPersonDetailsService.findAll()
}
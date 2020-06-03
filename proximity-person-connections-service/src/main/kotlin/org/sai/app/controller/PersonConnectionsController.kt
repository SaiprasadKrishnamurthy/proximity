package org.sai.app.controller

import org.sai.app.model.ConnectedPersons
import org.sai.app.model.ProximityPersonConnectionsService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*


/**
 * Ingests the events into the database.
 * @author Sai.
 */
@RestController
@RequestMapping("/api/v1")
@CrossOrigin
class PersonConnectionsController(private val proximityPersonConnectionsService: ProximityPersonConnectionsService) {

    @GetMapping("/connected-persons", produces = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun connectedPersons(@RequestParam("userName") userName: String): List<ConnectedPersons> =
            proximityPersonConnectionsService.connectedPersons(userName)

}
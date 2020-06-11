package org.sai.app.controller

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.sai.app.model.PersonConnections
import org.sai.app.model.ProximityPersonConnectionsService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController


/**
 * Ingests the events into the database.
 * @author Sai.
 */
@RestController
@RequestMapping("/api/v1")
@Api(value = "Analysis Job Resource", tags = ["CONNECTIONS"])
class PersonConnectionsController(private val proximityPersonConnectionsService: ProximityPersonConnectionsService) {

    @ApiOperation(value = "proximal-connections", response = MutableMap::class)
    @ApiResponses(ApiResponse(code = 400, message = "Bad Request"),
            ApiResponse(code = 404, message = "Not found"),
            ApiResponse(code = 200, message = "OK"))
    @GetMapping("/connected-persons")
    fun connectedPersons(@RequestParam("userName") userName: String): PersonConnections =
            proximityPersonConnectionsService.connectedPersons(userName)

}
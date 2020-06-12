package org.sai.app.controller

import org.sai.app.model.Location
import org.sai.app.service.DefaultLocationSearchService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/api/v1")
@CrossOrigin
class LocationSearchController(private val locationSearchService: DefaultLocationSearchService) {

    @PostMapping("/location", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun createLocations(@RequestBody location: Location) =
            locationSearchService.createLocation(location)

    @PostMapping("/locations", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun createBulkLocations(@RequestBody locations: List<Location>) =
            locationSearchService.createBulk(locations)

    @GetMapping("/category/{category}/location", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun fetchBasedOnCategery(@PathVariable("category") category: String, @RequestParam("originPoint") originPoint: String, @RequestParam("distanceSortOrder", defaultValue = "desc") distanceSortOrder: String) =
            locationSearchService.getLocationByCategory(category, originPoint, distanceSortOrder)

    @GetMapping("/category/{category}/location/{name}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun searchLocation(@PathVariable("category") category: String, @PathVariable("name") name: String, @RequestParam("originPoint") originPoint: String, @RequestParam("distanceSortOrder", defaultValue = "desc") distanceSortOrder: String) =
            locationSearchService.searchLocation(category, name, originPoint, distanceSortOrder)

    @GetMapping("/categories", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getAllCategories() =
            locationSearchService.getAllLocationCategories()

    @GetMapping("/locations", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun fetchAll() =
            locationSearchService.getAllLocation()

}
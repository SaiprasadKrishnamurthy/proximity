package org.sai.app.model

import reactor.core.publisher.Mono

data class Location(var id: String?, val name: String?, val geoLocation : String?, val category: String?, val address: String?, val imageBase64: String?, val phoneNumber: String?, val description: List<String>?)

interface LocationService {
    fun createLocation(location: Location): Boolean
    fun createBulk(locations: List<Location>): Boolean
    fun getLocationByCategory(category: String, originPoint: String, distanceSortOrder: String): Mono<List<Location>>
    fun getAllLocation(): Mono<List<Location>>
    fun searchLocation(category: String, locationName: String, originPoint: String, distanceSortOrder: String): Mono<List<Location>>
    fun getAllLocationCategories(): List<String>
}
package org.sai.app.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.sai.app.model.Location
import org.sai.app.model.LocationService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.util.*


@Service
class DefaultLocationSearchService(@Value("\${proximityLocationDetailsIndexName}") val proximityLocationDetailsIndexName: String,
                                   @Value("\${es.prefix}") val es_prefix: String,
                                   val webClient: WebClient
) : LocationService {

    private val jackObjectMapper = jacksonObjectMapper()
    private var GET_MATCH_BY_CATEGORY = "elastic/match_category.json"
    private val SEARCH_LOCATION = "elastic/search_query.json"

    companion object {
        val LOG = LoggerFactory.getLogger(DefaultLocationSearchService::class.java)
    }

    override fun createLocation(location: Location): Boolean {
        if (StringUtils.isEmpty(location.id)) {
            location.id = UUID.randomUUID().toString()
        }
        val index = "$es_prefix$proximityLocationDetailsIndexName/location/${location.id}"
        LOG.info(" In service:  $index")

        webClient.post()
                .uri(index)
                .header("Content-Type", "application/json")
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(location))
                .retrieve()
                .bodyToMono(Map::class.java)
                .subscribe {
                    LOG.info(" Ingested to ES $it")
                }
        return true
    }

    override fun createBulk(locations: List<Location>): Boolean {
        val index = "$es_prefix$proximityLocationDetailsIndexName"
        val bulkIndexBody = locations.joinToString("\n") {
            val docId = UUID.randomUUID().toString()
            it.id = docId
            val header = "{ \"index\":{ \"_index\": \"$index\",  \"_type\" : \"location\" }, \"_id\" : \"$docId\" }\n"
            val body = jackObjectMapper.writeValueAsString(it)
            header + body
        } + "\n"

        println(bulkIndexBody)
        webClient.post()
                .uri("_bulk")
                .header("Content-Type", "application/x-ndjson")
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(bulkIndexBody))
                .retrieve()
                .bodyToMono(Map::class.java)
                .subscribe {
                    LOG.info(" Ingested to ES $it")
                }
        return true
    }

    override fun getLocationByCategory(category: String, originPoint: String, distanceSortOrder: String): Mono<List<Location>> {
        val index = "$es_prefix$proximityLocationDetailsIndexName/location/_search"
        LOG.info(" In service:  $index")
        val queryTemplate = String(DefaultLocationSearchService::class.java.classLoader
                .getResourceAsStream(GET_MATCH_BY_CATEGORY).readBytes())
        val query = String.format(queryTemplate, originPoint, distanceSortOrder, category)
        return webClient.post()
                .uri(index)
                .header("Content-Type", "application/json")
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(query))
                .retrieve()
                .bodyToMono(Map::class.java)
                .map { doc ->
                    val hits = doc?.get("hits") as Map<String, Any>
                    val records = hits["hits"] as List<Map<String, Any>>
                    records.map {
                        val source = it["_source"] as Map<String, Any>
                        val heatMapResponse = getHeatMap(source["geoLocation"] as String)
                        Location(source["id"] as String?, source["name"] as String?, source["geoLocation"] as String?, source["category"] as String?, source["address"] as String?, source["imageBase64"] as String?, source["phoneNumber"] as String?, source["description"] as List<String>?)
                    }
                }
    }

    private fun getHeatMap(geoLocation: String): Any {
        val uri = "http://localhost:8084/api/v1/heatmap?location=$geoLocation"
         val heatMapresponse = webClient.get()
                .uri(uri)
                .header("Content-Type", "application/json")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(List::class.java)
                 .subscribe{
                     println(it[0])
                 }
        println(heatMapresponse)
        return ""
    }

    override fun searchLocation(category: String, locationName: String, originPoint: String, distanceSortOrder: String): Mono<List<Location>> {
        val index = "$es_prefix$proximityLocationDetailsIndexName/location/_search"
        LOG.info(" In service:  $index")
        val queryTemplate = String(DefaultLocationSearchService::class.java.classLoader
                .getResourceAsStream(SEARCH_LOCATION).readBytes())
        val query = String.format(queryTemplate, originPoint, distanceSortOrder, locationName, category)
        return webClient.post()
                .uri(index)
                .header("Content-Type", "application/json")
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(query))
                .retrieve()
                .bodyToMono(Map::class.java)
                .map { it ->
                    val hits = it?.get("hits") as Map<String, Any>
                    val records = hits["hits"] as List<Map<String, Any>>
                    records.map {
                        val source = it["_source"] as Map<String, Any>
                        Location(source["id"] as String?, source["name"] as String?, source["geoLocation"] as String?, source["category"] as String?, source["address"] as String?, source["imageBase64"] as String?, source["phoneNumber"] as String?, source["description"] as List<String>?)
                    }
                }
    }

    override fun getAllLocationCategories(): List<String> {
        TODO("Not yet implemented")
    }

    override fun getAllLocation(): Mono<List<Location>> {
        val index = "$es_prefix$proximityLocationDetailsIndexName/location/_search"
        LOG.info(" In service:  $index")

        return webClient.get()
                .uri(index)
                .header("Content-Type", "application/json")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Map::class.java)
                .map { doc ->
                    val hits = doc?.get("hits") as Map<String, Any>
                    val records = hits["hits"] as List<Map<String, Any>>
                    records.map {
                        val source = it["_source"] as Map<String, Any>
                        Location(source["id"] as String?, source["name"] as String?, source["geoLocation"] as String?, source["category"] as String?, source["address"] as String?, source["imageBase64"] as String?, source["phoneNumber"] as String?, source["description"] as List<String>?)
                    }
                }
    }

}
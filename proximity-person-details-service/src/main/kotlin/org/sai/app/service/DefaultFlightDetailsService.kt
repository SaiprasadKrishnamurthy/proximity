package org.sai.app.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.sai.app.model.FlightDetails
import org.sai.app.model.ProximityFlightDetailsService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

/**
 * @author Sai.
 */
@Service
class DefaultFlightDetailsService(@Value("\${flightDetailsIndexName}") val flightDetailsIndexName: String,
                                  @Value("\${es.url}") val esUrl: String,
                                  val webClient: WebClient
) : ProximityFlightDetailsService {

    private val jackObjectMapper = jacksonObjectMapper()

    private val GET_FLIGHT_DETAILS = "elastic/proximity_get_flight_details_query.json"

    companion object {
        val LOG = LoggerFactory.getLogger(DefaultFlightDetailsService::class.java)
    }

    override fun flightDetails(pnr: String, lastName: String): Mono<FlightDetails> {

        val queryTemplate = String(DefaultFlightDetailsService::class.java.classLoader
                .getResourceAsStream(GET_FLIGHT_DETAILS).readBytes())
        val flightDetailsQuery = String.format(queryTemplate, pnr, lastName)

        val url = "$esUrl/$flightDetailsIndexName*/_search"
        LOG.info("The Flight Details service query is {}", flightDetailsQuery)
        val queryAsMap = jackObjectMapper.readValue(flightDetailsQuery, Map::class.java)

        return webClient.post()
                .uri(url)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(queryAsMap))
                .retrieve()
                .bodyToMono(Map::class.java)
                .map {
                    val hits = it?.get("hits") as Map<String, Any>
                    val personHits = hits["hits"] as List<Map<String, Any>>
                    personHits.stream()
                            .map { doc -> doc["_source"] as Map<String, Object> }
                            .map { result -> jackObjectMapper.convertValue(result, FlightDetails::class.java) }
                            .findFirst()
                            .get()
                }
    }

}
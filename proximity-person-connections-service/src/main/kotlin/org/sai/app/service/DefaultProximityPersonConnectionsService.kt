package org.sai.app.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.sai.app.model.ConnectedPersons
import org.sai.app.model.ProximalPersons
import org.sai.app.model.ProximityEvent
import org.sai.app.model.ProximityPersonConnectionsService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.kotlin.coroutine.web.awaitFirstOrNull
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors.toList
import java.util.stream.Collectors.toSet

/**
 * @author Sai.
 */
@Service
class DefaultProximityPersonConnectionsService(@Value("\${proximityEventsIndexName}") val proximityEventsIndexName: String,
                                               @Value("\${elastic.query.heatmap.duration}") val heatMapDuration: String,
                                               @Value("\${proximityEventsIndexPartition}") val proximityEventsIndexPartition: String,
                                               val webClient: WebClient) : ProximityPersonConnectionsService {

    companion object {
        val LOG = LoggerFactory.getLogger(DefaultProximityPersonConnectionsService::class.java)
        val OBJECTMAPPER = jacksonObjectMapper()
    }

    private val GET_PERSONS_BY_USER_HASH_TEMPLATE = "elastic/proximity_get_person_by_userHash_query.json"
    private val GET_CONNECTED_PERSONS_IN_SAME_LOCATIONS = "elastic/proximity_get_connected_persons_same_location.json"


    override suspend fun connectedPersons(userIdHash: String): List<ConnectedPersons> {

        val connectedPersonsForInitialUserId = findConnectedPersons(userIdHash);
        val connectedPersons = connectedPersonsForInitialUserId.promixalPersons
                .values
                .stream()
                .flatMap { it?.stream() }
                .map { it.userIdHash }
                .collect(toSet())

        val connectedPersonList = mutableListOf<ConnectedPersons>()
        connectedPersonList.add(0, connectedPersonsForInitialUserId)

        coroutineScope {
            val allConnectedPersons = connectedPersons.stream().map {
                return@map async(Dispatchers.IO) {
                    findConnectedPersons(it)
                }
            }.collect(toList())
            allConnectedPersons.forEach {
                val persons = it.await()
                connectedPersonList.add(persons)
            }
        }
        return connectedPersonList
    }

    suspend fun findConnectedPersons(userIdHash: String): ConnectedPersons {
        val queryTemplate = String(DefaultProximityPersonConnectionsService::class.java.classLoader
                .getResourceAsStream(GET_PERSONS_BY_USER_HASH_TEMPLATE).readBytes())
        val aggregationQuery = String.format(queryTemplate, userIdHash)
        LOG.info("ES Get Person details Query: {}", aggregationQuery)
        val queryAsMap = OBJECTMAPPER.readValue(aggregationQuery, Map::class.java)
        val proximityEvents = webClient.post()
                .uri("$proximityEventsIndexName*/_search")
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(queryAsMap))
                .retrieve()
                .bodyToMono(Map::class.java)
                .map {
                    val hits = it?.get("hits") as Map<String, Any>
                    val personHits = hits.get("hits") as List<Map<String, Any>>
                    personHits.stream()
                            .map { doc -> doc["_source"] as Map<String, Object> }
                            .map { result -> OBJECTMAPPER.convertValue(result, ProximityEvent::class.java) }
                            .collect(toList())
                }
        val proxmitiyEventsForUsers = proximityEvents.awaitFirstOrNull()
        var locationPersonsMap = mutableMapOf<String, MutableList<ProximalPersons>?>()
        proxmitiyEventsForUsers?.forEach {
            locationPersonsMap["${it.canonicalLocationName}_${it.timestamp}"] = findProxmialPersons(it)
        }
        return ConnectedPersons(userIdHash, locationPersonsMap)
    }

    private suspend fun findProxmialPersons(proximityEvent: ProximityEvent): MutableList<ProximalPersons>? {
        val canoicalLocationName = proximityEvent.canonicalLocationName
        val timeSpendAtTheLocation = proximityEvent.timestamp
        val tenMinutesAfter = timeSpendAtTheLocation + (TimeUnit.MINUTES.toMillis(10))
        val tenMinutesBefore = timeSpendAtTheLocation - (TimeUnit.MINUTES.toMillis(10))
        val queryTemplate = String(DefaultProximityPersonConnectionsService::class.java.classLoader
                .getResourceAsStream(GET_CONNECTED_PERSONS_IN_SAME_LOCATIONS).readBytes())

        val queryForConnectedPersons = String.format(queryTemplate, tenMinutesBefore, tenMinutesAfter, canoicalLocationName)

        LOG.info("Get connected persons query: {}", queryForConnectedPersons)
        val queryAsMapForConnectedPersons = OBJECTMAPPER.readValue(queryForConnectedPersons, Map::class.java)

        val proixmalPersonsMono = webClient.post()
                .uri("$proximityEventsIndexName*/_search")
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(queryAsMapForConnectedPersons))
                .retrieve()
                .bodyToMono(Map::class.java)
                .map {
                    val hits = it?.get("hits") as Map<String, Any>
                    val personHits = hits.get("hits") as List<Map<String, Any>>
                    personHits.stream()
                            .map { doc -> doc["_source"] as Map<String, Object> }
                            .map { result -> OBJECTMAPPER.convertValue(result, ProximalPersons::class.java) }
                            .filter { s -> s.userIdHash != proximityEvent.userIdHash }
                            .distinct()
                            .collect(toList())
                }
        return proixmalPersonsMono.awaitFirstOrNull()
    }

}
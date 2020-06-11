package org.sai.app.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.sai.app.model.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors.toList
import java.util.stream.Collectors.toSet

/**
 * @author Sai.
 */
@Service
class DefaultProximityPersonConnectionsService(@Value("\${proximityEventsIndexName}") val proximityEventsIndexName: String,
                                               @Value("\${person.connections.level}") val personConnectionsLevel: Int,
                                               @Value("\${es.url}") var esUrl: String,
                                               val esRestTemplate: RestTemplate,
                                               val esAuthHeaders: HttpHeaders) : ProximityPersonConnectionsService {

    companion object {
        val LOG = LoggerFactory.getLogger(DefaultProximityPersonConnectionsService::class.java)
        val OBJECTMAPPER = jacksonObjectMapper()
    }

    private val GET_PERSONS_BY_USER_HASH_TEMPLATE = "elastic/proximity_get_person_by_userHash_query.json"
    private val GET_CONNECTED_PERSONS_IN_SAME_LOCATIONS = "elastic/proximity_get_connected_persons_same_location.json"


    override fun connectedPersons(userIdHash: String): PersonConnections {

        val proximalPersonsList = mutableListOf<ProximalPersons>()
        val connections = mutableListOf<Connections>()
        val proximalPerson = ProximalPersons(userIdHash)
        proximalPersonsList.add(proximalPerson)
        LOG.info("Initial proximal person list {}", proximalPersonsList)
        findConnectedPersons(userIdHash, proximalPersonsList, connections)
        var connectedPersons = proximalPersonsList
                .stream()
                .filter { it.userIdHash != userIdHash }
                .map { it.userIdHash }
                .collect(toSet())
        var level = 1;
        while (level <= personConnectionsLevel) {
            connectedPersons.forEach {
                findConnectedPersons(it, proximalPersonsList, connections)
            }
            val newConnectedPersons = proximalPersonsList
                    .stream()
                    .map { it.userIdHash }
                    .collect(toSet())
            newConnectedPersons.removeAll(connectedPersons)
            connectedPersons = newConnectedPersons
            level++
        }

        connections.sortWith(compareBy({ it.source }, { it.target }))
        return PersonConnections(proximalPersonsList, connections)
    }

    fun findConnectedPersons(userIdHash: String,
                             proximalPersonsList: MutableList<ProximalPersons>,
                             connections: MutableList<Connections>) {
        val queryTemplate = String(DefaultProximityPersonConnectionsService::class.java.classLoader
                .getResourceAsStream(GET_PERSONS_BY_USER_HASH_TEMPLATE).readBytes())
        val aggregationQuery = String.format(queryTemplate, userIdHash)
        LOG.info("ES Get Person details Query: {}", aggregationQuery)
        val queryAsMap = OBJECTMAPPER.readValue(aggregationQuery, Map::class.java)
        val response = esRestTemplate.exchange("$esUrl/$proximityEventsIndexName*/_search",
                HttpMethod.POST,
                HttpEntity(queryAsMap, esAuthHeaders),
                object : ParameterizedTypeReference<Map<String, Any>>() {})
                .body
        val hits: Map<String, Any> = response?.get("hits") as Map<String, Any>
        val personHits: List<Map<String, Any>> = hits["hits"] as List<Map<String, Any>>
        val proxmitiyEventsForUsers = personHits.stream()
                .map { doc -> doc["_source"] as Map<String, Object> }
                .map { result -> OBJECTMAPPER.convertValue(result, ProximityEvent::class.java) }
                .collect(toList())
        val userIdHashIndex = proximalPersonsList.indexOf(ProximalPersons(userIdHash))
        proxmitiyEventsForUsers.forEach { proximityEvent ->
            val proximalPersonsFromElastic = findProxmialPersons(proximityEvent)
            val locationDetail = LocationDetails(proximityEvent.canonicalLocationName, proximityEvent.timestamp)
            if (proximalPersonsFromElastic != null) {
                for (person in proximalPersonsFromElastic) {
                    if (proximalPersonsList.contains(person)) {
                        val targetElement = proximalPersonsList.find { it.userIdHash == person.userIdHash }
                        val targetElementIndex = proximalPersonsList.indexOf(targetElement)
                        val connectionForId = connections.find { it.source == userIdHashIndex && it.target == targetElementIndex }
                        if (connectionForId != null) {
                            val properties = connectionForId.properties
                            val locationDetails = mutableListOf<LocationDetails>()
                            locationDetails.addAll(properties)
                            locationDetails.add(locationDetail)
                            val connection = Connections(userIdHashIndex, proximalPersonsList.indexOf(person), locationDetails)
                            connections.remove(connectionForId)
                            connections.add(connection)
                        }
                    } else {
                        proximalPersonsList.add(person)
                        val locationDetails = mutableListOf<LocationDetails>()
                        locationDetails.add(locationDetail)
                        val connection = Connections(userIdHashIndex, proximalPersonsList.indexOf(person), locationDetails)
                        connections.add(connection)
                    }
                }
            }
        }
    }

    private fun findProxmialPersons(proximityEvent: ProximityEvent): MutableList<ProximalPersons>? {
        val canonicalLocationName = proximityEvent.canonicalLocationName
        val timeSpendAtTheLocation = proximityEvent.timestamp
        val userId = proximityEvent.userIdHash
        val tenMinutesAfter = timeSpendAtTheLocation + (TimeUnit.MINUTES.toMillis(10))
        val tenMinutesBefore = timeSpendAtTheLocation - (TimeUnit.MINUTES.toMillis(10))
        val queryTemplate = String(DefaultProximityPersonConnectionsService::class.java.classLoader
                .getResourceAsStream(GET_CONNECTED_PERSONS_IN_SAME_LOCATIONS).readBytes())

        val queryForConnectedPersons = String.format(queryTemplate, tenMinutesBefore, tenMinutesAfter, canonicalLocationName, userId)

        LOG.info("ES Get Proximal persons Query: {}", queryForConnectedPersons)

        val queryAsMapForConnectedPersons = OBJECTMAPPER.readValue(queryForConnectedPersons, Map::class.java)

        val response = esRestTemplate.exchange("$esUrl/$proximityEventsIndexName*/_search",
                HttpMethod.POST,
                HttpEntity(queryAsMapForConnectedPersons, esAuthHeaders),
                object : ParameterizedTypeReference<Map<String, Any>>() {})
                .body
        val hits: Map<String, Any> = response?.get("hits") as Map<String, Any>
        val personHits: List<Map<String, Any>> = hits["hits"] as List<Map<String, Any>>
        return personHits.stream()
                .map { doc -> doc["_source"] as Map<String, Object> }
                .map { result -> OBJECTMAPPER.convertValue(result, ProximalPersons::class.java) }
                .distinct()
                .collect(toList())
    }

}
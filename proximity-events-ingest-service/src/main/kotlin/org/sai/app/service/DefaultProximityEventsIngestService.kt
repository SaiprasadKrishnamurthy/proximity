package org.sai.app.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.sai.app.model.ProximityEventsIngestRequest
import org.sai.app.model.ProximityEventsIngestService
import org.sai.app.model.RealtimeCountCriteria
import org.sai.app.model.RealtimeCountRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * @author Sai.
 */
@Service
class DefaultProximityEventsIngestService(val realtimeCountRepository: RealtimeCountRepository,
                                          @Value("\${proximityEventsIndexName}") val proximityEventsIndexName: String,
                                          @Value("\${proximityEventsIndexPartition}") val proximityEventsIndexPartition: String,
                                          val webClient: WebClient
) : ProximityEventsIngestService {

    private val jackObjectMapper = jacksonObjectMapper()

    companion object {
        val LOG = LoggerFactory.getLogger(DefaultProximityEventsIngestService::class.java)
    }

    override suspend fun ingest(proximityEventsIngestRequest: ProximityEventsIngestRequest): Map<String, String> {
        val dateTimeFormatter = DateTimeFormatter.ofPattern(proximityEventsIndexPartition, Locale.ENGLISH)
        val dateTimeFormatterMinutes = DateTimeFormatter.ofPattern("yyMMddHHmm", Locale.ENGLISH)
        val dateIndexPartition = dateTimeFormatter.format(Instant.ofEpochMilli(System.currentTimeMillis()).atZone(ZoneId.systemDefault()).toLocalDate())
        val index = "$proximityEventsIndexName-$dateIndexPartition"
        LOG.info(" In service ")
        val proximityEvents = proximityEventsIngestRequest.proximityEvents
        realtimeCountRepository.save(proximityEvents)

        val bulkIndexBody = proximityEvents.joinToString("\n") {
            val docId = "${it.userIdHash}${dateTimeFormatterMinutes.format(Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime())}"
            val header = "{ \"index\":{ \"_index\": \"$index\",  \"_type\" : \"_doc\" }, \"_id\" : \"$docId\" }\n"
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

        return mapOf("status" to "SAVED")
    }

    override fun count(realtimeCountCriteria: RealtimeCountCriteria): Flux<Long> = realtimeCountRepository.count(realtimeCountCriteria)

}
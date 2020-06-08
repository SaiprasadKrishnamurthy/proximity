package org.sai.app.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.sai.app.model.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.connection.ReactiveHyperLogLogCommands
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * @author Sai.
 */
@Service
class DefaultProximityHeatMapService(@Value("\${proximityEventsIndexName}") val proximityEventsIndexName: String,
                                     @Value("\${elastic.query.heatmap.duration}") val heatMapDuration: String,
                                     private val redisCommands: ReactiveHyperLogLogCommands,
                                     @Value("\${proximityEventsIndexPartition}") val proximityEventsIndexPartition: String,
                                     val webClient: WebClient
) : ProximityHeatMapService {

    private val jackObjectMapper = jacksonObjectMapper()

    companion object {
        val LOG = LoggerFactory.getLogger(DefaultProximityHeatMapService::class.java)
    }

    private val HEATMAP_COUNT_QUERY_TEMPLATE = "elastic/proximity_heatmap_count_query.json"
    private val OBJECTMAPPER = jacksonObjectMapper()

    override fun heatmap(location: String): Mono<List<HeatMapResponse>> {
        val queryTemplate = String(DefaultProximityHeatMapService::class.java.classLoader.getResourceAsStream(HEATMAP_COUNT_QUERY_TEMPLATE).readBytes())
        val aggregationQuery = String.format(queryTemplate, heatMapDuration, location)
        LOG.info("ES count Query: {}", aggregationQuery)
        val queryAsMap = OBJECTMAPPER.readValue(aggregationQuery, Map::class.java)
        return webClient.post()
                .uri(proximityEventsIndexName + "*/_search")
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(queryAsMap))
                .retrieve()
                .bodyToMono(Map::class.java)
                .map {
                    val aggs = it?.get("aggregations") as Map<String, Any>
                    val heatmap = aggs?.get("heatmap") as Map<String, Any>
                    val buckets = heatmap["buckets"] as List<Map<String, Any>>
                    getRiskCounts(buckets)
                }
    }

    override fun count(realtimeCountCriteria: RealtimeCountCriteria): Flux<CountResponse> {
        val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm", Locale.ENGLISH)
        var dateTimeKey = dateTimeFormatter.format(Instant.ofEpochMilli(realtimeCountCriteria.timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime())
        dateTimeKey="2020-06-04-12-39";
        val dateTimeKey2 = dateTimeFormatter.format(Instant.ofEpochMilli(realtimeCountCriteria.timestamp + 60000).atZone(ZoneId.systemDefault()).toLocalDateTime())
        val locationName = realtimeCountCriteria.canonicalLocationName.replace("\\p{Punct}|\\s", "")

        val criteria = Flux.fromIterable(realtimeCountCriteria.criteria)
        val counts = Flux.fromIterable(realtimeCountCriteria.criteria)
                .flatMap {
                    val inKey = "$dateTimeKey:$locationName:$it:${ProximityType.In}"
                    val inKey2 = "${dateTimeKey2}:$locationName:$it:${ProximityType.In}"
                    println("Keys : $inKey $inKey2")
                    val totalIn = redisCommands.pfCount(ByteBuffer.wrap(inKey.toByteArray(Charset.defaultCharset())))
                    val totalIn2 = redisCommands.pfCount(ByteBuffer.wrap(inKey2.toByteArray(Charset.defaultCharset())))
                    totalIn.concatWith(totalIn2).reduce { a, b -> a + b }
                }
        // Concats 2 Fluxes with order.
        return Flux.zip(criteria, counts)
                .map { CountResponse(range = it.t1, risks = it.t2) }
    }

    fun getRiskCounts(buckets: List<Map<String, Any>>): List<HeatMapResponse> {
        return buckets.map {
            val key = it["key"] as String
            val risk = it["risks"] as Map<String, Any>
            val riskBuckets = risk["buckets"] as List<Map<String, Any>>
            val riskCount = mutableMapOf<String, Long>()
            riskBuckets.forEach { rb ->
                riskCount.put(rb["key"]!!.toString(), rb["doc_count"].toString().toLong()!!)
            }
            HeatMapResponse(key, riskCount.toMap())
        }
    }

}
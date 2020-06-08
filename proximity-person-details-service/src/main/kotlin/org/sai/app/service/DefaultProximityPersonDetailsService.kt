package org.sai.app.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.sai.app.model.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.connection.ReactiveHyperLogLogCommands
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.lang.Exception
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
class DefaultProximityPersonDetailsService(@Value("\${proximityPersonDetailsIndexName}") val proximityPersonDetailsIndexName: String,
                                           @Value("\${es.prefix}") val es_prefix: String,
                                     val webClient: WebClient
) : ProximityPersonDetailsService {

    private val jackObjectMapper = jacksonObjectMapper()

    companion object {
        val LOG = LoggerFactory.getLogger(DefaultProximityPersonDetailsService::class.java)
    }
    override fun save(personDetails: PersonDetails): Boolean {
        if (StringUtils.isEmpty(personDetails.id)) {
                    personDetails.id = UUID.randomUUID().toString()
        }
        val index = "$es_prefix-$proximityPersonDetailsIndexName/$proximityPersonDetailsIndexName/${personDetails.id}"
        LOG.info(" In service:  $index")

        try {
            webClient.post()
                    .uri(index)
                    .header("Content-Type", "application/json")
                    .accept(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(personDetails))
                    .retrieve()
                    .bodyToMono(Map::class.java)
                    .subscribe {
                        LOG.info(" Ingested to ES $it")
                    }
        }catch(e:Exception){
            e.printStackTrace()
        }
        return true;
    }


    override fun get(personId: String): Mono<PersonDetails> {
        val index = "$es_prefix-$proximityPersonDetailsIndexName/$proximityPersonDetailsIndexName/$personId"
        LOG.info(" In service:  $index")

         return webClient.get()
                    .uri(index)
                    .header("Content-Type", "application/json")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(Map::class.java)
                    .map {
                    val source = it?.get("_source") as Map<String, Any>
                    PersonDetails( source.get("id") as String?,source.get("firstName") as String?,source.get("lastName") as String?,source.get("country") as String?,source.get("nationality") as String?,source.get("phoneNumber") as String?)
                }
    }

   override fun findAll(): Mono<List<PersonDetails>> {
        val index = "$es_prefix-$proximityPersonDetailsIndexName/$proximityPersonDetailsIndexName/_search"
        LOG.info(" In service:  $index")

        return webClient.get()
                .uri(index)
                .header("Content-Type", "application/json")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Map::class.java)
                .map {
                    val hits = it?.get("hits") as Map<String, Any>
                    val records = hits["hits"] as List<Map<String, Any>>
                    records.map {
                        val source = it?.get("_source") as Map<String, Any>
                        PersonDetails(source.get("id") as String?, source.get("firstName") as String?, source.get("lastName") as String?, source.get("country") as String?, source.get("nationality") as String?, source.get("phoneNumber") as String?)
                    }
                }
    }



}
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
class DefaultSelfAssessmentService(@Value("\${selfAssessmentQuestionnaireIndexName}") val selfAssessmentQuestionnaireIndexName: String,
                                   @Value("\${es.prefix}") val es_prefix: String,
                                   @Value("\${selfAssessmentIndexName}") val selfAssessmentIndexName: String,
                                   val webClient: WebClient
) : SelfAssessmentService {

    private val jackObjectMapper = jacksonObjectMapper()

    companion object {
        val LOG = LoggerFactory.getLogger(DefaultSelfAssessmentService::class.java)
    }
    override fun save(selfAssessmentQuestion: SelfAssessmentQuestion): Boolean{
        if (StringUtils.isEmpty(selfAssessmentQuestion.id)) {
         selfAssessmentQuestion.id = UUID.randomUUID().toString()
        }
        val index = "$es_prefix-$selfAssessmentQuestionnaireIndexName/$selfAssessmentQuestionnaireIndexName/${selfAssessmentQuestion.id}"
        LOG.info(" In service:  $index")
            webClient.post()
                    .uri(index)
                    .header("Content-Type", "application/json")
                    .accept(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(selfAssessmentQuestion))
                    .retrieve()
                    .bodyToMono(Map::class.java)
                    .subscribe {
                        LOG.info(" Ingested to ES $it")
                    }
        return true;
    }



    override fun findAll(): Mono<List<SelfAssessmentQuestion>> {
        val index = "$es_prefix-$selfAssessmentQuestionnaireIndexName/$selfAssessmentQuestionnaireIndexName/_search"
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
                        SelfAssessmentQuestion(source.get("id") as String,source.get("question") as String, source.get("options") as List<String>, source.get("mandatory") as Boolean)
                    }
                }
    }

    override fun selfAccess(selfAssessment: SelfAssessment): Boolean {
        selfAssessment.id = UUID.randomUUID().toString()
        selfAssessment.timestamp=System.currentTimeMillis()
        val index = "$es_prefix-$selfAssessmentIndexName/$selfAssessmentIndexName/${selfAssessment.id}"

        LOG.info(" In service:  $index")
        webClient.post()
                .uri(index)
                .header("Content-Type", "application/json")
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(selfAssessment))
                .retrieve()
                .bodyToMono(Map::class.java)
                .subscribe {
                    LOG.info(" Ingested to ES $it")
                }
        return true;
    }


}
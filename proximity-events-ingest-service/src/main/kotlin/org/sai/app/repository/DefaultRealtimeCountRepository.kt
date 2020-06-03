package org.sai.app.repository

import org.sai.app.model.ProximityEvent
import org.sai.app.model.ProximityType
import org.sai.app.model.RealtimeCountCriteria
import org.sai.app.model.RealtimeCountRepository
import org.slf4j.LoggerFactory
import org.springframework.data.redis.connection.ReactiveHyperLogLogCommands
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.kotlin.core.publisher.toFlux
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * @author Sai.
 */
@Repository
class DefaultRealtimeCountRepository(private val redisCommands: ReactiveHyperLogLogCommands) : RealtimeCountRepository {
    companion object {
        val LOG = LoggerFactory.getLogger(DefaultRealtimeCountRepository::class.java)
    }

    override suspend fun save(proximityEvents: List<ProximityEvent>) {
        LOG.info(" Saving Realtime counts ")
        val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm", Locale.ENGLISH)
        proximityEvents.forEach { pe ->
            val dateTimeKey = dateTimeFormatter.format(Instant.ofEpochMilli(pe.timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime())
            val locationName = pe.canonicalLocationName.replace("\\p{Punct}|\\s", "")
            val tags = pe.tags.joinToString(",")
            val inOrOut = pe.proximityType.toString()
            val key = "$dateTimeKey:$locationName:$tags:$inOrOut"
            redisCommands.pfAdd(ByteBuffer.wrap(key.toByteArray(Charset.defaultCharset())), ByteBuffer.wrap(pe.userIdHash.toByteArray(Charset.defaultCharset())))
                    .subscribe {
                        LOG.info(" Subscribe Realtime counts $it")
                    }
        }
        LOG.info(" After Saving Realtime counts ")
    }

    override fun count(realtimeCountCriteria: RealtimeCountCriteria): Flux<Long> {
        val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm", Locale.ENGLISH)
        val dateTimeKey = dateTimeFormatter.format(Instant.ofEpochMilli(realtimeCountCriteria.timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime())
        val locationName = realtimeCountCriteria.canonicalLocationName.replace("\\p{Punct}|\\s", "")
        val tags = realtimeCountCriteria.criteria.joinToString(",")
        val inKey = "$dateTimeKey:$locationName:$tags:${ProximityType.In}"
        val outKey = "$dateTimeKey:$locationName:$tags:${ProximityType.Out}"

        val totalIn = redisCommands.pfCount(ByteBuffer.wrap(inKey.toByteArray(Charset.defaultCharset())))
        val totalOut = redisCommands.pfCount(ByteBuffer.wrap(outKey.toByteArray(Charset.defaultCharset())))

        return Flux.merge(totalIn, totalOut)
                .reduce { a, b ->
                    val total = a - b
                    if (total < 0) 0 else total
                }.toFlux()
    }
}
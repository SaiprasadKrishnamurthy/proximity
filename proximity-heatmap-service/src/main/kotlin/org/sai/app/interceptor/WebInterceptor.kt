package com.flopanda.ingest.interceptor

import net.jodah.expiringmap.ExpirationPolicy
import net.jodah.expiringmap.ExpiringMap
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.util.*
import java.util.concurrent.TimeUnit


@Component
class WebInterceptor : WebFilter {

    val URL_INCLUSIONS = listOf("/apis/")

    val NONCE_CACHE: MutableMap<String, String> = ExpiringMap.builder()
            .maxSize(30000)
            .expirationPolicy(ExpirationPolicy.CREATED)
            .expiration(60, TimeUnit.SECONDS)
            .build()

    companion object {
        const val API_KEY = "x-api-key"
    }

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val apiKey = exchange.request.cookies[API_KEY]?.get(0)?.value ?: UUID.randomUUID().toString()
        val shouldCheck = URL_INCLUSIONS.any { exchange.request.uri.path.contains(it) }
        if (shouldCheck) {
            // Check the token. TODO
        }
        return chain.filter(
                exchange.mutate()
                        .request(exchange.request).build())
    }
}
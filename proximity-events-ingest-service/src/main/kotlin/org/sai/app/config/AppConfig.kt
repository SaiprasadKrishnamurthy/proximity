package org.sai.app.config

import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import kotlinx.coroutines.newFixedThreadPoolContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.server.ServerWebExchange
import reactor.netty.http.client.HttpClient
import springfox.documentation.builders.ApiInfoBuilder
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.service.Contact
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger2.annotations.EnableSwagger2WebFlux


/**
 * @author Sai.
 */
@Configuration
@EnableSwagger2WebFlux
class AppConfig {

    @Bean("PROXIMITY_EVENTS_INGEST")
    fun proximityEventsIngest() = newFixedThreadPoolContext(Runtime.getRuntime().availableProcessors() * 2, "PROXIMITY_EVENTS_INGEST")

    @Bean("PROXIMITY_EVENTS_COUNTER_INGEST")
    fun proximityEventsCounterIngest() = newFixedThreadPoolContext(Runtime.getRuntime().availableProcessors() * 2, "PROXIMITY_EVENTS_COUNTER_INGEST")

    @ConditionalOnProperty(value = ["elasticsearch.self.signed.certificate"], havingValue = "true", matchIfMissing = true)
    @Bean
    fun webclient(@Value("\${es.username}") esUserName: String,
                  @Value("\${es.password}") esPassword: String,
                  @Value("\${es.url}") esUrl: String): WebClient {
        val sslContext = SslContextBuilder
                .forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build()
        val httpClient = HttpClient.create().secure { t -> t.sslContext(sslContext) }
        val httpConnector = ReactorClientHttpConnector(httpClient)
        return WebClient.builder()
                .clientConnector(httpConnector)
                .defaultHeaders { header -> header.setBasicAuth(esUserName, esPassword) }
                .baseUrl(esUrl)
                .build()
    }

    @Bean
    fun api(): Docket {
        return Docket(DocumentationType.SWAGGER_2)
                .apiInfo(ApiInfoBuilder().description("Proximity Events Ingest Service").contact(Contact("Sai", "", "saikris@gmail.com")).build())
                .ignoredParameterTypes(ServerWebExchange::class.java)
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.any())
                .build()
    }
}
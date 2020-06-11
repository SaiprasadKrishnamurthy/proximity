package org.sai.app.config

import com.google.common.base.Predicates
import org.apache.commons.codec.binary.Base64
import org.apache.http.HttpHost
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.conn.ssl.TrustSelfSignedStrategy
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.client.HttpClients
import org.apache.http.ssl.SSLContextBuilder
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.web.client.RestTemplate
import springfox.documentation.builders.ApiInfoBuilder
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.service.ApiInfo
import springfox.documentation.service.Contact
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.security.KeyStore

/**
 * @author Sai.
 */
@Configuration
class AppConfig {

    @Bean("esRestTemplate")
    fun esRestTemplate(@Value("\${es.username}") esUserName: String,
                       @Value("\${es.password}") esPassword: String,
                       @Value("\${es.url}") esUrl: String,
                       @Value("\${esJksFile}") esJksFile: String,
                       @Value("\${esJksPassword}") esJksPassword: String,
                       @Value("\${useProxy}") useProxy: Boolean): RestTemplate {
        return try {
            if (!esUrl.contains("https")) {
                val stringHttpMessageConverter = StringHttpMessageConverter()
                stringHttpMessageConverter.defaultCharset = Charset.defaultCharset()
                val restTemplate = RestTemplate()
                restTemplate.messageConverters.add(stringHttpMessageConverter)
                return restTemplate
            }
            var keystoreFile: InputStream? = null
            try {
                val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
                if (File(esJksFile).exists()) {
                    keystoreFile = FileInputStream(esJksFile)
                } else {
                    keystoreFile = this::class.java.getResourceAsStream(esJksFile)
                }
                keyStore.load(keystoreFile, esJksPassword.toCharArray())
                val socketFactory = SSLConnectionSocketFactory(
                        SSLContextBuilder()
                                .loadTrustMaterial(null, TrustSelfSignedStrategy())
                                .loadKeyMaterial(keyStore, null) // Can be null as the key is already loaded using the password.
                                .build(),
                        NoopHostnameVerifier.INSTANCE)
                val httpClientBuilder: HttpClientBuilder = HttpClients
                        .custom()
                if (useProxy) {
                    // ok - hardcoded!
                    httpClientBuilder.setProxy(HttpHost("192.168.46.100", 3128, "http"))
                }
                httpClientBuilder.setSSLSocketFactory(socketFactory).build()
                val requestFactory: ClientHttpRequestFactory = HttpComponentsClientHttpRequestFactory(httpClientBuilder.build())
                RestTemplate(requestFactory)
            } finally {
                if (keystoreFile != null) {
                    try {
                        keystoreFile.close()
                    } catch (ignore: IOException) {
                        // ignored.
                    }
                }
            }
        } catch (ex: Exception) {
            throw RuntimeException(ex)
        }
    }

    /**
     * Necessary HTTP headers for the Datalake.
     *
     * @return HttpHeaders.
     */
    @Qualifier("esAuthHeaders")
    @Bean
    fun esAuthHeaders(@Value("\${es.username}") esUserName: String,
                      @Value("\${es.password}") esPassword: String): HttpHeaders? {
        return object : HttpHeaders() {
            init {
                val auth = "$esUserName:$esPassword"
                val encodedAuth = Base64.encodeBase64(auth.toByteArray(Charset.defaultCharset()))
                val authHeader = "Basic " + String(encodedAuth)
                set("Authorization", authHeader)
                contentType = MediaType.APPLICATION_JSON
            }
        }
    }

    @Bean
    fun configApi(): Docket {
        return Docket(DocumentationType.SWAGGER_2)
                .groupName("config")
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(Predicates.not(PathSelectors.regex("/error"))) // Exclude Spring error controllers
                .build()
    }

    private fun apiInfo(): ApiInfo {
        return ApiInfoBuilder()
                .title("Proximity person connection service")
                .contact(Contact("SITA GSL Innovation Team", "www.sita.aero", "saiprasad.krishnamurthy@sita.aero,pankaj.jain2@sita.aero"))
                .version("Build: " + "1.0")
                .build()
    }
}
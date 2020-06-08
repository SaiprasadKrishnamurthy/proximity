package org.sai.app.model

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

    data class PersonDetails(var id: String?,val firstName: String?,val lastName: String?,val country: String?,val nationality: String?,val phoneNumber: String?)

interface ProximityPersonDetailsService {
    fun save(personDetails: PersonDetails): Boolean
    fun get(personId: String): Mono<PersonDetails>
    fun findAll(): Mono<List<PersonDetails>>
}



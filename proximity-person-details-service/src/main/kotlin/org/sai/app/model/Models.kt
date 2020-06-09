package org.sai.app.model

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.sql.Timestamp

data class PersonDetails(var id: String?,val firstName: String?,val lastName: String?,val country: String?,val nationality: String?,val phoneNumber: String?)

    interface ProximityPersonDetailsService {
        fun save(personDetails: PersonDetails): Boolean
        fun get(personId: String): Mono<PersonDetails>
        fun findAll(): Mono<List<PersonDetails>>
    }

    data class SelfAssessmentAnswer(val questionId:String,val answer:String)
    data class SelfAssessmentQuestion(var id: String?,val question: String,val options: List<String>,val mandatory: Boolean=true)
    data class SelfAssessment(var id: String?,val personId: String,var timestamp: Long?, val answers:List<SelfAssessmentAnswer>)

    interface SelfAssessmentService {
        fun save(selfAssessmentQuestion: SelfAssessmentQuestion): Boolean
        fun findAll(): Mono<List<SelfAssessmentQuestion>>
        fun selfAccess(selfAssessment: SelfAssessment): Boolean
    }





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

    data class FlightDetails(val departureTime:String="19:25",
                             val arrivalTime:String="08:20",
                             val travelTime:String="8hrs 25 mins",
                             val aircraft:String="British Airways",
                             val carrier:String="Boeing 777-200",
                             val passengers:String="Kumar Thangavel",
                             val seat:String="16A",
                             val flightNo: String="BA 257",
                             val gate:String="B32",
                             val pnr:String="AAA123"
    )

    interface SelfAssessmentService {
        fun save(selfAssessmentQuestion: SelfAssessmentQuestion): Boolean
        fun findAll(): Mono<List<SelfAssessmentQuestion>>
        fun selfAccess(selfAssessment: SelfAssessment): Boolean
    }

interface ProximityFlightDetailsService{
    fun flightDetails(pnr: String, lastName: String):Mono<FlightDetails>
}





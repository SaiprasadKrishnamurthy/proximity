package org.sai.app.controller

import com.flopanda.ingest.interceptor.WebInterceptor
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.sai.app.model.*
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import java.time.Duration


/**
 * Ingests the events into the database.
 * @author Sai.
 */
@RestController
@RequestMapping("/api/v1")
@CrossOrigin
class SelfAssessmentController(private val selfAssessmentService: SelfAssessmentService) {

    @PostMapping("/self-assessment" , consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun selfAssessment(@RequestBody selfAssessment: SelfAssessment)=
            selfAssessmentService.selfAccess(selfAssessment)

    @PostMapping("/self-assessment-questions" , consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun save(@RequestBody selfAssessmentQuestion: SelfAssessmentQuestion)=
            selfAssessmentService.save(selfAssessmentQuestion)

    @GetMapping("/self-assessment-questions", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun findAll() = selfAssessmentService.findAll()
}
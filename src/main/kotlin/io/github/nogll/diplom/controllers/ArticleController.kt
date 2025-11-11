package io.github.nogll.diplom.controllers

import io.github.nogll.diplom.dto.*
import io.github.nogll.diplom.service.ArticleProcessingService
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/api/v1")
class ArticleController(
    private val articleProcessingService: ArticleProcessingService
) {
    
    @PostMapping("/articles/process")
    fun processArticle(@RequestBody request: ProcessArticleRequest): ProcessArticleResponse {
        val interactions = articleProcessingService.processArticle(request)
        return ProcessArticleResponse(interactions.size);
    }
    
    @GetMapping("/articles")
    fun getArticles(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ArticleResponse {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"))
        val articlesPage = articleProcessingService.getArticles(pageable)
        
        val articlesDto = articlesPage.content.map { article ->
            ArticleDto(
                id = article.id,
                url = article.url,
                title = article.title,
                abstract = article.abstract
            )
        }
        
        val articlesDtoPage = PageImpl(articlesDto, pageable, articlesPage.totalElements)
        return ArticleResponse.fromPage(articlesDtoPage)
    }
    
    @GetMapping("/interactions")
    fun getInteractions(
        @RequestParam(required = false) plantName: String?,
        @RequestParam(required = false) compoundName: String?,
        @RequestParam(required = false) effect: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): InteractionResponse {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"))
        val interactionsPage = articleProcessingService.getInteractions(plantName, compoundName, effect, pageable)
        
        val interactionsDto = interactionsPage.content.map { interaction ->
            InteractionDto(
                id = interaction.id,
                plant = interaction.plant.name,
                compound = interaction.compound.name ?: "",
                effects = interaction.getEffectsList(),
                plantParts = interaction.getPlantPartsList(),
                model = interaction.source.model.name,
                articleTitle = interaction.source.article.title
            )
        }
        
        val interactionsDtoPage = PageImpl(interactionsDto, pageable, interactionsPage.totalElements)
        return InteractionResponse.fromPage(interactionsDtoPage)
    }
    
    @GetMapping("/articles/{articleId}/sources")
    fun getArticleSources(@PathVariable articleId: Long): List<SourceDto> {
        val sources = articleProcessingService.getSourcesByArticleId(articleId)
        return sources.map { source ->
            SourceDto(
                id = source.id,
                modelName = source.model.name,
                rawResponse = source.rawResponse
            )
        }
    }
    
    @GetMapping("/sources/{sourceId}")
    @ResponseStatus(HttpStatus.OK)
    fun getSourceById(@PathVariable sourceId: Long): SourceDto {
        val source = articleProcessingService.getSourceById(sourceId) 
            ?: throw NotFoundException("Source with id $sourceId not found")
        return SourceDto(
            id = source.id,
            modelName = source.model.name,
            rawResponse = source.rawResponse
        )
    }
    
    @GetMapping("/interactions/csv")
    fun downloadInteractionsCsv(
        @RequestParam(required = false) plantName: String?,
        @RequestParam(required = false) compoundName: String?,
        @RequestParam(required = false) effect: String?
    ): ResponseEntity<String> {
        val csvContent = articleProcessingService.generateCsv(plantName, compoundName, effect)
        
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val filename = "interactions_$timestamp.csv"
        
        val headers = HttpHeaders()
        headers.contentType = MediaType("text", "csv")
        headers.setContentDispositionFormData("attachment", filename)
        
        return ResponseEntity.ok()
            .headers(headers)
            .body(csvContent)
    }

    @ExceptionHandler(NotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(e: NotFoundException): ErrorResponse {
        return ErrorResponse(e.message ?: "Resource not found")
    }
    
    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun exception(e: Exception): ErrorResponse {
        return ErrorResponse(e.message)
    }
}


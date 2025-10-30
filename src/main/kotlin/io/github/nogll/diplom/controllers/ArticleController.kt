package io.github.nogll.diplom.controllers

import io.github.nogll.diplom.dto.*
import io.github.nogll.diplom.entity.Interaction
import io.github.nogll.diplom.entity.Source
import io.github.nogll.diplom.service.ArticleProcessingService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
class ArticleController(
    private val articleProcessingService: ArticleProcessingService
) {
    
    @PostMapping("/articles/process")
    fun processArticle(@RequestBody request: ProcessArticleRequest): ResponseEntity<Map<String, Any>> {
        try {
            val interactions = articleProcessingService.processArticle(request)
            return ResponseEntity.ok(mapOf(
                "success" to true,
                "message" to "Article processed successfully",
                "interactionsCount" to interactions.size
            ))
        } catch (e: Exception) {
            return ResponseEntity.status(500).body(mapOf(
                "success" to false,
                "message" to "Error processing article: ${e.message}"
            ))
        }
    }
    
    @GetMapping("/articles")
    fun getArticles(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<Map<String, Any>> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"))
        val articlesPage = articleProcessingService.getArticles(pageable)
        
        val articles = articlesPage.content.map { article ->
            ArticleDto(
                id = article.id,
                url = article.url,
                title = article.title,
                abstract = article.abstract
            )
        }
        
        return ResponseEntity.ok(mapOf(
            "content" to articles,
            "totalElements" to articlesPage.totalElements,
            "totalPages" to articlesPage.totalPages,
            "currentPage" to articlesPage.number,
            "size" to articlesPage.size
        ))
    }
    
    @GetMapping("/interactions")
    fun getInteractions(
        @RequestParam(required = false) plantName: String?,
        @RequestParam(required = false) compoundName: String?,
        @RequestParam(required = false) effect: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<Map<String, Any>> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"))
        val interactionsPage = articleProcessingService.getInteractions(plantName, compoundName, effect, pageable)
        
        val interactions = interactionsPage.content.map { interaction ->
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
        
        return ResponseEntity.ok(mapOf(
            "content" to interactions,
            "totalElements" to interactionsPage.totalElements,
            "totalPages" to interactionsPage.totalPages,
            "currentPage" to interactionsPage.number,
            "size" to interactionsPage.size
        ))
    }
    
    @GetMapping("/articles/{articleId}/sources")
    fun getArticleSources(@PathVariable articleId: Long): ResponseEntity<List<SourceDto>> {
        val sources = articleProcessingService.getSourcesByArticleId(articleId)
        val sourceDtos = sources.map { source ->
            SourceDto(
                id = source.id,
                modelName = source.model.name,
                rawResponse = source.rawResponse
            )
        }
        return ResponseEntity.ok(sourceDtos)
    }
    
    @GetMapping("/sources/{sourceId}")
    fun getSourceById(@PathVariable sourceId: Long): ResponseEntity<SourceDto?> {
        val source = articleProcessingService.getSourceById(sourceId)
        if (source == null) {
            return ResponseEntity.notFound().build()
        }
        val sourceDto = SourceDto(
            id = source.id,
            modelName = source.model.name,
            rawResponse = source.rawResponse
        )
        return ResponseEntity.ok(sourceDto)
    }
}


package io.github.nogll.diplom.controllers

import io.github.nogll.diplom.dto.*
import io.github.nogll.diplom.service.ArticleProcessingService
import io.github.nogll.diplom.service.PubMedService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/pubmed")
class PubMedController(
    private val pubmedService: PubMedService,
    private val articleProcessingService: ArticleProcessingService
) {
    
    @GetMapping("/search")
    fun search(
        @RequestParam query: String,
        @RequestParam(defaultValue = "1") page: Int
    ): PubMedSearchResponse {
        val articles = pubmedService.searchArticles(query, page)
        val totalPages = pubmedService.getTotalPages(query)
        
        return PubMedSearchResponse(
            articles = articles,
            totalPages = totalPages,
            currentPage = page,
            hasMore = page < totalPages
        )
    }
    
    @GetMapping("/article/abstract")
    fun getArticleAbstract(@RequestParam url: String): PubMedArticleDto {
        val abstract = pubmedService.getArticleAbstract(url)
        return PubMedArticleDto(
            title = "", // Title not needed for abstract-only response
            url = url,
            abstract = abstract
        )
    }
    
    @PostMapping("/article/process")
    fun processPubMedArticle(
        @RequestParam url: String,
        @RequestParam title: String,
        @RequestParam abstract: String
    ): ProcessArticleResponse {
        val request = ProcessArticleRequest(
            url = url,
            title = title,
            abstract = abstract
        )
        val interactions = articleProcessingService.processArticle(request)
        return ProcessArticleResponse(interactions.size)
    }
    
    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun exception(e: Exception): ErrorResponse {
        return ErrorResponse(e.message)
    }
}


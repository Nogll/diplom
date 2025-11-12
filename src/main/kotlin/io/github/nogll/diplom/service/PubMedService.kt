package io.github.nogll.diplom.service

import io.github.nogll.diplom.dto.PubMedArticleDto
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Service
class PubMedService {
    private val client = RestClient.create("https://pubmed.ncbi.nlm.nih.gov")
    
    fun searchArticles(query: String, page: Int = 1): List<PubMedArticleDto> {
        val html = client.get()
            .uri("/?term={query}&page={page}", query, page)
            .retrieve()
            .body(String::class.java) ?: return emptyList()
        
        val doc = Jsoup.parse(html)
        val articles = mutableListOf<PubMedArticleDto>()
        
        // PubMed search results - try multiple selectors
        // Modern PubMed uses article.docsum with a.docsum-title
        val articleElements = doc.select("article.docsum")
        
        if (articleElements.isNotEmpty()) {
            articleElements.forEach { element ->
                val titleLink = element.select("a.docsum-title").first()
                if (titleLink != null) {
                    val title = titleLink.text().trim()
                    var href = titleLink.attr("href")
                    if (href.isNotEmpty() && title.isNotEmpty()) {
                        // Normalize URL
                        if (href.startsWith("/")) {
                            href = "https://pubmed.ncbi.nlm.nih.gov$href"
                        } else if (!href.startsWith("http")) {
                            href = "https://pubmed.ncbi.nlm.nih.gov/$href"
                        }
                        articles.add(PubMedArticleDto(
                            title = title,
                            url = href
                        ))
                    }
                }
            }
        } else {
            // Fallback: look for links with PubMed article pattern (numeric IDs)
            doc.select("a[href*='/']").forEach { link ->
                val href = link.attr("href")
                val text = link.text().trim()
                
                // Match PubMed article URLs: /12345678/ or /12345678
                if (href.matches(Regex(".*/\\d+/?$")) && text.length > 10) {
                    val url = if (href.startsWith("http")) {
                        href
                    } else if (href.startsWith("/")) {
                        "https://pubmed.ncbi.nlm.nih.gov$href"
                    } else {
                        "https://pubmed.ncbi.nlm.nih.gov/$href"
                    }
                    
                    // Avoid duplicates and ensure it's a valid article link
                    if (!articles.any { it.url == url }) {
                        articles.add(PubMedArticleDto(
                            title = text,
                            url = url
                        ))
                    }
                }
            }
        }
        
        return articles.distinctBy { it.url }
    }
    
    fun getArticleAbstract(articleUrl: String): String? {
        try {
            // Extract path from full URL
            val path = if (articleUrl.startsWith("http")) {
                articleUrl.replace("https://pubmed.ncbi.nlm.nih.gov", "")
            } else {
                articleUrl
            }
            
            val html = client.get()
                .uri(path)
                .retrieve()
                .body(String::class.java) ?: return null
            
            val doc = Jsoup.parse(html)
            
            // Try multiple selectors for abstract - PubMed uses various structures
            val selectors = listOf(
                "#abstract", 
                ".abstract", 
                ".abstract-content",
                "div.abstract-text",
                "section#abstract",
                "[data-abstract]",
                ".abstract-text-content",
                "div[class*='abstract']"
            )
            
            for (selector in selectors) {
                val abstractElement = doc.select(selector).first()
                if (abstractElement != null) {
                    var abstractText = abstractElement.text()
                    // Remove common labels
                    abstractText = abstractText.replace(
                        Regex("^(Abstract|Background|Objective|Methods|Results|Conclusion|Introduction):\\s*", 
                            RegexOption.IGNORE_CASE), 
                        ""
                    )
                    abstractText = abstractText.trim()
                    if (abstractText.length > 50) { // Ensure we have substantial content
                        return abstractText
                    }
                }
            }
            
            // Fallback: try finding by text pattern
            val allText = doc.body().text()
            val abstractMatch = Regex(
                "(?i)(?:abstract|background)[\\s:]+(.+?)(?:\\n\\n|introduction|methods|results|conclusion|\\Z)", 
                RegexOption.DOT_MATCHES_ALL
            ).find(allText)
            
            return abstractMatch?.groupValues?.get(1)?.trim()?.takeIf { it.length > 50 }
        } catch (e: Exception) {
            return null
        }
    }
    
    fun getTotalPages(query: String): Int {
        val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8)
        val html = client.get()
            .uri("/?term={query}&page=1", encodedQuery)
            .retrieve()
            .body(String::class.java) ?: return 1
        
        val doc = Jsoup.parse(html)
        
        // Look for pagination info
        val paginationElements = doc.select(".pagination, .page-numbers, [class*='page']")
        var maxPage = 1
        
        paginationElements.forEach { element ->
            val pageLinks = element.select("a, button")
            pageLinks.forEach { link ->
                val pageText = link.text().trim()
                val pageNum = pageText.toIntOrNull()
                if (pageNum != null && pageNum > maxPage) {
                    maxPage = pageNum
                }
            }
        }
        
        // Also check for "of X pages" text
        val pageInfoText = doc.body().text()
        val pageInfoMatch = Regex("(?:page|of)\\s+(\\d+)", RegexOption.IGNORE_CASE).find(pageInfoText)
        pageInfoMatch?.let {
            val totalPages = it.groupValues[1].toIntOrNull()
            if (totalPages != null && totalPages > maxPage) {
                maxPage = totalPages
            }
        }
        
        return maxPage.coerceAtLeast(1)
    }
}


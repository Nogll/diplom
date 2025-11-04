package io.github.nogll.diplom.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.nogll.diplom.dto.ProcessArticleRequest
import io.github.nogll.diplom.entity.*
import io.github.nogll.diplom.llm.GeminiService
import io.github.nogll.diplom.repository.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ArticleProcessingService(
    private val geminiService: GeminiService,
    private val articleRepository: ArticleRepository,
    private val plantRepository: PlantRepository,
    private val compoundRepository: CompoundRepository,
    private val modelRepository: ModelRepository,
    private val sourceRepository: SourceRepository,
    private val interactionRepository: InteractionRepository
) {
    private val objectMapper = jacksonObjectMapper()

    @Transactional
    fun processArticle(request: ProcessArticleRequest): List<Interaction> {
        // Generate extraction using Gemini
        val geminiResponse = geminiService.generate(request.abstract)
        
        // Parse the response
        val extractedData: List<ExtractedInteraction> = objectMapper.readValue(geminiResponse)
        
        // Get or create model
        val model = modelRepository.findById(1).orElseGet {
            modelRepository.save(Model(name = "gemini-2.5-flash", description = "Google Gemini 2.5 Flash"))
        }
        
        // Save article
        val article = articleRepository.save(
            Article(url = request.url, title = request.title, abstract = request.abstract)
        )
        
        // Save source with raw response
        val source = Source().apply {
            this.article = article
            this.model = model
            this.rawResponse = geminiResponse
        }
        val savedSource = sourceRepository.save(source)
        
        // Process extracted interactions
        val savedInteractions = mutableListOf<Interaction>()
        for (data in extractedData) {
            // Get or create plant
            val plant = plantRepository.findByName(data.plant) ?: plantRepository.save(Plant(name = data.plant))
            
            // Get or create compound
            val compound = compoundRepository.findByName(data.compound) 
                ?: compoundRepository.save(Compound(name = data.compound))
            
            // Create interaction
            val interaction = Interaction().apply {
                this.plant = plant
                this.compound = compound
                setEffectsList(data.effects)
                setPlantPartsList(data.part)
                this.source = savedSource
            }
            
            savedInteractions.add(interactionRepository.save(interaction))
        }
        
        return savedInteractions
    }

    @Transactional(readOnly = true)
    fun getArticles(pageable: Pageable): Page<Article> {
        return articleRepository.findAllByOrderByIdDesc(pageable)
    }

    @Transactional(readOnly = true)
    fun getInteractions(plantName: String?, compoundName: String?, effect: String?, pageable: Pageable): Page<Interaction> {
        return if (plantName.isNullOrBlank() && compoundName.isNullOrBlank() && effect.isNullOrBlank()) {
            interactionRepository.findAllWithRelations(pageable)
        } else {
            interactionRepository.findByFilters(
                plantName = if (plantName.isNullOrBlank()) null else plantName,
                compoundName = if (compoundName.isNullOrBlank()) null else compoundName,
                effect = if (effect.isNullOrBlank()) null else effect,
                pageable = pageable
            )
        }
    }

    @Transactional(readOnly = true)
    fun getSourcesByArticleId(articleId: Long): List<Source> {
        return sourceRepository.findByArticleId(articleId)
    }
    
    @Transactional(readOnly = true)
    fun getSourceById(sourceId: Long): Source? {
        return sourceRepository.findByIdWithRelations(sourceId)
    }

    private data class ExtractedInteraction(
        val plant: String,
        val compound: String,
        val effects: List<String>,
        val part: List<String>? = null
    )
}


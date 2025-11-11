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
    
    @Transactional(readOnly = true)
    fun getAllInteractionsForCsv(plantName: String?, compoundName: String?, effect: String?): List<Interaction> {
        return if (plantName.isNullOrBlank() && compoundName.isNullOrBlank() && effect.isNullOrBlank()) {
            interactionRepository.findAllWithRelations()
        } else {
            interactionRepository.findAllWithFilters(
                plantName = if (plantName.isNullOrBlank()) null else plantName,
                compoundName = if (compoundName.isNullOrBlank()) null else compoundName,
                effect = if (effect.isNullOrBlank()) null else effect
            )
        }
    }
    
    fun generateCsv(plantName: String?, compoundName: String?, effect: String?): String {
        val interactions = getAllInteractionsForCsv(plantName, compoundName, effect)
        
        val csvRows = mutableListOf<String>()
        
        // CSV Header
        csvRows.add("row,plant,compound,effect,article,model")
        
        // CSV Data - one row per effect
        var rowNumber = 1
        interactions.forEach { interaction ->
            val plant = interaction.plant.name
            val compound = interaction.compound.name ?: ""
            val articleUrl = interaction.source.article.url
            val modelName = interaction.source.model.name
            val effects = interaction.getEffectsList()
            
            if (effects.isEmpty()) {
                // If no effects, still create one row with empty effect
                csvRows.add(escapeCsvValue(rowNumber.toString()) + "," +
                           escapeCsvValue(plant) + "," +
                           escapeCsvValue(compound) + "," +
                           escapeCsvValue("") + "," +
                           escapeCsvValue(articleUrl) + "," +
                           escapeCsvValue(modelName))
                rowNumber++
            } else {
                // Create one row per effect
                effects.forEach { effectValue ->
                    csvRows.add(escapeCsvValue(rowNumber.toString()) + "," +
                               escapeCsvValue(plant) + "," +
                               escapeCsvValue(compound) + "," +
                               escapeCsvValue(effectValue) + "," +
                               escapeCsvValue(articleUrl) + "," +
                               escapeCsvValue(modelName))
                    rowNumber++
                }
            }
        }
        
        return csvRows.joinToString("\n")
    }
    
    private fun escapeCsvValue(value: String): String {
        // If value contains comma, newline, or quote, wrap in quotes and escape quotes
        if (value.contains(",") || value.contains("\n") || value.contains("\"") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\""
        }
        return value
    }

    private data class ExtractedInteraction(
        val plant: String,
        val compound: String,
        val effects: List<String>,
        val part: List<String>? = null
    )
}


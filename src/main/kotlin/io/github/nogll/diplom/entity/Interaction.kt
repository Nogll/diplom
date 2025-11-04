package io.github.nogll.diplom.entity

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.*

@Entity
@Table(name = "interactions")
class Interaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plant_id", nullable = false)
    lateinit var plant: Plant
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compound_id", nullable = false)
    lateinit var compound: Compound
    
    @Column(nullable = false, columnDefinition = "TEXT")
    var effects: String = "" // Stored as JSON array string
    
    @Column(columnDefinition = "TEXT")
    var plantParts: String? = null // Stored as JSON array string
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    lateinit var source: Source
    
    fun getEffectsList(): List<String> = fromJsonString(effects)
    
    fun setEffectsList(effects: List<String>) {
        this.effects = toJsonString(effects)
    }
    
    fun getPlantPartsList(): List<String>? = plantParts?.let { fromJsonString(it) }
    
    fun setPlantPartsList(parts: List<String>?) {
        this.plantParts = parts?.let { toJsonString(it) }
    }
    
    companion object {
        private val objectMapper = ObjectMapper()
        
        private fun toJsonString(list: List<String>): String {
            return objectMapper.writeValueAsString(list)
        }
        
        private fun fromJsonString(json: String): List<String> {
            return objectMapper.readValue(json, Array<String>::class.java).toList()
        }
    }
}


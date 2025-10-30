package io.github.nogll.diplom.repository

import io.github.nogll.diplom.entity.Interaction
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface InteractionRepository : JpaRepository<Interaction, Long> {
    @Query("SELECT DISTINCT i FROM Interaction i " +
            "LEFT JOIN FETCH i.plant " +
            "LEFT JOIN FETCH i.compound " +
            "LEFT JOIN FETCH i.source s " +
            "LEFT JOIN FETCH s.model " +
            "LEFT JOIN FETCH s.article " +
            "WHERE (:plantName IS NULL OR LOWER(i.plant.name) LIKE LOWER(CONCAT('%', :plantName, '%'))) " +
            "AND (:compoundName IS NULL OR LOWER(i.compound.name) LIKE LOWER(CONCAT('%', :compoundName, '%'))) " +
            "AND (:effect IS NULL OR LOWER(i.effects) LIKE LOWER(CONCAT('%', :effect, '%')))")
    fun findByFilters(
        @Param("plantName") plantName: String?,
        @Param("compoundName") compoundName: String?,
        @Param("effect") effect: String?,
        pageable: Pageable
    ): Page<Interaction>
    
    @Query("SELECT DISTINCT i FROM Interaction i " +
            "LEFT JOIN FETCH i.plant " +
            "LEFT JOIN FETCH i.compound " +
            "LEFT JOIN FETCH i.source s " +
            "LEFT JOIN FETCH s.model " +
            "LEFT JOIN FETCH s.article")
    fun findAllWithRelations(pageable: Pageable): Page<Interaction>
}


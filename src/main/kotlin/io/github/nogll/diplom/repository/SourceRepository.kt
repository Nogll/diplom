package io.github.nogll.diplom.repository

import io.github.nogll.diplom.entity.Source
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface SourceRepository : JpaRepository<Source, Long> {
    @Query("SELECT s FROM Source s " +
            "LEFT JOIN FETCH s.model " +
            "WHERE s.article.id = :articleId")
    fun findByArticleId(@Param("articleId") articleId: Long): List<Source>
    
    @Query("SELECT s FROM Source s " +
            "LEFT JOIN FETCH s.article " +
            "LEFT JOIN FETCH s.model " +
            "WHERE s.id = :id")
    fun findByIdWithRelations(@Param("id") id: Long): Source?
}


package io.github.nogll.diplom.repository

import io.github.nogll.diplom.entity.Article
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ArticleRepository : JpaRepository<Article, Long> {
    fun findAllByOrderByIdDesc(pageable: Pageable): Page<Article>
}


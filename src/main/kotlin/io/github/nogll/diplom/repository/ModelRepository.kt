package io.github.nogll.diplom.repository

import io.github.nogll.diplom.entity.Model
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ModelRepository : JpaRepository<Model, Long>


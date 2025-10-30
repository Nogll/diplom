package io.github.nogll.diplom.repository

import io.github.nogll.diplom.entity.Plant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PlantRepository : JpaRepository<Plant, Long> {
    fun findByName(name: String): Plant?
}


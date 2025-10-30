package io.github.nogll.diplom.repository

import io.github.nogll.diplom.entity.Compound
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CompoundRepository : JpaRepository<Compound, Long> {
    fun findByName(name: String): Compound?
}


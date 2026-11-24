package io.github.eduardosantiag0.kaizan_companion.domain.repositories;

import io.github.eduardosantiag0.kaizan_companion.domain.entities.PracticePreferencesEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PracticePreferencesRepository extends JpaRepository<PracticePreferencesEntity, Long> {
}

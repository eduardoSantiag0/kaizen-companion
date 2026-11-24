package io.github.eduardosantiag0.kaizan_companion.domain.repositories;

import io.github.eduardosantiag0.kaizan_companion.domain.entities.PracticeExerciseEntity;
import io.github.eduardosantiag0.kaizan_companion.domain.entities.SessionEntity;
import io.github.eduardosantiag0.kaizan_companion.domain.entities.TelegramChatEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PracticeExerciseRepository extends JpaRepository<PracticeExerciseEntity, UUID> {
    List<PracticeExerciseEntity> findBySessionOrderByPositionOrder(SessionEntity session);

    Optional<PracticeExerciseEntity> findByIdAndSession_Chat(UUID id, TelegramChatEntity chat);

    List<PracticeExerciseEntity> findBySession_Chat(TelegramChatEntity chat);
}

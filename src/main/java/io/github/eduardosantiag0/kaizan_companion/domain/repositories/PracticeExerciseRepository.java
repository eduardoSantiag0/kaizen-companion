package io.github.eduardosantiag0.kaizan_companion.domain.repositories;

import io.github.eduardosantiag0.kaizan_companion.domain.entities.PracticeExerciseEntity;
import io.github.eduardosantiag0.kaizan_companion.domain.entities.SessionEntity;
import io.github.eduardosantiag0.kaizan_companion.domain.entities.TelegramChatEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PracticeExerciseRepository extends JpaRepository<PracticeExerciseEntity, UUID> {
    List<PracticeExerciseEntity> findBySessionOrderByPositionOrder(SessionEntity session);

    Optional<PracticeExerciseEntity> findByIdAndSession_IdAndSession_Chat(UUID id, UUID sessionId, TelegramChatEntity chat);

    @Modifying
    @Query("""
            update PracticeExerciseEntity exercise set exercise.completed = true
            where exercise.id = :id and exercise.session.id = :sessionId and exercise.completed = false
            """)
    int completeIfPending(UUID id, UUID sessionId);

    boolean existsBySessionAndCompletedFalse(SessionEntity session);

    List<PracticeExerciseEntity> findBySession_Chat(TelegramChatEntity chat);
}

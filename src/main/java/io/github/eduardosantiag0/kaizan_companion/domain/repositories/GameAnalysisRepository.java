package io.github.eduardosantiag0.kaizan_companion.domain.repositories;

import io.github.eduardosantiag0.kaizan_companion.domain.entities.AnalysisJobEntity;
import io.github.eduardosantiag0.kaizan_companion.domain.entities.GameAnalysisEntity;
import io.github.eduardosantiag0.kaizan_companion.domain.entities.TelegramChatEntity;
import io.github.eduardosantiag0.kaizan_companion.miniapp.GameHistoryDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameAnalysisRepository extends JpaRepository<GameAnalysisEntity, UUID> {
    boolean existsByJobEntity(AnalysisJobEntity jobEntity);

    Optional<GameAnalysisEntity> findByIdAndChat(UUID id, TelegramChatEntity chat);

    List<GameAnalysisEntity> findByChat(TelegramChatEntity chat);

    @Query("""
        SELECT new io.github.eduardosantiag0.kaizan_companion.miniapp.GameHistoryDto(
            g.id, g.jobEntity.correlationId, g.openingAnalysed, g.middlegameAnalysed, g.endgameAnalysed,
            g.blackPlayer, g.whitePlayer, g.gameRank
        )
        FROM GameAnalysisEntity g
        WHERE g.chat = :chat
        ORDER BY g.jobEntity.createdAt DESC
    """)
    List<GameHistoryDto> findHistoryByChat(@Param("chat") TelegramChatEntity chat);
}

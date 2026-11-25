package io.github.eduardosantiag0.kaizan_companion.domain.repositories;

import io.github.eduardosantiag0.kaizan_companion.domain.entities.SessionEntity;
import io.github.eduardosantiag0.kaizan_companion.domain.entities.TelegramChatEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<SessionEntity, UUID> {
    Optional<SessionEntity> findByChatAndSessionDateAndKind(TelegramChatEntity chat, LocalDate sessionDate, String kind);

    List<SessionEntity> findByChat(TelegramChatEntity chat);

    List<SessionEntity> findByChatAndKind(TelegramChatEntity chat, String kind);

    Optional<SessionEntity> findByIdAndChat(UUID id, TelegramChatEntity chat);
}

package io.github.eduardosantiag0.kaizan_companion.domain.repositories;

import io.github.eduardosantiag0.kaizan_companion.domain.entities.SessionEntity;
import io.github.eduardosantiag0.kaizan_companion.domain.entities.TelegramChatEntity;
import io.github.eduardosantiag0.kaizan_companion.miniapp.practice.SessionSummaryDto;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<SessionEntity, UUID> {
    Optional<SessionEntity> findByChatAndSessionDateAndKind(TelegramChatEntity chat, LocalDate sessionDate, String kind);

    @Query("""
            select new io.github.eduardosantiag0.kaizan_companion.miniapp.practice.SessionSummaryDto(
                session.id, session.kind, session.sessionDate, session.completed,
                count(exercise.id), sum(case when exercise.completed = true then 1L else 0L end))
            from SessionEntity session
            left join PracticeExerciseEntity exercise on exercise.session = session
            where session.chat = :chat and (:kind is null or session.kind = :kind)
            group by session.id, session.kind, session.sessionDate, session.completed, session.createdAt
            order by session.createdAt desc, session.id desc
            """)
    Slice<SessionSummaryDto> listSummaries(TelegramChatEntity chat, String kind, Pageable pageable);

    List<SessionEntity> findByChatAndKind(TelegramChatEntity chat, String kind);

    Optional<SessionEntity> findByIdAndChat(UUID id, TelegramChatEntity chat);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from SessionEntity session where session.id = :id and session.chat = :chat")
    Optional<SessionEntity> findForResultUpdate(UUID id, TelegramChatEntity chat);
}

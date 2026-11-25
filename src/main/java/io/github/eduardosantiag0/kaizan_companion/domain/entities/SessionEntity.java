package io.github.eduardosantiag0.kaizan_companion.domain.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "daily_session_db")
@Getter
public class SessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Getter
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "chat_id", nullable = false)
    private TelegramChatEntity chat;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "kind", nullable = false)
    private String kind;

    @Setter
    @Column(name = "completed", nullable = false)
    private boolean completed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public SessionEntity(TelegramChatEntity chat, LocalDate sessionDate, String kind) {
        this.chat = chat;
        this.sessionDate = sessionDate;
        this.kind = kind;
        this.completed = false;
        this.createdAt = Instant.now();
    }

    public SessionEntity() {
    }
}

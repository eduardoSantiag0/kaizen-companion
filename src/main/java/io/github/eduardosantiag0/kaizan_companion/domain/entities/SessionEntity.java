package io.github.eduardosantiag0.kaizan_companion.domain.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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

    @Setter
    @Column(name = "completed", nullable = false)
    private boolean completed;

    public SessionEntity(TelegramChatEntity chat, LocalDate sessionDate) {
        this.chat = chat;
        this.sessionDate = sessionDate;
        this.completed = false;
    }

    public SessionEntity() {
    }
}

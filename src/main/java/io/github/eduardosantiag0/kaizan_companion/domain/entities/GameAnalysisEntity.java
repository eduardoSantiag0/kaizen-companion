package io.github.eduardosantiag0.kaizan_companion.domain.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "game_analysis_results_db")
@Getter
public class GameAnalysisEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Getter
    private UUID id;

    @OneToOne
    @JoinColumn(name = "job_id", nullable = false)
    private AnalysisJobEntity jobEntity;

    @ManyToOne
    @JoinColumn(name = "chat_id", nullable = false)
    private TelegramChatEntity chat;

    @Column(name = "opening_analysed",
            columnDefinition = "TEXT")
    @Setter
    private String openingAnalysed;

    @Column(name = "midgame_analysed",
            columnDefinition = "TEXT")
    @Setter
    private String middlegameAnalysed;

    @Column(name = "endgame_analysed",
            columnDefinition = "TEXT")
    @Setter
    private String endgameAnalysed;

    @Column(name = "full_analysed",
            columnDefinition = "TEXT")
    @Setter
    private String fullAnalysedGame;


    @Column(name = "black_player")
    @Setter
    private String blackPlayer;

    @Column(name = "white_player")
    @Setter
    private String whitePlayer;

    @Column(name = "game_rank")
    @Setter
    private String gameRank;


    public GameAnalysisEntity(AnalysisJobEntity jobEntity, TelegramChatEntity chat) {
        this.jobEntity = jobEntity;
        this.chat = chat;
    }

    public GameAnalysisEntity() {
    }
}

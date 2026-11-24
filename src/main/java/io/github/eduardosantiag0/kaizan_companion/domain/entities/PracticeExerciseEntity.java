package io.github.eduardosantiag0.kaizan_companion.domain.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "practice_exercise_db")
@Getter
public class PracticeExerciseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Getter
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "session_id", nullable = false)
    private SessionEntity session;

    @ManyToOne
    @JoinColumn(name = "game_id", nullable = false)
    private GameAnalysisEntity game;

    @Column(name = "phase", nullable = false)
    private String phase;

    @Column(name = "position_order", nullable = false)
    private int positionOrder;

    @Setter
    @Column(name = "completed", nullable = false)
    private boolean completed;

    @Column(name = "top_variants", nullable = false)
    private int topVariants;

    @Setter
    @Column(name = "current_depth", nullable = false)
    private int currentDepth;

    @Setter
    @Column(name = "selected_variant")
    private Integer selectedVariant;

    @Setter
    @Column(name = "step", nullable = false)
    private int step;

    public PracticeExerciseEntity(SessionEntity session, GameAnalysisEntity game, String phase, int positionOrder,
                                   int topVariants, int currentDepth) {
        this.session = session;
        this.game = game;
        this.phase = phase;
        this.positionOrder = positionOrder;
        this.completed = false;
        this.topVariants = topVariants;
        this.currentDepth = currentDepth;
        this.selectedVariant = null;
        this.step = 1;
    }

    public PracticeExerciseEntity() {
    }
}

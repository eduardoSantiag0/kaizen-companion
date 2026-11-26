package io.github.eduardosantiag0.kaizan_companion.domain.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "practice_preferences_db")
@Getter
public class PracticePreferencesEntity {

    @Id
    @Column(name = "chat_id")
    private Long chatId;

    @Setter
    @Column(name = "top_variants", nullable = false)
    private int topVariants;

    @Setter
    @Column(name = "continuation_depth", nullable = false)
    private int continuationDepth;

    @Setter
    @Column(name = "exercise_count", nullable = false)
    private int exerciseCount;

    @Setter
    @Column(name = "exercise_source", nullable = false)
    private String exerciseSource;

    public PracticePreferencesEntity(Long chatId, int topVariants, int continuationDepth, int exerciseCount, String exerciseSource) {
        this.chatId = chatId;
        this.topVariants = topVariants;
        this.continuationDepth = continuationDepth;
        this.exerciseCount = exerciseCount;
        this.exerciseSource = exerciseSource;
    }

    public PracticePreferencesEntity() {
    }
}

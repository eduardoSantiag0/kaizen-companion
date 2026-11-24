package io.github.eduardosantiag0.kaizan_companion.miniapp.practice;

import java.util.UUID;

public record ExerciseDto(
        UUID id,
        UUID gameId,
        String phase,
        boolean completed,
        String opponentName,
        String opponentRank,
        int boardSize,
        int userSign,
        int moveNumber
) {
}

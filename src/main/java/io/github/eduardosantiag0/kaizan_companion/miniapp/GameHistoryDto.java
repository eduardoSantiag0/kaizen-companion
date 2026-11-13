package io.github.eduardosantiag0.kaizan_companion.miniapp;

import java.util.UUID;

public record GameHistoryDto(
        UUID id,
        UUID correlationId,
        String openingAnalysed,
        String middlegameAnalysed,
        String endgameAnalysed,
        String blackPlayer,
        String whitePlayer,
        String gameRank
) {
}

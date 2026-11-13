package io.github.eduardosantiag0.kaizan_companion.miniapp;

import java.util.UUID;

public record GameHistoryItemDto(
        UUID id,
        UUID correlationId,
        String status,
        String openingAnalysed,
        String middlegameAnalysed,
        String endgameAnalysed,
        String blackPlayer,
        String whitePlayer,
        String gameRank
) {
}

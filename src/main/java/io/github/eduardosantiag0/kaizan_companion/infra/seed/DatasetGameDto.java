package io.github.eduardosantiag0.kaizan_companion.infra.seed;

public record DatasetGameDto(
        String openingAnalysed,
        String middlegameAnalysed,
        String endgameAnalysed,
        String blackPlayer,
        String whitePlayer,
        String gameRank
) {
}

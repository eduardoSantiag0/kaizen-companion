package io.github.eduardosantiag0.kaizan_companion.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.eduardosantiag0.kaizan_companion.features.analysis.dtos.GamePhase;
import io.github.eduardosantiag0.kaizan_companion.features.analysis.dtos.runpod.CriticalPoint;
import io.github.eduardosantiag0.kaizan_companion.features.analysis.katago.KatagoAssembler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;


public final class PoolAssembler {

    private PoolAssembler() {
    }

    /** Mirrors miniapp.GameHistoryDto's shape — not reusing that record directly since blackPlayer/whitePlayer/gameRank have no source data here (see class javadoc) and this tool has no reason to depend on the miniapp package. */
    private record PoolEntry(
            String id, String correlationId, String openingAnalysed, String middlegameAnalysed, String endgameAnalysed,
            String blackPlayer, String whitePlayer, String gameRank
    ) {
    }

    private record RawGameAnalysis(String correlationId, String originalSgf, List<CriticalPoint> criticalPoints) {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Usage: PoolAssembler <input.jsonl> <output.json>");
            System.exit(1);
        }

        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        ObjectMapper mapper = new ObjectMapper();
        KatagoAssembler assembler = new KatagoAssembler();

        List<PoolEntry> entries = new ArrayList<>();
        int lineNumber = 0;
        int skipped = 0;

        for (String line : Files.readAllLines(input)) {
            lineNumber++;
            if (line.isBlank()) continue;

            RawGameAnalysis raw;
            try {
                raw = mapper.readValue(line, RawGameAnalysis.class);
            } catch (Exception e) {
                System.err.println("Line " + lineNumber + ": failed to parse — " + e.getMessage());
                skipped++;
                continue;
            }

            Map<GamePhase, String> analysedByPhase = new EnumMap<>(GamePhase.class);
            for (CriticalPoint point : raw.criticalPoints()) {
                try {
                    analysedByPhase.put(point.phase(), assembler.attachAnalysisToGame(point, raw.originalSgf()));
                } catch (Exception e) {
                    System.err.println(raw.correlationId() + "/" + point.phase() + ": failed to assemble — " + e.getMessage());
                }
            }

            if (analysedByPhase.isEmpty()) {
                System.err.println(raw.correlationId() + ": no phase could be assembled, skipping entirely");
                skipped++;
                continue;
            }

            entries.add(new PoolEntry(
                    raw.correlationId(), raw.correlationId(),
                    analysedByPhase.get(GamePhase.opening), analysedByPhase.get(GamePhase.middlegame), analysedByPhase.get(GamePhase.endgame),
                    null, null, null
            ));
        }

        mapper.writerWithDefaultPrettyPrinter().writeValue(output.toFile(), entries);
        System.out.printf("%d games assembled, %d skipped, written to %s%n", entries.size(), skipped, output);
    }
}

package io.github.eduardosantiag0.kaizan_companion.features.analysis.katago;

import io.github.eduardosantiag0.kaizan_companion.features.analysis.dtos.MoveInformation;
import io.github.eduardosantiag0.kaizan_companion.features.analysis.dtos.runpod.CriticalPoint;
import io.github.eduardosantiag0.sgf.go.*;
import io.github.eduardosantiag0.sgf.model.SgfCollection;
import io.github.eduardosantiag0.sgf.model.SgfGameTree;
import io.github.eduardosantiag0.sgf.model.SgfNode;
import io.github.eduardosantiag0.sgf.model.SgfProperty;
import io.github.eduardosantiag0.sgf.parser.SgfParser;
import io.github.eduardosantiag0.sgf.serializer.SgfSerializer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Component
public class KatagoAssembler {

    private static final int BEST_CANDIDATES_LIMIT = 10;
    private static final int VARIATION_MOVES_LIMIT = 11;

    private static final SgfParser sgfParser = new SgfParser();

    public KatagoAssembler() {
    }

    private static GoReplayBoard reconstructBoard(GoPosition positionBefore) {
        if (positionBefore.hasMidGameSetup()) {
            throw new IllegalArgumentException("Cannot replay a game with mid-game setup changes");
        }
        GoReplayBoard board = new GoReplayBoard(positionBefore.boardSize());
        for (SgfMove stone : positionBefore.setupStones()) {
            board.setup(stone.coordinate().orElseThrow(), stone.color());
        }
        for (SgfMove move : positionBefore.moves()) board.play(move);
        return board;
    }

    private static SgfNode buildSetupRoot(SgfGameTree originalTree, GoReplayBoard board, StoneColor toMove) {
        SgfNode newRoot = new SgfNode();

        Set<String> excludedProperties = Set.of(
                "AB", "AW", "AE", "PL", "C"
        );

        for (SgfProperty property : originalTree.root().properties()) {
            String identifier = property.identifier();

            if (!excludedProperties.contains(identifier)) {
                newRoot.setProperty(identifier, property.values());
            }
        }

        List<String> blackSetup = new ArrayList<>();

        List<String> whiteSetup = new ArrayList<>();

        board.position().forEach((coordinate, color) -> {
            String point = SgfMove.of(color, coordinate).toSgfValue();
            (color == StoneColor.BLACK ? blackSetup : whiteSetup).add(point);
        });

        if (!blackSetup.isEmpty()) {
            newRoot.setProperty("AB", blackSetup);
        }

        if (!whiteSetup.isEmpty()) {
            newRoot.setProperty("AW", whiteSetup);
        }

        newRoot.setProperty("PL", toMove.sgfIdentifier());

        return newRoot;
    }

    private static SgfGameTree attach(CriticalPoint criticalPoint, String originalGame) {

        SgfCollection collection = sgfParser.parse(originalGame);
        SgfGameTree originalTree = collection.firstGame();

        GoGame goGame = GoGame.of(originalTree);
        int boardSize = goGame.boardSize();

        SgfNode originalMoveNode = originalTree.getMove(criticalPoint.turn());

        SgfMove originalMove = SgfMove.from(originalMoveNode, boardSize).orElseThrow();

        GoReplayBoard board = reconstructBoard(goGame.positionBefore(originalMoveNode));
        SgfNode newRoot = buildSetupRoot(originalTree, board, originalMove.color());

        List<MoveInformation> bestCandidates = criticalPoint.before().moveInfos()
                .stream()
                .sorted(Comparator.comparingInt(MoveInformation::order))
                .limit(BEST_CANDIDATES_LIMIT)
                .toList();

        for (MoveInformation candidate : bestCandidates) {
            List<SgfMove> candidateMoves = new ArrayList<>();
            GoReplayBoard line = board.copy();
            StoneColor color = originalMove.color();
            for (String point : candidate.pv().stream().limit(VARIATION_MOVES_LIMIT).toList()) {
                if (point.equalsIgnoreCase("pass")) break;
                try {
                    SgfMove move = SgfMove.of(color, SgfCoordinate.fromHuman(point, boardSize));
                    line.play(move);
                    candidateMoves.add(move);
                    color = color.opposite();
                } catch (IllegalArgumentException e) {
                    candidateMoves.clear();
                    break;
                }
            }

            if (!candidateMoves.isEmpty()) {
                newRoot.addVariation(SgfMove.toNodes(candidateMoves));
            }
        }

        return new SgfGameTree(newRoot);
    }


    public String attachAnalysisToGame(CriticalPoint criticalPoint, String originalGame) {
        return SgfSerializer.pretty().serialize(attach(criticalPoint, originalGame));
    }

//    public ByteArrayResource assebleAllPhases(List<ByteArrayResource> gamePhases) {
//
//    }
}

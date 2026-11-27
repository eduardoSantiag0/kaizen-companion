package io.github.eduardosantiag0.kaizan_companion.miniapp.practice;

import io.github.eduardosantiag0.kaizan_companion.features.analysis.katago.GoReplayBoard;
import io.github.eduardosantiag0.sgf.go.GoNodes;
import io.github.eduardosantiag0.sgf.go.SgfMove;
import io.github.eduardosantiag0.sgf.go.StoneColor;
import io.github.eduardosantiag0.sgf.model.SgfNode;
import io.github.eduardosantiag0.sgf.parser.SgfParser;
import org.springframework.stereotype.Component;
import org.springframework.util.ConcurrentLruCache;

import java.util.Optional;

@Component
public class PracticeExerciseAssembler {

    private static final int MAX_PLIES = 11;
    private final SgfParser sgfParser = new SgfParser();

    private final ConcurrentLruCache<String, Optional<ExerciseDisplayInfo>> validationCache =
            new ConcurrentLruCache<>(256, this::validateUncached);

    public Optional<ExerciseDisplayInfo> validate(String sgf) {
        return sgf == null ? Optional.empty() : validationCache.get(sgf);
    }

    private Optional<ExerciseDisplayInfo> validateUncached(String sgf) {
        try {
            SgfNode root = sgfParser.parse(sgf).firstGame().root();
            String player = root.value("PL").orElse("B");
            if (!player.equals("B") && !player.equals("W")) return Optional.empty();

            int size = Integer.parseInt(root.value("SZ").orElse("19"));
            StoneColor user = player.equals("W") ? StoneColor.WHITE : StoneColor.BLACK;
            GoReplayBoard board = new GoReplayBoard(size);
            for (var point : GoNodes.points(root, "AB", size)) board.setup(point, StoneColor.BLACK);
            for (var point : GoNodes.points(root, "AW", size)) board.setup(point, StoneColor.WHITE);
            if (root.isMove() || root.hasProperty("AE") || !setupPointsMatchFrontend(root)) return Optional.empty();

            SgfNode branch = root;
            while (branch.children().size() == 1) branch = branch.children().getFirst();
            int moveNumber = 1;
            if (branch.children().isEmpty()) {
                // PL on a setup root identifies a single-line exercise.
                if (!root.hasProperty("PL")) return Optional.empty();
                branch = root;
            } else {
                for (SgfNode node = root; node != branch; ) {
                    node = node.children().getFirst();
                    if (node.isMove()) {
                        board.play(readMove(node, size));
                        moveNumber++;
                    } else if (hasSetup(node)) {
                        return Optional.empty();
                    }
                }
            }

            boolean hasPlayableVariant = false;
            for (SgfNode child : branch.children()) {
                if (hasSetup(child)) return Optional.empty();
                if (!child.isMove()) continue;
                GoReplayBoard line = board.copy();
                StoneColor expected = user;
                int plies = 0;
                for (SgfNode node = child; node != null && plies < MAX_PLIES && node.isMove();
                     node = node.next().orElse(null)) {
                    SgfMove move = readMove(node, size);

                    if (move.isPass() || move.color() != expected) return Optional.empty();
                    line.play(move);
                    expected = expected.opposite();
                    plies++;
                }
                hasPlayableVariant = true;
            }
            if (!hasPlayableVariant) return Optional.empty();

            boolean black = user == StoneColor.BLACK;
            return Optional.of(new ExerciseDisplayInfo(black ? 1 : -1,
                    root.value(black ? "PW" : "PB").orElse(""),
                    root.value(black ? "WR" : "BR").orElse(""), size, moveNumber));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private boolean setupPointsMatchFrontend(SgfNode root) {
        return root.values("AB").stream().allMatch(point -> point.matches("[a-s]{2}"))
                && root.values("AW").stream().allMatch(point -> point.matches("[a-s]{2}"));
    }

    private boolean hasSetup(SgfNode node) {
        return node.hasProperty("AB") || node.hasProperty("AW") || node.hasProperty("AE");
    }

    private SgfMove readMove(SgfNode node, int size) {
        if (hasSetup(node) || node.values("B").size() + node.values("W").size() != 1) {
            throw new IllegalArgumentException("Expected one move");
        }
        return SgfMove.from(node, size).orElseThrow();
    }
}

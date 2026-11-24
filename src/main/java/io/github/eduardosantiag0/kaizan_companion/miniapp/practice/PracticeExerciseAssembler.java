package io.github.eduardosantiag0.kaizan_companion.miniapp.practice;

import io.github.eduardosantiag0.sgf.model.SgfNode;
import io.github.eduardosantiag0.sgf.parser.SgfParser;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PracticeExerciseAssembler {

    private static final int BLACK = 1;
    private static final int WHITE = -1;

    private final SgfParser sgfParser = new SgfParser();

    public Optional<ExerciseDisplayInfo> tryDisplayInfo(String sgf) {
        try {
            SgfNode root = sgfParser.parse(sgf).firstGame().root();
            if (!hasBranchPoint(root)) {
                return Optional.empty();
            }
            return Optional.of(displayInfo(root));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private ExerciseDisplayInfo displayInfo(SgfNode root) {
        int userSign = "W".equalsIgnoreCase(root.value("PL").orElse("B")) ? WHITE : BLACK;
        String blackName = root.value("PB").orElse("");
        String whiteName = root.value("PW").orElse("");
        String blackRank = root.value("BR").orElse("");
        String whiteRank = root.value("WR").orElse("");

        return new ExerciseDisplayInfo(
                userSign,
                userSign == BLACK ? whiteName : blackName,
                userSign == BLACK ? whiteRank : blackRank,
                parseBoardSize(root),
                branchPointDepth(root)
        );
    }

    private int parseBoardSize(SgfNode root) {
        try {
            return Integer.parseInt(root.value("SZ").orElse("19"));
        } catch (NumberFormatException e) {
            return 19;
        }
    }

    private boolean hasBranchPoint(SgfNode root) {
        SgfNode node = root;
        while (node != null) {
            if (node.children().size() > 1) {
                return true;
            }
            if (node.children().isEmpty()) {
                return false;
            }
            node = node.children().get(0);
        }
        return false;
    }

    private int branchPointDepth(SgfNode root) {
        int count = 0;
        SgfNode node = root;
        while (node != null && node.children().size() == 1) {
            if (node.isMove()) {
                count++;
            }
            node = node.children().get(0);
        }
        return count + 1;
    }
}

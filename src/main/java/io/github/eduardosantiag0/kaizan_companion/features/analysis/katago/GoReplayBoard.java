package io.github.eduardosantiag0.kaizan_companion.features.analysis.katago;

import io.github.eduardosantiag0.sgf.go.SgfCoordinate;
import io.github.eduardosantiag0.sgf.go.SgfMove;
import io.github.eduardosantiag0.sgf.go.StoneColor;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.IntStream;

public final class GoReplayBoard {
    private final int size;
    private byte[] stones;
    private byte[] previous;

    public GoReplayBoard(int size) {
        if (size < 2 || size > 19) throw new IllegalArgumentException("Unsupported Go board size");
        this.size = size;
        this.stones = new byte[size * size];
    }

    public GoReplayBoard copy() {
        var copy = new GoReplayBoard(size);
        copy.stones = stones.clone();
        copy.previous = previous;
        return copy;
    }

    public void setup(SgfCoordinate point, StoneColor color) {
        int index = index(point);
        if (stones[index] != 0) throw new IllegalArgumentException("Overlapping setup stones");
        stones[index] = sign(color);
    }

    public Map<SgfCoordinate, StoneColor> position() {
        Map<SgfCoordinate, StoneColor> position = new LinkedHashMap<>();
        for (int i = 0; i < stones.length; i++) {
            if (stones[i] != 0) {
                position.put(new SgfCoordinate(i % size, i / size),
                        stones[i] == 1 ? StoneColor.BLACK : StoneColor.WHITE);
            }
        }
        return position;
    }

    public void play(SgfMove move) {
        if (move.isPass()) {
            previous = stones;
            return;
        }
        int played = index(move.coordinate().orElseThrow());
        if (stones[played] != 0) throw new IllegalArgumentException("Occupied point");

        byte[] next = stones.clone();
        byte color = sign(move.color());
        next[played] = color;
        neighbors(played).filter(p -> next[p] == -color).forEach(p -> removeIfCaptured(next, p));
        if (removeIfCaptured(next, played)) throw new IllegalArgumentException("Suicide");
        if (Arrays.equals(previous, next)) throw new IllegalArgumentException("Ko");

        previous = stones;
        stones = next;
    }

    private boolean removeIfCaptured(byte[] board, int start) {
        var group = new ArrayDeque<Integer>();
        var frontier = new ArrayDeque<Integer>();
        boolean[] seen = new boolean[board.length];
        byte color = board[start];
        frontier.add(start);
        seen[start] = true;
        while (!frontier.isEmpty()) {
            int point = frontier.remove();
            group.add(point);
            int[] adjacent = neighbors(point).toArray();
            for (int neighbor : adjacent) {
                if (board[neighbor] == 0) return false;
                if (board[neighbor] == color && !seen[neighbor]) {
                    frontier.add(neighbor);
                    seen[neighbor] = true;
                }
            }
        }
        group.forEach(point -> board[point] = 0);
        return true;
    }

    private IntStream neighbors(int point) {
        return IntStream.of(
                point % size == 0 ? -1 : point - 1,
                point % size == size - 1 ? -1 : point + 1,
                point - size,
                point + size
        ).filter(p -> p >= 0 && p < stones.length);
    }

    private int index(SgfCoordinate point) {
        if (!point.isOnBoard(size)) throw new IllegalArgumentException("Point outside Go board");
        return point.y() * size + point.x();
    }

    private byte sign(StoneColor color) {
        return (byte) (color == StoneColor.BLACK ? 1 : -1);
    }
}

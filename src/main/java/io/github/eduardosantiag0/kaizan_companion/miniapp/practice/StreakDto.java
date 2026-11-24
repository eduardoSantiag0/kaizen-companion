package io.github.eduardosantiag0.kaizan_companion.miniapp.practice;

import java.util.List;

public record StreakDto(int currentStreak, List<Boolean> last7Days) {
}

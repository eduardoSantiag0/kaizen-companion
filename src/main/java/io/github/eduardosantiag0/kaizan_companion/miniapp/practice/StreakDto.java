package io.github.eduardosantiag0.kaizan_companion.miniapp.practice;

import java.time.LocalDate;
import java.util.List;

public record StreakDto(int currentStreak, List<Boolean> last7Days, LocalDate date, String timeZone) {
}

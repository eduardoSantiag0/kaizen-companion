package io.github.eduardosantiag0.kaizan_companion.miniapp.practice;

import java.time.LocalDate;
import java.util.UUID;

public record SessionSummaryDto(UUID id, String kind, LocalDate date, boolean completed,
                                Long exerciseCount, Long completedCount) {
}

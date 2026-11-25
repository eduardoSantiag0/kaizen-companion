package io.github.eduardosantiag0.kaizan_companion.miniapp.practice;

import java.util.List;
import java.util.UUID;

public record SessionDto(UUID id, String kind, String date, boolean completed, List<ExerciseDto> exercises) {
}

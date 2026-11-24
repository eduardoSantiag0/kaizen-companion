package io.github.eduardosantiag0.kaizan_companion.miniapp.practice;

import java.util.List;

public record SessionDto(String date, boolean completed, List<ExerciseDto> exercises) {
}

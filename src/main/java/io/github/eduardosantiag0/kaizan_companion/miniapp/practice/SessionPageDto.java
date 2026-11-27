package io.github.eduardosantiag0.kaizan_companion.miniapp.practice;

import java.util.List;

public record SessionPageDto(List<SessionSummaryDto> sessions, Integer nextPage) {
}

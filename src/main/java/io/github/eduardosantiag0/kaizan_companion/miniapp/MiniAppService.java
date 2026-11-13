package io.github.eduardosantiag0.kaizan_companion.miniapp;

import io.github.eduardosantiag0.kaizan_companion.domain.entities.AnalysisJobEntity;
import io.github.eduardosantiag0.kaizan_companion.domain.entities.JobStatus;
import io.github.eduardosantiag0.kaizan_companion.domain.entities.TelegramChatEntity;
import io.github.eduardosantiag0.kaizan_companion.domain.repositories.AnalysisJobRepository;
import io.github.eduardosantiag0.kaizan_companion.domain.repositories.GameAnalysisRepository;
import io.github.eduardosantiag0.kaizan_companion.domain.repositories.TelegramChatRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class MiniAppService {

    private static final Set<JobStatus> IN_PROGRESS_STATUSES = EnumSet.of(
            JobStatus.ANALYSIS_REQUESTED, JobStatus.SUBMITTING, JobStatus.IN_QUEUE
    );

    private final GameAnalysisRepository gameRepository;
    private final TelegramChatRepository telegramChatRepository;
    private final AnalysisJobRepository analysisJobRepository;

    public MiniAppService(GameAnalysisRepository gameRepository, TelegramChatRepository telegramChatRepository,
                           AnalysisJobRepository analysisJobRepository) {
        this.gameRepository = gameRepository;
        this.telegramChatRepository = telegramChatRepository;
        this.analysisJobRepository = analysisJobRepository;
    }

    public List<GameHistoryItemDto> findAllByTelegramUserId(Long telegramUserId) {

        var chatEntity = telegramChatRepository.findById(telegramUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Telegram chat not found"));

        return mergedHistoryFor(chatEntity);
    }

    private List<GameHistoryItemDto> mergedHistoryFor(TelegramChatEntity chatEntity) {
        List<RankedItem> ranked = new ArrayList<>();

        for (GameHistoryDto dto : gameRepository.findHistoryByChat(chatEntity)) {
            Instant createdAt = analysisJobRepository.findByCorrelationId(dto.correlationId())
                    .map(AnalysisJobEntity::getCreatedAt)
                    .orElse(Instant.EPOCH);
            ranked.add(new RankedItem(toCompletedItem(dto), createdAt));
        }

        for (AnalysisJobEntity job : analysisJobRepository.findByChatAndJobStatusIn(chatEntity, IN_PROGRESS_STATUSES)) {
            ranked.add(new RankedItem(toInProgressItem(job), job.getCreatedAt()));
        }

        return ranked.stream()
                .sorted(Comparator.comparing(RankedItem::createdAt).reversed())
                .map(RankedItem::item)
                .toList();
    }

    private GameHistoryItemDto toCompletedItem(GameHistoryDto dto) {
        return new GameHistoryItemDto(
                dto.id(), dto.correlationId(), "DONE",
                dto.openingAnalysed(), dto.middlegameAnalysed(), dto.endgameAnalysed(),
                dto.blackPlayer(), dto.whitePlayer(), dto.gameRank()
        );
    }

    private GameHistoryItemDto toInProgressItem(AnalysisJobEntity job) {
        return new GameHistoryItemDto(
                job.getId(), job.getCorrelationId(), translateInProgressStatus(job.getJobStatus()),
                null, null, null,
                null, null, null
        );
    }

    private String translateInProgressStatus(JobStatus jobStatus) {
        return switch (jobStatus) {
            case ANALYSIS_REQUESTED, SUBMITTING -> "WAITING";
            case IN_QUEUE -> "RUNNING";
            default -> throw new IllegalStateException(
                    "Unexpected job status for an in-progress job: " + jobStatus);
        };
    }

    private record RankedItem(GameHistoryItemDto item, Instant createdAt) {
    }
}

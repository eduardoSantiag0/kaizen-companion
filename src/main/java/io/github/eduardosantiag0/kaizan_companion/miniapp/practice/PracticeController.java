package io.github.eduardosantiag0.kaizan_companion.miniapp.practice;

import io.github.eduardosantiag0.kaizan_companion.miniapp.TelegramInitDataService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/kaizan/api/v1/practice")
public class PracticeController {

    private final PracticeService service;
    private final TelegramInitDataService telegramInitDataService;

    public PracticeController(PracticeService service, TelegramInitDataService telegramInitDataService) {
        this.service = service;
        this.telegramInitDataService = telegramInitDataService;
    }

    @GetMapping("today")
    public ResponseEntity<SessionDto> getToday(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        if (authorization == null || !authorization.startsWith("tma ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        String initData = authorization.substring(4);
        Long telegramUserId = telegramInitDataService.validateAndGetUserId(initData);

        return service.getTodaySession(telegramUserId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("today/start")
    public ResponseEntity<SessionDto> startToday(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        if (authorization == null || !authorization.startsWith("tma ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        String initData = authorization.substring(4);
        Long telegramUserId = telegramInitDataService.validateAndGetUserId(initData);

        return ResponseEntity.ok(service.startTodaySession(telegramUserId));
    }

    @PostMapping("today/new-round")
    public ResponseEntity<SessionDto> startNewRound(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        if (authorization == null || !authorization.startsWith("tma ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        String initData = authorization.substring(4);
        Long telegramUserId = telegramInitDataService.validateAndGetUserId(initData);

        return ResponseEntity.ok(service.startNewRound(telegramUserId));
    }

    @PostMapping("random")
    public ResponseEntity<SessionDto> startRandomPractice(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam int count
    ) {
        if (authorization == null || !authorization.startsWith("tma ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        String initData = authorization.substring(4);
        Long telegramUserId = telegramInitDataService.validateAndGetUserId(initData);

        return ResponseEntity.ok(service.startRandomPractice(telegramUserId, count));
    }

    @GetMapping("sessions")
    public ResponseEntity<SessionPageDto> listSessions(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String kind
    ) {
        if (authorization == null || !authorization.startsWith("tma ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        String initData = authorization.substring(4);
        Long telegramUserId = telegramInitDataService.validateAndGetUserId(initData);

        return ResponseEntity.ok(service.listSessions(telegramUserId, page, size, kind));
    }

    @GetMapping("sessions/{sessionId}")
    public ResponseEntity<SessionDto> getSession(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID sessionId
    ) {
        if (authorization == null || !authorization.startsWith("tma ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        String initData = authorization.substring(4);
        Long telegramUserId = telegramInitDataService.validateAndGetUserId(initData);

        return service.getSession(telegramUserId, sessionId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("sessions/{sessionId}/exercises/{exerciseId}/result")
    public ResponseEntity<Void> recordResult(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID sessionId,
            @PathVariable UUID exerciseId
    ) {
        if (authorization == null || !authorization.startsWith("tma ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        String initData = authorization.substring(4);
        Long telegramUserId = telegramInitDataService.validateAndGetUserId(initData);

        service.recordResult(telegramUserId, sessionId, exerciseId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("streak")
    public ResponseEntity<StreakDto> getStreak(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        if (authorization == null || !authorization.startsWith("tma ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        String initData = authorization.substring(4);
        Long telegramUserId = telegramInitDataService.validateAndGetUserId(initData);

        return ResponseEntity.ok(service.getStreak(telegramUserId));
    }

    @GetMapping("preferences")
    public ResponseEntity<PreferencesDto> getPreferences(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        if (authorization == null || !authorization.startsWith("tma ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        String initData = authorization.substring(4);
        Long telegramUserId = telegramInitDataService.validateAndGetUserId(initData);

        return ResponseEntity.ok(service.getPreferences(telegramUserId));
    }

    @PutMapping("preferences")
    public ResponseEntity<Void> setPreferences(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody PreferencesDto preferences
    ) {
        if (authorization == null || !authorization.startsWith("tma ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        String initData = authorization.substring(4);
        Long telegramUserId = telegramInitDataService.validateAndGetUserId(initData);

        service.setPreferences(telegramUserId, preferences);
        return ResponseEntity.noContent().build();
    }
}

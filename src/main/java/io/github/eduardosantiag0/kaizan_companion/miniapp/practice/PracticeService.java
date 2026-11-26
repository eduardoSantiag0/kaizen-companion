package io.github.eduardosantiag0.kaizan_companion.miniapp.practice;

import io.github.eduardosantiag0.kaizan_companion.domain.entities.GameAnalysisEntity;
import io.github.eduardosantiag0.kaizan_companion.domain.entities.PracticeExerciseEntity;
import io.github.eduardosantiag0.kaizan_companion.domain.entities.PracticePreferencesEntity;
import io.github.eduardosantiag0.kaizan_companion.domain.entities.SessionEntity;
import io.github.eduardosantiag0.kaizan_companion.domain.entities.TelegramChatEntity;
import io.github.eduardosantiag0.kaizan_companion.domain.repositories.GameAnalysisRepository;
import io.github.eduardosantiag0.kaizan_companion.domain.repositories.PracticeExerciseRepository;
import io.github.eduardosantiag0.kaizan_companion.domain.repositories.PracticePreferencesRepository;
import io.github.eduardosantiag0.kaizan_companion.domain.repositories.SessionRepository;
import io.github.eduardosantiag0.kaizan_companion.domain.repositories.TelegramChatRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@Slf4j
public class PracticeService {

    private static final int DEFAULT_TOP_VARIANTS = 3;
    private static final int DEFAULT_CONTINUATION_DEPTH = 4;
    private static final int DEFAULT_EXERCISE_COUNT = 5;
    private static final String DEFAULT_EXERCISE_SOURCE = "own";
    private static final String EXERCISE_SOURCE_MIXED = "mixed";

    private final TelegramChatRepository telegramChatRepository;
    private final SessionRepository sessionRepository;
    private final PracticeExerciseRepository practiceExerciseRepository;
    private final PracticePreferencesRepository practicePreferencesRepository;
    private final GameAnalysisRepository gameAnalysisRepository;
    private final PracticeExerciseAssembler assembler;

    public PracticeService(TelegramChatRepository telegramChatRepository, SessionRepository sessionRepository,
                            PracticeExerciseRepository practiceExerciseRepository,
                            PracticePreferencesRepository practicePreferencesRepository,
                            GameAnalysisRepository gameAnalysisRepository, PracticeExerciseAssembler assembler) {
        this.telegramChatRepository = telegramChatRepository;
        this.sessionRepository = sessionRepository;
        this.practiceExerciseRepository = practiceExerciseRepository;
        this.practicePreferencesRepository = practicePreferencesRepository;
        this.gameAnalysisRepository = gameAnalysisRepository;
        this.assembler = assembler;
    }

    public Optional<SessionDto> getTodaySession(Long telegramUserId) {
        TelegramChatEntity chat = requireChat(telegramUserId);
        return sessionRepository.findByChatAndSessionDateAndKind(chat, LocalDate.now(), "daily").map(this::toSessionDto);
    }

    @Transactional
    public SessionDto startTodaySession(Long telegramUserId) {
        TelegramChatEntity chat = requireChat(telegramUserId);
        LocalDate today = LocalDate.now();

        SessionEntity session = sessionRepository.findByChatAndSessionDateAndKind(chat, today, "daily")
                .orElseGet(() -> sessionRepository.save(new SessionEntity(chat, today, "daily")));

        if (!hasExercises(session)) {
            populateExercises(session, chat, resolvePreferences(telegramUserId));
        }

        return toSessionDto(session);
    }


    @Transactional
    public SessionDto startNewRound(Long telegramUserId) {
        TelegramChatEntity chat = requireChat(telegramUserId);
        return toSessionDto(createSession(chat, LocalDate.now(), "generated", resolvePreferences(telegramUserId)));
    }

    public List<SessionDto> listSessions(Long telegramUserId) {
        TelegramChatEntity chat = requireChat(telegramUserId);
        List<SessionEntity> sessions = new ArrayList<>(sessionRepository.findByChat(chat));
        sessions.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return sessions.stream().map(this::toSessionDto).toList();
    }

    public Optional<SessionDto> getSession(Long telegramUserId, UUID sessionId) {
        TelegramChatEntity chat = requireChat(telegramUserId);
        return sessionRepository.findByIdAndChat(sessionId, chat).map(this::toSessionDto);
    }

    @Transactional
    public void recordResult(Long telegramUserId, UUID exerciseId) {
        TelegramChatEntity chat = requireChat(telegramUserId);
        PracticeExerciseEntity exercise = practiceExerciseRepository.findByIdAndSession_Chat(exerciseId, chat)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exercise not found"));

        exercise.setCompleted(true);
        practiceExerciseRepository.save(exercise);

        SessionEntity session = exercise.getSession();
        boolean allCompleted = practiceExerciseRepository.findBySessionOrderByPositionOrder(session)
                .stream()
                .allMatch(PracticeExerciseEntity::isCompleted);

        if (allCompleted) {
            session.setCompleted(true);
            sessionRepository.save(session);
        }
    }

    public PreferencesDto getPreferences(Long telegramUserId) {
        requireChat(telegramUserId);
        return resolvePreferences(telegramUserId);
    }

    @Transactional
    public void saveProgress(Long telegramUserId, UUID exerciseId, ProgressDto progress) {
        TelegramChatEntity chat = requireChat(telegramUserId);
        PracticeExerciseEntity exercise = practiceExerciseRepository.findByIdAndSession_Chat(exerciseId, chat)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exercise not found"));

        exercise.setSelectedVariant(progress.selectedVariant());
        exercise.setStep(progress.step());
        exercise.setCurrentDepth(progress.currentDepth());
        practiceExerciseRepository.save(exercise);
    }


    public StreakDto getStreak(Long telegramUserId) {
        TelegramChatEntity chat = requireChat(telegramUserId);
        Map<LocalDate, SessionEntity> byDate = new LinkedHashMap<>();
        for (SessionEntity session : sessionRepository.findByChatAndKind(chat, "daily")) {
            byDate.put(session.getSessionDate(), session);
        }

        LocalDate today = LocalDate.now();

        SessionEntity todaySession = byDate.get(today);
        boolean todayCounts = todaySession != null && hasExercises(todaySession) && todaySession.isCompleted();

        int streak = 0;
        LocalDate cursor = todayCounts ? today : today.minusDays(1);
        while (true) {
            SessionEntity session = byDate.get(cursor);
            if (session == null) break;
            if (!hasExercises(session)) {
                cursor = cursor.minusDays(1);
                continue;
            }
            if (!session.isCompleted()) break;
            streak++;
            cursor = cursor.minusDays(1);
        }

        List<Boolean> last7Days = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            SessionEntity session = byDate.get(date);
            last7Days.add(session != null && session.isCompleted() && hasExercises(session));
        }

        return new StreakDto(streak, last7Days);
    }

    private boolean hasExercises(SessionEntity session) {
        return !practiceExerciseRepository.findBySessionOrderByPositionOrder(session).isEmpty();
    }

    private PreferencesDto resolvePreferences(Long telegramUserId) {
        return practicePreferencesRepository.findById(telegramUserId)
                .map(p -> new PreferencesDto(p.getTopVariants(), p.getContinuationDepth(), p.getExerciseCount(), p.getExerciseSource()))
                .orElse(new PreferencesDto(DEFAULT_TOP_VARIANTS, DEFAULT_CONTINUATION_DEPTH, DEFAULT_EXERCISE_COUNT, DEFAULT_EXERCISE_SOURCE));
    }

    @Transactional
    public void setPreferences(Long telegramUserId, PreferencesDto preferences) {
        requireChat(telegramUserId);
        validateRange(preferences);

        PracticePreferencesEntity entity = practicePreferencesRepository.findById(telegramUserId)
                .orElseGet(() -> new PracticePreferencesEntity(
                        telegramUserId, DEFAULT_TOP_VARIANTS, DEFAULT_CONTINUATION_DEPTH, DEFAULT_EXERCISE_COUNT, DEFAULT_EXERCISE_SOURCE));
        entity.setTopVariants(preferences.topVariants());
        entity.setContinuationDepth(preferences.continuationDepth());
        entity.setExerciseCount(preferences.exerciseCount());
        entity.setExerciseSource(preferences.exerciseSource());
        practicePreferencesRepository.save(entity);
    }

    private TelegramChatEntity requireChat(Long telegramUserId) {
        return telegramChatRepository.findById(telegramUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Telegram chat not found"));
    }

    private void validateRange(PreferencesDto preferences) {
        if (preferences.topVariants() < 1 || preferences.topVariants() > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "topVariants must be between 1 and 5");
        }
        if (preferences.continuationDepth() < 2 || preferences.continuationDepth() > 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "continuationDepth must be between 2 and 10");
        }
        if (preferences.exerciseCount() < 1 || preferences.exerciseCount() > 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "exerciseCount must be between 1 and 10");
        }
        if (!"own".equals(preferences.exerciseSource()) && !EXERCISE_SOURCE_MIXED.equals(preferences.exerciseSource())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "exerciseSource must be \"own\" or \"mixed\"");
        }
    }


    private SessionEntity createSession(TelegramChatEntity chat, LocalDate date, String kind, PreferencesDto preferences) {
        SessionEntity session = sessionRepository.save(new SessionEntity(chat, date, kind));
        populateExercises(session, chat, preferences);
        return session;
    }

    /** Fills an existing, exercise-less session from the chat's candidate pool — shared by
     * createSession (brand-new session) and startTodaySession's retry on an existing-but-empty
     * one. No-op (leaves the session exactly as it was) if the pool has nothing usable.
     * preferences.exerciseCount() caps how many exercises this fills in (was a hardcoded 5).
     * preferences.exerciseSource() ("own" vs "mixed") is persisted and validated but NOT YET
     * wired in here — every session is still built from this chat's own games only. There is
     * no "community" pool of other chats' analyzed games on the backend at all right now, and
     * building one means deciding whether/how one user's private game analyses get surfaced to
     * another — a real product/privacy call this fix doesn't make unilaterally. Until that
     * pool exists, "mixed" behaves exactly like "own". */
    private void populateExercises(SessionEntity session, TelegramChatEntity chat, PreferencesDto preferences) {
        Set<String> usedKeys = new HashSet<>();
        for (PracticeExerciseEntity exercise : practiceExerciseRepository.findBySession_Chat(chat)) {
            usedKeys.add(usageKey(exercise.getGame().getId(), exercise.getPhase()));
        }

        List<Candidate> unused = new ArrayList<>();
        List<Candidate> used = new ArrayList<>();

        for (GameAnalysisEntity game : gameAnalysisRepository.findByChat(chat)) {
            for (Candidate candidate : candidatesOf(game)) {
                boolean wasUsed = usedKeys.contains(usageKey(candidate.game().getId(), candidate.phase()));
                (wasUsed ? used : unused).add(candidate);
            }
        }

        Collections.shuffle(unused);
        Collections.shuffle(used);

        List<Candidate> ordered = new ArrayList<>(unused);
        ordered.addAll(used);

        int positionOrder = 1;
        int rejectedUnreadable = 0;
        for (Candidate candidate : ordered) {
            if (positionOrder > preferences.exerciseCount()) {
                break;
            }
            if (assembler.tryDisplayInfo(candidate.sgf()).isEmpty()) {
                rejectedUnreadable++;
                continue;
            }
            practiceExerciseRepository.save(
                    new PracticeExerciseEntity(session, candidate.game(), candidate.phase(), positionOrder,
                            preferences.topVariants(), preferences.continuationDepth()));
            positionOrder++;
        }

        if (positionOrder == 1) {
            log.warn("populateExercises found nothing usable for session {} (chat {}): games={}, candidates={}, rejectedUnreadable={}",
                    session.getId(), chat.getChatId(), gameAnalysisRepository.findByChat(chat).size(), ordered.size(), rejectedUnreadable);
            session.setCompleted(true);
            sessionRepository.save(session);
        }
    }

    private List<Candidate> candidatesOf(GameAnalysisEntity game) {
        List<Candidate> candidates = new ArrayList<>();
        if (game.getOpeningAnalysed() != null) {
            candidates.add(new Candidate(game, "opening", game.getOpeningAnalysed()));
        }
        if (game.getMiddlegameAnalysed() != null) {
            candidates.add(new Candidate(game, "middlegame", game.getMiddlegameAnalysed()));
        }
        if (game.getEndgameAnalysed() != null) {
            candidates.add(new Candidate(game, "endgame", game.getEndgameAnalysed()));
        }
        return candidates;
    }

    private static String usageKey(UUID gameId, String phase) {
        return gameId + ":" + phase;
    }

    private SessionDto toSessionDto(SessionEntity session) {
        List<ExerciseDto> exercises = practiceExerciseRepository.findBySessionOrderByPositionOrder(session)
                .stream()
                .flatMap(exercise -> {
                    try {
                        return Stream.of(toExerciseDto(exercise));
                    } catch (IllegalStateException e) {
                        log.error("Dropping unreadable exercise {} from session {}", exercise.getId(), session.getId(), e);
                        return Stream.<ExerciseDto>empty();
                    }
                })
                .toList();
        return new SessionDto(session.getId(), session.getKind(), session.getSessionDate().toString(), session.isCompleted(), exercises);
    }

    private ExerciseDto toExerciseDto(PracticeExerciseEntity exercise) {
        GameAnalysisEntity game = exercise.getGame();
        String sgf = sgfForPhase(game, exercise.getPhase());
        ExerciseDisplayInfo info = assembler.tryDisplayInfo(sgf)
                .orElseThrow(() -> new IllegalStateException("Persisted exercise has unparsable SGF: " + exercise.getId()));

        return new ExerciseDto(
                exercise.getId(), game.getId(), exercise.getPhase(), exercise.isCompleted(),
                info.opponentName(), info.opponentRank(), info.boardSize(), info.userSign(), info.moveNumber(),
                exercise.getTopVariants(), exercise.getCurrentDepth(), exercise.getSelectedVariant(), exercise.getStep()
        );
    }

    private String sgfForPhase(GameAnalysisEntity game, String phase) {
        return switch (phase) {
            case "opening" -> game.getOpeningAnalysed();
            case "middlegame" -> game.getMiddlegameAnalysed();
            case "endgame" -> game.getEndgameAnalysed();
            default -> throw new IllegalStateException("Unknown phase: " + phase);
        };
    }

    private record Candidate(GameAnalysisEntity game, String phase, String sgf) {
    }
}

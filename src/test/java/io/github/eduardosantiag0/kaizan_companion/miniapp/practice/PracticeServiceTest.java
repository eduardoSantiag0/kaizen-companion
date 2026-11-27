package io.github.eduardosantiag0.kaizan_companion.miniapp.practice;

import io.github.eduardosantiag0.kaizan_companion.domain.entities.AnalysisJobEntity;
import io.github.eduardosantiag0.kaizan_companion.domain.entities.ExerciseOrigin;
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
import io.github.eduardosantiag0.kaizan_companion.infra.seed.DatasetGameDto;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PracticeServiceTest {

    // Root has two direct variations (B[pd], B[dd]) — a branch point at move 1, same minimal
    // fixture shape kaizen-miniapp's own exercise-tree tests use.
    private static final String BRANCHING_SGF =
            "(;FF[4]SZ[19]PB[blackPlayer]PW[whitePlayer]BR[5k]WR[3d]PL[B](;B[pd];W[dp])(;B[dd]))";

    // No second child anywhere on the main line — not exercise-able.
    private static final String NO_BRANCH_SGF = "(;FF[4]SZ[19]PL[B];B[pd];W[dp])";

    private final TelegramChatRepository chatRepository = mock(TelegramChatRepository.class);
    private final SessionRepository sessionRepository = mock(SessionRepository.class);
    private final PracticeExerciseRepository exerciseRepository = mock(PracticeExerciseRepository.class);
    private final PracticePreferencesRepository preferencesRepository = mock(PracticePreferencesRepository.class);
    private final GameAnalysisRepository gameRepository = mock(GameAnalysisRepository.class);
    private final PracticeExerciseAssembler assembler = new PracticeExerciseAssembler();

    private final PracticeService service = new PracticeService(
            chatRepository, sessionRepository, exerciseRepository, preferencesRepository, gameRepository, assembler);

    private static TelegramChatEntity chat() {
        return new TelegramChatEntity(1L, "Ana", "ana", "private", false);
    }

    private static GameAnalysisEntity gameWithPhases(TelegramChatEntity chat, String... phaseSgfs) {
        AnalysisJobEntity job = new AnalysisJobEntity(chat, Instant.now(), UUID.randomUUID());
        GameAnalysisEntity game = new GameAnalysisEntity(job, chat);
        for (int i = 0; i + 1 < phaseSgfs.length; i += 2) {
            switch (phaseSgfs[i]) {
                case "opening" -> game.setOpeningAnalysed(phaseSgfs[i + 1]);
                case "middlegame" -> game.setMiddlegameAnalysed(phaseSgfs[i + 1]);
                case "endgame" -> game.setEndgameAnalysed(phaseSgfs[i + 1]);
                default -> throw new IllegalArgumentException(phaseSgfs[i]);
            }
        }
        return game;
    }

    /** A shared public-pool entity (ExerciseOrigin.PUBLIC, no owning chat) — same shape
     * SeedPublicDataset produces from dataset/games.json via GameAnalysisEntity.fromDataset. */
    private static GameAnalysisEntity publicGameWithPhases(String... phaseSgfs) {
        GameAnalysisEntity game = GameAnalysisEntity.fromDataset(new DatasetGameDto(null, null, null, null, null, null));
        for (int i = 0; i + 1 < phaseSgfs.length; i += 2) {
            switch (phaseSgfs[i]) {
                case "opening" -> game.setOpeningAnalysed(phaseSgfs[i + 1]);
                case "middlegame" -> game.setMiddlegameAnalysed(phaseSgfs[i + 1]);
                case "endgame" -> game.setEndgameAnalysed(phaseSgfs[i + 1]);
                default -> throw new IllegalArgumentException(phaseSgfs[i]);
            }
        }
        return game;
    }

    private static void setId(GameAnalysisEntity game, UUID id) {
        try {
            Field field = GameAnalysisEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(game, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void startTodaySession_onMissingChat_throwsNotFound() {
        when(chatRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> service.startTodaySession(99L));
    }

    @Test
    void startTodaySession_firstCall_createsExercisesOnlyFromBranchableSgfs() {
        TelegramChatEntity chat = chat();
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));
        when(sessionRepository.findByChatAndSessionDateAndKind(eq(chat), any(), eq("daily"))).thenReturn(Optional.empty());
        when(sessionRepository.save(any(SessionEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        GameAnalysisEntity game = gameWithPhases(chat, "opening", BRANCHING_SGF, "middlegame", NO_BRANCH_SGF);
        when(gameRepository.findByChat(chat)).thenReturn(List.of(game));
        when(exerciseRepository.findBySession_Chat(chat)).thenReturn(List.of());

        List<PracticeExerciseEntity> saved = new ArrayList<>();
        when(exerciseRepository.save(any(PracticeExerciseEntity.class))).thenAnswer(inv -> {
            PracticeExerciseEntity e = inv.getArgument(0);
            saved.add(e);
            return e;
        });
        when(exerciseRepository.findBySessionOrderByPositionOrder(any(SessionEntity.class))).thenAnswer(inv -> saved);

        SessionDto dto = service.startTodaySession(1L);

        assertEquals(1, dto.exercises().size());
        assertEquals("opening", dto.exercises().get(0).phase());
        assertEquals("personal", dto.exercises().get(0).origin());
        assertEquals(BRANCHING_SGF, dto.exercises().get(0).sgf());
        assertFalse(dto.completed());
    }

    @Test
    void startTodaySession_whenNoBranchableSgfExists_createsAnAlreadyCompletedEmptySession() {
        TelegramChatEntity chat = chat();
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));
        when(sessionRepository.findByChatAndSessionDateAndKind(eq(chat), any(), eq("daily"))).thenReturn(Optional.empty());
        when(sessionRepository.save(any(SessionEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        GameAnalysisEntity game = gameWithPhases(chat, "opening", NO_BRANCH_SGF);
        when(gameRepository.findByChat(chat)).thenReturn(List.of(game));
        when(exerciseRepository.findBySession_Chat(chat)).thenReturn(List.of());
        when(exerciseRepository.findBySessionOrderByPositionOrder(any(SessionEntity.class))).thenReturn(List.of());

        SessionDto dto = service.startTodaySession(1L);

        assertTrue(dto.exercises().isEmpty());
        assertTrue(dto.completed());
    }

    @Test
    void startTodaySession_isIdempotent_whenASessionAlreadyHasExercisesToday() {
        TelegramChatEntity chat = chat();
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        SessionEntity existing = new SessionEntity(chat, LocalDate.now(), "daily");
        GameAnalysisEntity game = gameWithPhases(chat, "opening", BRANCHING_SGF);
        PracticeExerciseEntity exercise = new PracticeExerciseEntity(existing, game, "opening", 1, 3, 4);
        when(sessionRepository.findByChatAndSessionDateAndKind(eq(chat), any(), eq("daily"))).thenReturn(Optional.of(existing));
        when(exerciseRepository.findBySessionOrderByPositionOrder(existing)).thenReturn(List.of(exercise));

        service.startTodaySession(1L);

        verify(gameRepository, never()).findByChat(any());
        verify(sessionRepository, never()).save(any());
    }

    /** Regression: a session row that already exists for today but was saved with zero
     * exercises (a transient pool issue at the time, or rows cleaned up by hand) must not stay
     * stuck empty for the rest of the day — see populateExercises' doc comment. */
    @Test
    void startTodaySession_whenExistingSessionHasNoExercises_retriesPopulatingItInstead() {
        TelegramChatEntity chat = chat();
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        SessionEntity existing = new SessionEntity(chat, LocalDate.now(), "daily");
        when(sessionRepository.findByChatAndSessionDateAndKind(eq(chat), any(), eq("daily"))).thenReturn(Optional.of(existing));

        GameAnalysisEntity game = gameWithPhases(chat, "opening", BRANCHING_SGF);
        when(gameRepository.findByChat(chat)).thenReturn(List.of(game));
        when(exerciseRepository.findBySession_Chat(chat)).thenReturn(List.of());

        List<PracticeExerciseEntity> saved = new ArrayList<>();
        when(exerciseRepository.save(any(PracticeExerciseEntity.class))).thenAnswer(inv -> {
            PracticeExerciseEntity e = inv.getArgument(0);
            saved.add(e);
            return e;
        });
        when(exerciseRepository.findBySessionOrderByPositionOrder(existing)).thenAnswer(inv -> saved);

        SessionDto dto = service.startTodaySession(1L);

        verify(gameRepository).findByChat(chat);
        assertEquals(1, dto.exercises().size());
        assertFalse(dto.completed());
    }

    @Test
    void startTodaySession_prefersNeverUsedPositions_whenMoreThanFiveCandidatesExist() {
        TelegramChatEntity chat = chat();
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));
        when(sessionRepository.findByChatAndSessionDateAndKind(eq(chat), any(), eq("daily"))).thenReturn(Optional.empty());
        when(sessionRepository.save(any(SessionEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        GameAnalysisEntity game1 = gameWithPhases(chat,
                "opening", BRANCHING_SGF, "middlegame", BRANCHING_SGF, "endgame", BRANCHING_SGF);
        GameAnalysisEntity game2 = gameWithPhases(chat,
                "opening", BRANCHING_SGF, "middlegame", BRANCHING_SGF, "endgame", BRANCHING_SGF);
        setId(game1, UUID.randomUUID());
        setId(game2, UUID.randomUUID());
        when(gameRepository.findByChat(chat)).thenReturn(List.of(game1, game2));

        // Used previously: game1/middlegame and game2/opening — 2 used, 4 unused, 6 candidates total.
        SessionEntity oldSession = new SessionEntity(chat, LocalDate.now().minusDays(1), "daily");
        PracticeExerciseEntity usedOne = new PracticeExerciseEntity(oldSession, game1, "middlegame", 1, 3, 4);
        PracticeExerciseEntity usedTwo = new PracticeExerciseEntity(oldSession, game2, "opening", 2, 3, 4);
        when(exerciseRepository.findBySession_Chat(chat)).thenReturn(List.of(usedOne, usedTwo));

        List<PracticeExerciseEntity> saved = new ArrayList<>();
        when(exerciseRepository.save(any(PracticeExerciseEntity.class))).thenAnswer(inv -> {
            PracticeExerciseEntity e = inv.getArgument(0);
            saved.add(e);
            return e;
        });
        when(exerciseRepository.findBySessionOrderByPositionOrder(any(SessionEntity.class))).thenAnswer(inv -> saved);

        service.startTodaySession(1L);

        assertEquals(5, saved.size());
        long usedPicked = saved.stream()
                .filter(e -> (e.getGame() == game1 && e.getPhase().equals("middlegame"))
                        || (e.getGame() == game2 && e.getPhase().equals("opening")))
                .count();
        // All 4 unused combinations must be present; only 1 of the 2 used ones fills the 5th slot.
        assertEquals(1, usedPicked);
    }

    @Test
    void startTodaySession_usesTheSavedExerciseCountPreference_insteadOfAFixedFive() {
        TelegramChatEntity chat = chat();
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));
        when(sessionRepository.findByChatAndSessionDateAndKind(eq(chat), any(), eq("daily"))).thenReturn(Optional.empty());
        when(sessionRepository.save(any(SessionEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(preferencesRepository.findById(1L)).thenReturn(Optional.of(new PracticePreferencesEntity(1L, 3, 4, 2, "own")));

        GameAnalysisEntity game = gameWithPhases(chat,
                "opening", BRANCHING_SGF, "middlegame", BRANCHING_SGF, "endgame", BRANCHING_SGF);
        when(gameRepository.findByChat(chat)).thenReturn(List.of(game));
        when(exerciseRepository.findBySession_Chat(chat)).thenReturn(List.of());

        List<PracticeExerciseEntity> saved = new ArrayList<>();
        when(exerciseRepository.save(any(PracticeExerciseEntity.class))).thenAnswer(inv -> {
            PracticeExerciseEntity e = inv.getArgument(0);
            saved.add(e);
            return e;
        });
        when(exerciseRepository.findBySessionOrderByPositionOrder(any(SessionEntity.class))).thenAnswer(inv -> saved);

        service.startTodaySession(1L);

        assertEquals(2, saved.size()); // capped at the saved preference, not the 3 branchable candidates available
    }

    @Test
    void recordResult_onExerciseNotOwnedByChat_throwsNotFound() {
        TelegramChatEntity chat = chat();
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));
        UUID exerciseId = UUID.randomUUID();
        when(exerciseRepository.findByIdAndSession_Chat(exerciseId, chat)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> service.recordResult(1L, exerciseId));
    }

    @Test
    void recordResult_whenItIsTheLastUnfinishedExercise_marksTheSessionCompleted() {
        TelegramChatEntity chat = chat();
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        SessionEntity session = new SessionEntity(chat, LocalDate.now(), "daily");
        GameAnalysisEntity game = gameWithPhases(chat, "opening", BRANCHING_SGF);
        PracticeExerciseEntity exercise = new PracticeExerciseEntity(session, game, "opening", 1, 3, 4);
        UUID exerciseId = UUID.randomUUID();
        when(exerciseRepository.findByIdAndSession_Chat(exerciseId, chat)).thenReturn(Optional.of(exercise));
        when(exerciseRepository.findBySessionOrderByPositionOrder(session)).thenReturn(List.of(exercise));

        service.recordResult(1L, exerciseId);

        assertTrue(exercise.isCompleted());
        assertTrue(session.isCompleted());
        verify(sessionRepository).save(session);
    }

    @Test
    void recordResult_whenOtherExercisesAreStillPending_leavesSessionIncomplete() {
        TelegramChatEntity chat = chat();
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        SessionEntity session = new SessionEntity(chat, LocalDate.now(), "daily");
        GameAnalysisEntity game = gameWithPhases(chat, "opening", BRANCHING_SGF, "middlegame", BRANCHING_SGF);
        PracticeExerciseEntity exercise = new PracticeExerciseEntity(session, game, "opening", 1, 3, 4);
        PracticeExerciseEntity pending = new PracticeExerciseEntity(session, game, "middlegame", 2, 3, 4);
        UUID exerciseId = UUID.randomUUID();
        when(exerciseRepository.findByIdAndSession_Chat(exerciseId, chat)).thenReturn(Optional.of(exercise));
        when(exerciseRepository.findBySessionOrderByPositionOrder(session)).thenReturn(List.of(exercise, pending));

        service.recordResult(1L, exerciseId);

        assertFalse(session.isCompleted());
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void getPreferences_withNoneSaved_returnsDefaults() {
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat()));
        when(preferencesRepository.findById(1L)).thenReturn(Optional.empty());

        PreferencesDto preferences = service.getPreferences(1L);

        assertEquals(3, preferences.topVariants());
        assertEquals(4, preferences.continuationDepth());
    }

    @Test
    void getPreferences_withSavedPreferences_returnsThem() {
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat()));
        when(preferencesRepository.findById(1L)).thenReturn(Optional.of(new PracticePreferencesEntity(1L, 5, 10, 7, "mixed")));

        PreferencesDto preferences = service.getPreferences(1L);

        assertEquals(5, preferences.topVariants());
        assertEquals(10, preferences.continuationDepth());
        assertEquals(7, preferences.exerciseCount());
        assertEquals("mixed", preferences.exerciseSource());
    }

    @Test
    void setPreferences_onOutOfRangeTopVariants_throwsBadRequest() {
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat()));

        assertThrows(ResponseStatusException.class, () -> service.setPreferences(1L, new PreferencesDto(6, 4, 5, "own")));
    }

    @Test
    void setPreferences_onOutOfRangeContinuationDepth_throwsBadRequest() {
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat()));

        assertThrows(ResponseStatusException.class, () -> service.setPreferences(1L, new PreferencesDto(3, 11, 5, "own")));
    }

    @Test
    void setPreferences_onOutOfRangeExerciseCount_throwsBadRequest() {
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat()));

        assertThrows(ResponseStatusException.class, () -> service.setPreferences(1L, new PreferencesDto(3, 4, 11, "own")));
    }

    @Test
    void setPreferences_onInvalidExerciseSource_throwsBadRequest() {
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat()));

        assertThrows(ResponseStatusException.class, () -> service.setPreferences(1L, new PreferencesDto(3, 4, 5, "someone_else")));
    }

    @Test
    void setPreferences_withValidValues_savesThem() {
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat()));
        when(preferencesRepository.findById(1L)).thenReturn(Optional.empty());

        service.setPreferences(1L, new PreferencesDto(2, 8, 7, "mixed"));

        verify(preferencesRepository).save(argThat(p ->
                p.getTopVariants() == 2 && p.getContinuationDepth() == 8
                        && p.getExerciseCount() == 7 && p.getExerciseSource().equals("mixed")));
    }

    @Test
    void saveProgress_onExerciseNotOwnedByChat_throwsNotFound() {
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat()));
        UUID exerciseId = UUID.randomUUID();
        when(exerciseRepository.findByIdAndSession_Chat(exerciseId, chat())).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> service.saveProgress(1L, exerciseId, new ProgressDto(1, 2, 6)));
    }

    @Test
    void saveProgress_withValidExercise_updatesSelectedVariantStepAndDepth() {
        TelegramChatEntity chat = chat();
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        SessionEntity session = new SessionEntity(chat, LocalDate.now(), "daily");
        GameAnalysisEntity game = gameWithPhases(chat, "opening", BRANCHING_SGF);
        PracticeExerciseEntity exercise = new PracticeExerciseEntity(session, game, "opening", 1, 3, 4);
        UUID exerciseId = UUID.randomUUID();
        when(exerciseRepository.findByIdAndSession_Chat(exerciseId, chat)).thenReturn(Optional.of(exercise));

        service.saveProgress(1L, exerciseId, new ProgressDto(1, 2, 6));

        assertEquals(1, exercise.getSelectedVariant());
        assertEquals(2, exercise.getStep());
        assertEquals(6, exercise.getCurrentDepth());
        verify(exerciseRepository).save(exercise);
    }

    @Test
    void getStreak_withNoSessionsAtAll_isZero() {
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat()));
        when(sessionRepository.findByChatAndKind(chat(), "daily")).thenReturn(List.of());

        StreakDto streak = service.getStreak(1L);

        assertEquals(0, streak.currentStreak());
        assertEquals(List.of(false, false, false, false, false, false, false), streak.last7Days());
    }

    @Test
    void getStreak_countsConsecutiveCompletedDaysEndingToday() {
        TelegramChatEntity chat = chat();
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        LocalDate today = LocalDate.now();
        SessionEntity todaySession = completedSessionWithOneExercise(chat, today);
        SessionEntity yesterdaySession = completedSessionWithOneExercise(chat, today.minusDays(1));
        SessionEntity twoDaysAgoSession = completedSessionWithOneExercise(chat, today.minusDays(2));
        when(sessionRepository.findByChatAndKind(chat, "daily")).thenReturn(List.of(todaySession, yesterdaySession, twoDaysAgoSession));

        StreakDto streak = service.getStreak(1L);

        assertEquals(3, streak.currentStreak());
        assertEquals(List.of(false, false, false, false, true, true, true), streak.last7Days());
    }

    @Test
    void getStreak_aDayWithNoExercisesAvailable_neitherCountsNorBreaksIt() {
        TelegramChatEntity chat = chat();
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        LocalDate today = LocalDate.now();
        SessionEntity todaySession = completedSessionWithOneExercise(chat, today);
        SessionEntity emptyYesterday = new SessionEntity(chat, today.minusDays(1), "daily"); // 0 exercises, auto-completed
        emptyYesterday.setCompleted(true);
        SessionEntity twoDaysAgoSession = completedSessionWithOneExercise(chat, today.minusDays(2));
        when(sessionRepository.findByChatAndKind(chat, "daily")).thenReturn(List.of(todaySession, emptyYesterday, twoDaysAgoSession));
        when(exerciseRepository.findBySessionOrderByPositionOrder(emptyYesterday)).thenReturn(List.of());

        StreakDto streak = service.getStreak(1L);

        assertEquals(2, streak.currentStreak());
    }

    @Test
    void getStreak_anUnfinishedOrMissingDay_breaksIt() {
        TelegramChatEntity chat = chat();
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        LocalDate today = LocalDate.now();
        SessionEntity todaySession = completedSessionWithOneExercise(chat, today);
        // No session at all two days ago — the backward walk stops there.
        when(sessionRepository.findByChatAndKind(chat, "daily")).thenReturn(List.of(todaySession));

        StreakDto streak = service.getStreak(1L);

        assertEquals(1, streak.currentStreak());
    }

    @Test
    void getStreak_whenTodaysPracticeIsntFinishedYet_stillCountsThroughYesterday() {
        TelegramChatEntity chat = chat();
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        LocalDate today = LocalDate.now();
        SessionEntity yesterdaySession = completedSessionWithOneExercise(chat, today.minusDays(1));
        SessionEntity todaySession = new SessionEntity(chat, today, "daily"); // started, not finished
        GameAnalysisEntity game = gameWithPhases(chat, "opening", BRANCHING_SGF);
        PracticeExerciseEntity pendingExercise = new PracticeExerciseEntity(todaySession, game, "opening", 1, 3, 4);
        when(exerciseRepository.findBySessionOrderByPositionOrder(todaySession)).thenReturn(List.of(pendingExercise));
        when(sessionRepository.findByChatAndKind(chat, "daily")).thenReturn(List.of(yesterdaySession, todaySession));

        StreakDto streak = service.getStreak(1L);

        // Handoff v5-1 section 3: "Until today's practice is finished, it counts through
        // yesterday" — today being in progress must not zero out a streak already earned.
        assertEquals(1, streak.currentStreak());
        assertFalse(streak.last7Days().get(6)); // today itself still reads as not finished
    }

    @Test
    void getStreak_whenTodayHasNoSessionAtAll_stillCountsThroughYesterday() {
        TelegramChatEntity chat = chat();
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        LocalDate today = LocalDate.now();
        SessionEntity yesterdaySession = completedSessionWithOneExercise(chat, today.minusDays(1));
        when(sessionRepository.findByChatAndKind(chat, "daily")).thenReturn(List.of(yesterdaySession));

        StreakDto streak = service.getStreak(1L);

        assertEquals(1, streak.currentStreak());
    }

    @Test
    void startNewRound_neverTouchesTheDailySessionOrTheStreak_alwaysCreatesANewGeneratedSession() {
        TelegramChatEntity chat = chat();
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));
        when(sessionRepository.save(any(SessionEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        GameAnalysisEntity game = gameWithPhases(chat, "opening", BRANCHING_SGF);
        when(gameRepository.findByChat(chat)).thenReturn(List.of(game));
        when(exerciseRepository.findBySession_Chat(chat)).thenReturn(List.of());
        when(exerciseRepository.save(any(PracticeExerciseEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(exerciseRepository.findBySessionOrderByPositionOrder(any(SessionEntity.class)))
                .thenReturn(List.of(new PracticeExerciseEntity(null, game, "opening", 1, 3, 4)));

        SessionDto dto = service.startNewRound(1L);

        assertEquals("generated", dto.kind());
        verify(sessionRepository, never()).findByChatAndSessionDateAndKind(any(), any(), any());
    }

    @Test
    void startTodaySession_ownExerciseSource_neverReadsThePublicPool() {
        TelegramChatEntity chat = chat();
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));
        when(sessionRepository.findByChatAndSessionDateAndKind(eq(chat), any(), eq("daily"))).thenReturn(Optional.empty());
        when(sessionRepository.save(any(SessionEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        GameAnalysisEntity game = gameWithPhases(chat, "opening", BRANCHING_SGF);
        when(gameRepository.findByChat(chat)).thenReturn(List.of(game));
        when(exerciseRepository.findBySession_Chat(chat)).thenReturn(List.of());
        when(exerciseRepository.save(any(PracticeExerciseEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(exerciseRepository.findBySessionOrderByPositionOrder(any(SessionEntity.class)))
                .thenReturn(List.of(new PracticeExerciseEntity(null, game, "opening", 1, 3, 4)));

        service.startTodaySession(1L);

        verify(gameRepository, never()).findByOrigin(any());
    }

    @Test
    void startTodaySession_mixedExerciseSource_padsOwnGamesWithThePublicPool() {
        TelegramChatEntity chat = chat();
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));
        when(sessionRepository.findByChatAndSessionDateAndKind(eq(chat), any(), eq("daily"))).thenReturn(Optional.empty());
        when(sessionRepository.save(any(SessionEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(preferencesRepository.findById(1L)).thenReturn(Optional.of(new PracticePreferencesEntity(1L, 3, 4, 5, "mixed")));

        GameAnalysisEntity ownGame = gameWithPhases(chat, "opening", BRANCHING_SGF);
        GameAnalysisEntity publicGame = publicGameWithPhases("opening", BRANCHING_SGF);
        when(gameRepository.findByChat(chat)).thenReturn(List.of(ownGame));
        when(gameRepository.findByOrigin(ExerciseOrigin.PUBLIC)).thenReturn(List.of(publicGame));
        when(exerciseRepository.findBySession_Chat(chat)).thenReturn(List.of());

        List<PracticeExerciseEntity> saved = new ArrayList<>();
        when(exerciseRepository.save(any(PracticeExerciseEntity.class))).thenAnswer(inv -> {
            PracticeExerciseEntity e = inv.getArgument(0);
            saved.add(e);
            return e;
        });
        when(exerciseRepository.findBySessionOrderByPositionOrder(any(SessionEntity.class))).thenAnswer(inv -> saved);

        SessionDto dto = service.startTodaySession(1L);

        // Own games always come first — the public pool only fills out what's left.
        assertEquals(2, dto.exercises().size());
        assertEquals("personal", dto.exercises().get(0).origin());
        assertEquals("dataset", dto.exercises().get(1).origin());
    }

    @Test
    void startRandomPractice_buildsFromThePublicPoolAlone_ignoringTheChatsOwnGamesAndPreference() {
        TelegramChatEntity chat = chat();
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));
        when(sessionRepository.save(any(SessionEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(preferencesRepository.findById(1L)).thenReturn(Optional.of(new PracticePreferencesEntity(1L, 3, 4, 5, "own")));

        GameAnalysisEntity publicGame = publicGameWithPhases("opening", BRANCHING_SGF);
        when(gameRepository.findByOrigin(ExerciseOrigin.PUBLIC)).thenReturn(List.of(publicGame));
        when(exerciseRepository.findBySession_Chat(chat)).thenReturn(List.of());
        when(exerciseRepository.save(any(PracticeExerciseEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(exerciseRepository.findBySessionOrderByPositionOrder(any(SessionEntity.class)))
                .thenReturn(List.of(new PracticeExerciseEntity(null, publicGame, "opening", 1, 3, 4)));

        SessionDto dto = service.startRandomPractice(1L, 3);

        assertEquals("generated", dto.kind());
        assertEquals("dataset", dto.exercises().get(0).origin());
        verify(gameRepository, never()).findByChat(any());
    }

    @Test
    void startRandomPractice_capsAtTheRequestedCount_notTheExerciseCountPreference() {
        TelegramChatEntity chat = chat();
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));
        when(sessionRepository.save(any(SessionEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(preferencesRepository.findById(1L)).thenReturn(Optional.of(new PracticePreferencesEntity(1L, 3, 4, 2, "own")));

        GameAnalysisEntity publicGame = publicGameWithPhases(
                "opening", BRANCHING_SGF, "middlegame", BRANCHING_SGF, "endgame", BRANCHING_SGF);
        when(gameRepository.findByOrigin(ExerciseOrigin.PUBLIC)).thenReturn(List.of(publicGame));
        when(exerciseRepository.findBySession_Chat(chat)).thenReturn(List.of());

        List<PracticeExerciseEntity> saved = new ArrayList<>();
        when(exerciseRepository.save(any(PracticeExerciseEntity.class))).thenAnswer(inv -> {
            PracticeExerciseEntity e = inv.getArgument(0);
            saved.add(e);
            return e;
        });
        when(exerciseRepository.findBySessionOrderByPositionOrder(any(SessionEntity.class))).thenAnswer(inv -> saved);

        service.startRandomPractice(1L, 3);

        assertEquals(3, saved.size()); // the dialog's own count (3), not the exerciseCount preference (2)
    }

    @Test
    void startRandomPractice_onOutOfRangeCount_throwsBadRequest() {
        assertThrows(ResponseStatusException.class, () -> service.startRandomPractice(1L, 0));
        assertThrows(ResponseStatusException.class, () -> service.startRandomPractice(1L, 11));
    }

    @Test
    void listSessions_returnsDailyAndGeneratedSessionsTogether() {
        TelegramChatEntity chat = chat();
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        SessionEntity daily = new SessionEntity(chat, LocalDate.now(), "daily");
        SessionEntity generated = new SessionEntity(chat, LocalDate.now(), "generated");
        when(sessionRepository.findByChat(chat)).thenReturn(List.of(daily, generated));
        when(exerciseRepository.findBySessionOrderByPositionOrder(any(SessionEntity.class))).thenReturn(List.of());

        List<SessionDto> sessions = service.listSessions(1L);

        assertEquals(2, sessions.size());
        assertEquals(1, sessions.stream().filter(s -> s.kind().equals("daily")).count());
        assertEquals(1, sessions.stream().filter(s -> s.kind().equals("generated")).count());
    }

    @Test
    void getSession_onSessionNotOwnedByChat_isEmpty() {
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat()));
        UUID sessionId = UUID.randomUUID();
        when(sessionRepository.findByIdAndChat(sessionId, chat())).thenReturn(Optional.empty());

        assertTrue(service.getSession(1L, sessionId).isEmpty());
    }

    /** Regression: one exercise whose SGF no longer parses (e.g. the underlying game analysis
     * was edited/cleared after this exercise was persisted) used to make toSessionDto throw,
     * taking the WHOLE session down — every endpoint returning a SessionDto would 500 for this
     * chat. It must instead just drop that one exercise and return the rest. */
    @Test
    void getSession_dropsAnExerciseWithUnreadableSgf_insteadOfFailingTheWholeSession() {
        TelegramChatEntity chat = chat();
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));
        UUID sessionId = UUID.randomUUID();
        SessionEntity session = new SessionEntity(chat, LocalDate.now(), "daily");
        when(sessionRepository.findByIdAndChat(sessionId, chat)).thenReturn(Optional.of(session));

        GameAnalysisEntity healthyGame = gameWithPhases(chat, "opening", BRANCHING_SGF);
        PracticeExerciseEntity healthy = new PracticeExerciseEntity(session, healthyGame, "opening", 1, 3, 4);
        GameAnalysisEntity brokenGame = gameWithPhases(chat, "opening", NO_BRANCH_SGF);
        PracticeExerciseEntity broken = new PracticeExerciseEntity(session, brokenGame, "opening", 2, 3, 4);
        when(exerciseRepository.findBySessionOrderByPositionOrder(session)).thenReturn(List.of(healthy, broken));

        SessionDto dto = service.getSession(1L, sessionId).orElseThrow();

        assertEquals(1, dto.exercises().size());
        assertEquals(healthy.getId(), dto.exercises().get(0).id());
    }

    private SessionEntity completedSessionWithOneExercise(TelegramChatEntity chat, LocalDate date) {
        SessionEntity session = new SessionEntity(chat, date, "daily");
        session.setCompleted(true);
        GameAnalysisEntity game = gameWithPhases(chat, "opening", BRANCHING_SGF);
        PracticeExerciseEntity exercise = new PracticeExerciseEntity(session, game, "opening", 1, 3, 4);
        exercise.setCompleted(true);
        when(exerciseRepository.findBySessionOrderByPositionOrder(session)).thenReturn(List.of(exercise));
        return session;
    }
}

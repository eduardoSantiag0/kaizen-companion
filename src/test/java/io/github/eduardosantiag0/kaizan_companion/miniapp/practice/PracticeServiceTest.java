package io.github.eduardosantiag0.kaizan_companion.miniapp.practice;

import io.github.eduardosantiag0.kaizan_companion.domain.entities.AnalysisJobEntity;
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
        when(sessionRepository.findByChatAndSessionDate(eq(chat), any())).thenReturn(Optional.empty());
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
        assertFalse(dto.completed());
    }

    @Test
    void startTodaySession_whenNoBranchableSgfExists_createsAnAlreadyCompletedEmptySession() {
        TelegramChatEntity chat = chat();
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));
        when(sessionRepository.findByChatAndSessionDate(eq(chat), any())).thenReturn(Optional.empty());
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
    void startTodaySession_isIdempotent_whenASessionAlreadyExistsToday() {
        TelegramChatEntity chat = chat();
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        SessionEntity existing = new SessionEntity(chat, LocalDate.now());
        when(sessionRepository.findByChatAndSessionDate(eq(chat), any())).thenReturn(Optional.of(existing));
        when(exerciseRepository.findBySessionOrderByPositionOrder(existing)).thenReturn(List.of());

        service.startTodaySession(1L);

        verify(gameRepository, never()).findByChat(any());
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void startTodaySession_prefersNeverUsedPositions_whenMoreThanFiveCandidatesExist() {
        TelegramChatEntity chat = chat();
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));
        when(sessionRepository.findByChatAndSessionDate(eq(chat), any())).thenReturn(Optional.empty());
        when(sessionRepository.save(any(SessionEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        GameAnalysisEntity game1 = gameWithPhases(chat,
                "opening", BRANCHING_SGF, "middlegame", BRANCHING_SGF, "endgame", BRANCHING_SGF);
        GameAnalysisEntity game2 = gameWithPhases(chat,
                "opening", BRANCHING_SGF, "middlegame", BRANCHING_SGF, "endgame", BRANCHING_SGF);
        setId(game1, UUID.randomUUID());
        setId(game2, UUID.randomUUID());
        when(gameRepository.findByChat(chat)).thenReturn(List.of(game1, game2));

        // Used previously: game1/middlegame and game2/opening — 2 used, 4 unused, 6 candidates total.
        SessionEntity oldSession = new SessionEntity(chat, LocalDate.now().minusDays(1));
        PracticeExerciseEntity usedOne = new PracticeExerciseEntity(oldSession, game1, "middlegame", 1);
        PracticeExerciseEntity usedTwo = new PracticeExerciseEntity(oldSession, game2, "opening", 2);
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

        SessionEntity session = new SessionEntity(chat, LocalDate.now());
        GameAnalysisEntity game = gameWithPhases(chat, "opening", BRANCHING_SGF);
        PracticeExerciseEntity exercise = new PracticeExerciseEntity(session, game, "opening", 1);
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

        SessionEntity session = new SessionEntity(chat, LocalDate.now());
        GameAnalysisEntity game = gameWithPhases(chat, "opening", BRANCHING_SGF, "middlegame", BRANCHING_SGF);
        PracticeExerciseEntity exercise = new PracticeExerciseEntity(session, game, "opening", 1);
        PracticeExerciseEntity pending = new PracticeExerciseEntity(session, game, "middlegame", 2);
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
        when(preferencesRepository.findById(1L)).thenReturn(Optional.of(new PracticePreferencesEntity(1L, 5, 10)));

        PreferencesDto preferences = service.getPreferences(1L);

        assertEquals(5, preferences.topVariants());
        assertEquals(10, preferences.continuationDepth());
    }

    @Test
    void setPreferences_onOutOfRangeTopVariants_throwsBadRequest() {
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat()));

        assertThrows(ResponseStatusException.class, () -> service.setPreferences(1L, new PreferencesDto(6, 4)));
    }

    @Test
    void setPreferences_onOutOfRangeContinuationDepth_throwsBadRequest() {
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat()));

        assertThrows(ResponseStatusException.class, () -> service.setPreferences(1L, new PreferencesDto(3, 11)));
    }

    @Test
    void setPreferences_withValidValues_savesThem() {
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat()));
        when(preferencesRepository.findById(1L)).thenReturn(Optional.empty());

        service.setPreferences(1L, new PreferencesDto(2, 8));

        verify(preferencesRepository).save(argThat(p -> p.getTopVariants() == 2 && p.getContinuationDepth() == 8));
    }
}

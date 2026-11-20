package io.github.eduardosantiag0.kaizan_companion.features.analysis;

import io.github.eduardosantiag0.kaizan_companion.domain.entities.*;
import io.github.eduardosantiag0.kaizan_companion.domain.repositories.AnalysisJobRepository;
import io.github.eduardosantiag0.kaizan_companion.domain.repositories.GameAnalysisRepository;
import io.github.eduardosantiag0.kaizan_companion.domain.repositories.TelegramChatRepository;
import io.github.eduardosantiag0.kaizan_companion.domain.repositories.UserInfoRepository;
import io.github.eduardosantiag0.kaizan_companion.features.analysis.dtos.GamePhase;
import io.github.eduardosantiag0.kaizan_companion.features.analysis.katago.KatagoAssembler;
import io.github.eduardosantiag0.kaizan_companion.features.analysis.dtos.runpod.RunPodResponse;
import io.github.eduardosantiag0.kaizan_companion.features.notifications.messaging.EmailNotificationEvent;
import io.github.eduardosantiag0.kaizan_companion.features.notifications.messaging.EmailNotificationProducer;
import io.github.eduardosantiag0.kaizan_companion.features.storage.messaging.DriveStorageEvent;
import io.github.eduardosantiag0.kaizan_companion.features.storage.messaging.DriveStorageProducer;
import io.github.eduardosantiag0.kaizan_companion.utils.FilenamePattern;
import io.github.eduardosantiag0.kaizan_companion.utils.MessageFormatter;
import io.github.eduardosantiag0.kaizan_companion.features.notifications.NotificationService;
import io.github.eduardosantiag0.kaizan_companion.features.notifications.replies.ReplyWithFile;
import io.github.eduardosantiag0.kaizan_companion.features.notifications.replies.ReplyWithText;
import io.github.eduardosantiag0.kaizan_companion.infra.IStorageProvider;
import io.github.eduardosantiag0.sgf.model.SgfNode;
import io.github.eduardosantiag0.sgf.parser.SgfParser;
import jakarta.mail.MessagingException;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


@Service
public class AnalysisAggregator {
    private static final Logger log = LoggerFactory.getLogger(AnalysisAggregator.class);

    private final NotificationService notificationService;
    private final TelegramChatRepository chatRepository;
    private final AnalysisJobRepository jobRepository;
    private final KatagoAssembler assembler;
    private final IStorageProvider storageProvider;
    private final GameAnalysisRepository gameAnalysisRepository;
    private final UserInfoRepository userInfoRepository;
    private final EmailNotificationProducer emailNotificationProducer;
    private final DriveStorageProducer driveStorageProducer;

    public AnalysisAggregator(NotificationService notificationService, TelegramChatRepository chatRepository, AnalysisJobRepository jobRepository, KatagoAssembler assembler, IStorageProvider storageProvider, GameAnalysisRepository gameAnalysisRepository, UserInfoRepository userInfoRepository, EmailNotificationProducer emailNotificationProducer, DriveStorageProducer driveStorageProducer) {
        this.notificationService = notificationService;
        this.chatRepository = chatRepository;
        this.jobRepository = jobRepository;
        this.assembler = assembler;
        this.storageProvider = storageProvider;
        this.gameAnalysisRepository = gameAnalysisRepository;
        this.userInfoRepository = userInfoRepository;
        this.emailNotificationProducer = emailNotificationProducer;
        this.driveStorageProducer = driveStorageProducer;
    }

    public void handleResponse(RunPodResponse response) throws IOException, MessagingException {

        AnalysisJobEntity jobEntity = jobRepository.findByCorrelationId(response.output().correlationId())
                .orElseThrow(() -> new RuntimeException("Job  not found"));

        if (gameAnalysisRepository.existsByJobEntity(jobEntity)) {
            log.warn("Callback duplicado ignorado para o job {}", jobEntity.getId());
            return;
        }

        TelegramChatEntity chatEntity = chatRepository.findById(response.output().chatId())
                .orElseThrow(() -> new RuntimeException("Chat not found"));

        if (response.output().output() == null) {
            // Contrato do runpod-handler: falha de processamento = status FAILED,
            // output null. Sem essa checagem, a linha abaixo que lê
            // output().criticalPoints() dá NPE pra todo job que falha no RunPod.
            jobEntity.setJobStatus(JobStatus.FAILED);
            jobRepository.save(jobEntity);
            chatEntity.setWaitingForAnalysis(false);
            log.error("KataGo retornou falha para o job {} (correlationId={}, status={})",
                    jobEntity.getId(), response.output().correlationId(), response.output().status());
            notificationService.sendErrorMessage(
                    new ReplyWithText(response.output().chatId(), MessageFormatter.requestFailed()));
            return;
        }

        GameAnalysisEntity gameAnalysisEntity = new GameAnalysisEntity(jobEntity, chatEntity);
        setPlayersInfo(gameAnalysisEntity, jobEntity.getOriginalGame());

        String completionMessage;
        try {
            completionMessage = MessageFormatter.requestCompleted(
                    jobEntity.getOriginalGame().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            completionMessage = MessageFormatter.requestCompleted("Your game");
        }
        notificationService.sendMessageToChat(
                new ReplyWithText(response.output().chatId(), completionMessage));

        List<Future<?>> futures = new ArrayList<>();
        String[] sgfs = new String[3];
        byte[][] sgfsInBytes = new byte[3][];

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            response.output().output().criticalPoints()
                    .forEach(point -> futures.add(
                            executor.submit(() -> {
                String node = assembler.attachAnalysisToGame(point, jobEntity.getOriginalGame());
                byte[] data = node.getBytes(StandardCharsets.UTF_8);

                switch (point.phase()) {
                    case GamePhase.opening -> {
                        sgfsInBytes[0] = data;
                        sgfs[0] = node;
                        gameAnalysisEntity.setOpeningAnalysed(node);
                    }
                    case GamePhase.middlegame -> {
                        sgfsInBytes[1] = data;
                        sgfs[1] = node;
                        gameAnalysisEntity.setMiddlegameAnalysed(node);
                    }
                    case GamePhase.endgame -> {
                        sgfsInBytes[2] = data;
                        sgfs[2] = node;
                        gameAnalysisEntity.setEndgameAnalysed(node);
                    }
                }

                // Salvar e notificar com o ByteArrayResource
                UUID uuid = UUID.randomUUID();
                storageProvider.store(data, point.phase(), uuid);
                notificationService.sendMessageToChat(
                        new ReplyWithFile(response.output().chatId(), data,
                                "",
                                point.phase(), uuid
                        )
                );
            })));
        }

        for (Future<?> future : futures) {
            try {
                future.get(); // Relança o erro caso houve algum
            } catch (ExecutionException e) {
                throw new RuntimeException("Not possible to save your game", e.getCause());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Not possible to save your game", e);
            }
        }


//        gameAnalysisEntity.setFullAnalysedGame(String.valueOf(assembler.assebleAllPhases(gamePhases)));

        gameAnalysisRepository.save(gameAnalysisEntity);

        jobEntity.setJobStatus(JobStatus.COMPLETED);

        chatEntity.setWaitingForAnalysis(false);

        if (sgfsInBytes[0] == null && sgfsInBytes[1] == null && sgfsInBytes[2] == null)
            return;

        dispatchExternalDeliveries(chatEntity, sgfsInBytes);
    }

    private void setPlayersInfo(GameAnalysisEntity gameAnalysisEntity, String originalGame) {
        if (originalGame == null) {
            return;
        }

        SgfNode root = new SgfParser().parse(originalGame).firstGame().root();

        String blackPlayer = root.value("PB").orElse(null);
        String whitePlayer = root.value("PW").orElse(null);
        String blackRank = root.value("BR").orElse(null);
        String whiteRank = root.value("WR").orElse(null);

        gameAnalysisEntity.setBlackPlayer(blackPlayer);
        gameAnalysisEntity.setWhitePlayer(whitePlayer);

        if (blackRank != null && whiteRank != null) {
            gameAnalysisEntity.setGameRank(blackRank + "/" + whiteRank);
        } else if (blackRank != null) {
            gameAnalysisEntity.setGameRank(blackRank);
        } else if (whiteRank != null) {
            gameAnalysisEntity.setGameRank(whiteRank);
        }
    }

    private void dispatchExternalDeliveries(TelegramChatEntity chatEntity,
                                            byte[][] sgfsInBytes) {
        Optional<UserInfoEntity> optionalUser = userInfoRepository.findByChat(chatEntity);

        if (optionalUser.isPresent() && optionalUser.get().getEmailAddress() != null)
        {
            String emailAddress = optionalUser.get().getEmailAddress();
            emailNotificationProducer.send(
                    new EmailNotificationEvent(
                            emailAddress,
                            sgfsInBytes,
                            FilenamePattern.getPlayersName(sgfsInBytes[0]),
                            chatEntity.getUsername()));
        }

        if (chatEntity.isDriveLinked()) {
            driveStorageProducer.send(new DriveStorageEvent(chatEntity.getChatId(), sgfsInBytes,
                    FilenamePattern.getPlayersName(sgfsInBytes[0]), chatEntity.getUsername()));
        }
    }
}

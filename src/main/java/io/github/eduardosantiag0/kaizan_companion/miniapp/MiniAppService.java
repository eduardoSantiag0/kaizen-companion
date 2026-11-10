package io.github.eduardosantiag0.kaizan_companion.miniapp;

import io.github.eduardosantiag0.kaizan_companion.domain.repositories.GameAnalysisRepository;
import io.github.eduardosantiag0.kaizan_companion.domain.repositories.TelegramChatRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MiniAppService {

    private final GameAnalysisRepository gameRepository;
    private final TelegramChatRepository telegramChatRepository;

    public MiniAppService(GameAnalysisRepository gameRepository, TelegramChatRepository telegramChatRepository) {
        this.gameRepository = gameRepository;
        this.telegramChatRepository = telegramChatRepository;
    }

    public List<GameHistoryDto> findAllByTelegramUserId(Long telegramUserId) {

        var chatEntity = telegramChatRepository.findById(telegramUserId).orElseThrow();

        return gameRepository.findHistoryByChat(chatEntity);

    }
}

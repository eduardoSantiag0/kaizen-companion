package io.github.eduardosantiag0.kaizan_companion.miniapp;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/kaizan/api/v1/katago")
public class MiniAppController {

    private final MiniAppService service;
    private final TelegramInitDataService telegramInitDataService;

    public MiniAppController(MiniAppService service, TelegramInitDataService telegramInitDataService) {
        this.service = service;
        this.telegramInitDataService = telegramInitDataService;
    }

    // Get all past games
    // Get one game

    @GetMapping("/history")
    public ResponseEntity<List<GameHistoryDto>> getAllGames(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorization
    ) {
        if (authorization == null ||
                !authorization.startsWith("tma ")) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED
            );
        }


        String initData = authorization.substring(4);

//        https://core.telegram.org/bots/webapps?utm_source=chatgpt.com#validating-data-received-via-the-mini-app

        Long telegramUserId =
                telegramInitDataService.validateAndGetUserId(initData);

        List<GameHistoryDto> games =
                service.findAllByTelegramUserId(telegramUserId);

        return ResponseEntity.ok(games);
    }



}

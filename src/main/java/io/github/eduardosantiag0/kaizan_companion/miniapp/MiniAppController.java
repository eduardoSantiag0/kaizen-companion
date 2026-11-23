package io.github.eduardosantiag0.kaizan_companion.miniapp;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

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
    public ResponseEntity<List<GameHistoryItemDto>> getAllGames(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorization
    ) {

        // Recebe um HashMap<String, String>
        if (authorization == null ||
                !authorization.startsWith("tma ")) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED
            );
        }



        String initData = authorization.substring(4);

        Long telegramUserId =
                telegramInitDataService.validateAndGetUserId(initData);

        List<GameHistoryItemDto> games =
                service.findAllByTelegramUserId(telegramUserId);

        return ResponseEntity.ok(games);
    }

    @DeleteMapping("/history/{id}")
    public ResponseEntity<Void> resignGame(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorization,
            @PathVariable UUID id
    ) {

        if (authorization == null ||
                !authorization.startsWith("tma ")) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED
            );
        }

        String initData = authorization.substring(4);

        Long telegramUserId =
                telegramInitDataService.validateAndGetUserId(initData);

        service.resignGame(telegramUserId, id);

        return ResponseEntity.noContent().build();
    }

}

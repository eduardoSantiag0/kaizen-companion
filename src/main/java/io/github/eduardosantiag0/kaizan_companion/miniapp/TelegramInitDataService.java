package io.github.eduardosantiag0.kaizan_companion.miniapp;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class TelegramInitDataService {

    @Value("${telegram.bot.token}")
    private String botToken;

    private final HmacService hmacService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TelegramInitDataService(HmacService hmacService) {
        this.hmacService = hmacService;
    }


    public Long validateAndGetUserId(String initData) {

        Map<String, String> decodedData = new TreeMap<>();

        // Separa cada parâmetro pelo '&'
        // é necessário porque o data check é baseado no na string pura
        for (String item : initData.split("&")) {
            String[] par = item.split("=", 2);
            // Colocar o par no hashmap
            if (par.length == 2) {
                String chave = URLDecoder.decode(par[0], StandardCharsets.UTF_8);
                String valor = URLDecoder.decode(par[1], StandardCharsets.UTF_8);
                decodedData.put(chave, valor);
            }
        }

        // Remove o hash e recria a string
        String receivedHash = decodedData.remove("hash");
        String dataCheckString = decodedData.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("\n"));


        if (!hmacService.isValidTelegramWebAppData(dataCheckString, receivedHash)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Assinatura inválida");
        }

        String userJson = decodedData.get("user");
        if (userJson == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Dados do usuário ausentes");
        }

        JsonNode userNode = objectMapper.readTree(userJson);
        JsonNode idNode = userNode.get("id");
        if (idNode == null || !idNode.isNumber()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "ID do usuário inválido");
        }
        return idNode.asLong();


    }
}

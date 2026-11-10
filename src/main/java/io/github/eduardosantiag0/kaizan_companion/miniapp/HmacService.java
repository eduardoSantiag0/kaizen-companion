package io.github.eduardosantiag0.kaizan_companion.miniapp;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class HmacService {

    private static final String ALGORITHM = "HmacSHA256";
    private static final String CONSTANT_KEY = "WebAppData";

    private final String BOT_TOKEN;

    public HmacService(
            @Value("${telegram.bot.token}")
            String botToken) {
        BOT_TOKEN = botToken;
    }

    public boolean isValidTelegramWebAppData(String dataCheckString, String expectedHash) {
        try {
            byte[] constantKeyBytes = CONSTANT_KEY.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec constantKeySpec = new SecretKeySpec(constantKeyBytes, ALGORITHM);

            Mac mac1 = Mac.getInstance(ALGORITHM);
            mac1.init(constantKeySpec);
            byte[] secretKey = mac1.doFinal(BOT_TOKEN.getBytes(StandardCharsets.UTF_8));

            SecretKeySpec finalKeySpec = new SecretKeySpec(secretKey, ALGORITHM);
            Mac mac2 = Mac.getInstance(ALGORITHM);
            mac2.init(finalKeySpec);
            byte[] calculatedHashBytes = mac2.doFinal(dataCheckString.getBytes(StandardCharsets.UTF_8));

            String calculatedHash = HexFormat.of().formatHex(calculatedHashBytes);

            return calculatedHash.equalsIgnoreCase(expectedHash);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Erro ao validar credenciais do Telegram", e);
        }
    }

    public String calculateHmac(String data, String secretKey) {
        try {
            Mac sha256Hmac = Mac.getInstance(ALGORITHM);

            // 2. Transforma a chave secreta em um objeto SecretKeySpec
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8),
                    ALGORITHM
            );
            sha256Hmac.init(secretKeySpec);

            byte[] rawHmac = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));


            return HexFormat.of().formatHex(rawHmac);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Erro ao calcular HMAC-SHA256", e);
        }
    }

    public boolean verifyHmac(String data, String secretKey, String expectedHmac) {
        String calculatedHmac = calculateHmac(data, secretKey);
        return calculatedHmac.equalsIgnoreCase(expectedHmac);
    }
}

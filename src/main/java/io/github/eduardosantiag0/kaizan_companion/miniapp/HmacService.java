package io.github.eduardosantiag0.kaizan_companion.miniapp;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

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
            // Secret key = Bot token + "WebAppData"

            SecretKeySpec finalKeySpec = new SecretKeySpec(secretKey, ALGORITHM);
            Mac mac2 = Mac.getInstance(ALGORITHM);
            mac2.init(finalKeySpec);
            byte[] calculatedHashBytes = mac2.doFinal(dataCheckString.getBytes(StandardCharsets.UTF_8));
            // Hash do DataCheckString que veio + secret key ()

            String calculatedHash = HexFormat.of().formatHex(calculatedHashBytes);

            // Calcula se é o mesmo
            return constantTimeEquals(calculatedHash, expectedHash);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Erro ao validar credenciais do Telegram", e);
        }
    }

    private static boolean constantTimeEquals(String calculated, String expected) {
        return MessageDigest.isEqual(
                calculated.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8),
                expected.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8)
        );
    }
}

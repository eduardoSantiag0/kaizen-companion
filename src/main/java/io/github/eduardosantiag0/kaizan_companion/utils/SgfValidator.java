package io.github.eduardosantiag0.kaizan_companion.utils;

import lombok.experimental.UtilityClass;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

@UtilityClass
public class SgfValidator {

    private static final long MAX_FILE_SIZE_BYTES = 512 * 1024;

    private static final Pattern SGF_STRUCTURE_PATTERN =
            Pattern.compile("^\\(\\s*;.*\\)$", Pattern.DOTALL);

    public static void validateMetadata(String fileName, Long fileSize) {

        if (fileName == null ||
                !fileName.toLowerCase(Locale.ROOT).endsWith(".sgf")) {
            throw new IllegalArgumentException(
                    "Apenas arquivos com extensão .sgf são permitidos."
            );
        }

        if (fileSize == null || fileSize <= 0 ||
                fileSize > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException(
                    "Tamanho de arquivo inválido. O limite é de 512 KB."
            );
        }
    }

    public static boolean validate(byte[] fileData) {

        if (fileData == null || fileData.length == 0 ||
                fileData.length > MAX_FILE_SIZE_BYTES) {
            return false;
        }

        for (byte value : fileData) {
            if (value == 0) {
                return false;
            }
        }

        CharsetDecoder decoder = StandardCharsets.UTF_8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);

        try {
            String content = decoder
                    .decode(ByteBuffer.wrap(fileData))
                    .toString()
                    .strip();

            // Remove o BOM UTF-8, caso exista
            if (content.startsWith("\uFEFF")) {
                content = content.substring(1).strip();
            }

            return SGF_STRUCTURE_PATTERN
                    .matcher(content)
                    .matches();

        } catch (CharacterCodingException e) {
            return false;
        }
    }
}

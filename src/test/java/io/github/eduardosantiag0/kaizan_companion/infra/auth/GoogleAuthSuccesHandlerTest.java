package io.github.eduardosantiag0.kaizan_companion.infra.auth;

import io.github.eduardosantiag0.kaizan_companion.domain.entities.OAuthConnectionEntity;
import io.github.eduardosantiag0.kaizan_companion.domain.entities.TelegramChatEntity;
import io.github.eduardosantiag0.kaizan_companion.domain.repositories.OAuthConnectionRepository;
import io.github.eduardosantiag0.kaizan_companion.domain.repositories.TelegramChatRepository;
import io.github.eduardosantiag0.kaizan_companion.infra.security.services.EncryptionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GoogleAuthSuccesHandlerTest {

    @Test
    void onAuthenticationSuccess_mustSendA400_insteadOfThrowing_whenSessionHasNoChatId() throws Exception {
        TelegramChatRepository chatRepository = mock(TelegramChatRepository.class);
        OAuthConnectionRepository connectionRepository = mock(OAuthConnectionRepository.class);
        OAuth2AuthorizedClientService clientService = mock(OAuth2AuthorizedClientService.class);
        EncryptionService encryptionService = mock(EncryptionService.class);

        GoogleAuthSuccesHandler handler = new GoogleAuthSuccesHandler(
                chatRepository, connectionRepository, clientService, encryptionService);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        Authentication authentication = mock(Authentication.class);

        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("telegramChatId")).thenReturn(null);

        assertDoesNotThrow(() -> handler.onAuthenticationSuccess(request, response, authentication));
        verify(response).sendError(anyInt(), any());
    }

    @Test
    void onAuthenticationSuccess_whenConnectionAlreadyExists_reusesIt_insteadOfInsertingDuplicate() throws Exception {
        TelegramChatRepository chatRepository = mock(TelegramChatRepository.class);
        OAuthConnectionRepository connectionRepository = mock(OAuthConnectionRepository.class);
        OAuth2AuthorizedClientService clientService = mock(OAuth2AuthorizedClientService.class);
        EncryptionService encryptionService = mock(EncryptionService.class);

        GoogleAuthSuccesHandler handler = new GoogleAuthSuccesHandler(
                chatRepository, connectionRepository, clientService, encryptionService);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);

        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("telegramChatId")).thenReturn(1L);

        TelegramChatEntity chat = new TelegramChatEntity(1L, "Ana", "ana", "private", false);
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        OAuthConnectionEntity existing = new OAuthConnectionEntity(chat, "google", "old-provider-id", Instant.now());
        when(connectionRepository.findByChat(chat)).thenReturn(Optional.of(existing));

        OAuth2User oauth2User = mock(OAuth2User.class);
        when(oauth2User.getAttribute("sub")).thenReturn("new-provider-id");

        OAuth2AuthenticationToken authentication = mock(OAuth2AuthenticationToken.class);
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(authentication.getAuthorizedClientRegistrationId()).thenReturn("google");
        when(authentication.getName()).thenReturn("principal-name");

        OAuth2AccessToken accessToken = mock(OAuth2AccessToken.class);
        when(accessToken.getExpiresAt()).thenReturn(Instant.now().plusSeconds(3600));
        when(accessToken.getTokenValue()).thenReturn("raw-access-token");

        OAuth2AuthorizedClient authorizedClient = mock(OAuth2AuthorizedClient.class);
        when(authorizedClient.getAccessToken()).thenReturn(accessToken);
        when(authorizedClient.getRefreshToken()).thenReturn(null);

        when(clientService.loadAuthorizedClient("google", "principal-name")).thenReturn(authorizedClient);
        when(encryptionService.encrypt("raw-access-token")).thenReturn("encrypted-access-token");

        StringWriter writer = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(writer));

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(connectionRepository).save(existing);
        assertEquals("new-provider-id", existing.getProviderId());
        assertEquals("encrypted-access-token", existing.getAccessToken());
    }
}

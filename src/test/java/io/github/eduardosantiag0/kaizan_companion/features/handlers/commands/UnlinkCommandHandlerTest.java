package io.github.eduardosantiag0.kaizan_companion.features.handlers.commands;

import io.github.eduardosantiag0.kaizan_companion.domain.entities.OAuthConnectionEntity;
import io.github.eduardosantiag0.kaizan_companion.domain.entities.TelegramChatEntity;
import io.github.eduardosantiag0.kaizan_companion.domain.entities.UserInfoEntity;
import io.github.eduardosantiag0.kaizan_companion.domain.repositories.OAuthConnectionRepository;
import io.github.eduardosantiag0.kaizan_companion.domain.repositories.TelegramChatRepository;
import io.github.eduardosantiag0.kaizan_companion.domain.repositories.UserInfoRepository;
import io.github.eduardosantiag0.kaizan_companion.features.notifications.NotificationService;
import io.github.eduardosantiag0.kaizan_companion.support.TelegramFixtures;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UnlinkCommandHandlerTest {

    private static final TelegramChatEntity CHAT = new TelegramChatEntity(1L, "Ana", "ana", "private", false);

    @Test
    void executeCommand_onDriveTarget_deletesStoredConnection_andClearsChatFlags() {
        TelegramChatRepository chatRepository = mock(TelegramChatRepository.class);
        UserInfoRepository userInfoRepository = mock(UserInfoRepository.class);
        OAuthConnectionRepository oAuthConnectionRepository = mock(OAuthConnectionRepository.class);
        NotificationService notificationService = mock(NotificationService.class);

        CHAT.setDriveLinked(true);
        CHAT.setDriveFolderId("some-folder-id");
        when(chatRepository.findById(1L)).thenReturn(Optional.of(CHAT));
        when(userInfoRepository.findByChat(CHAT)).thenReturn(Optional.of(new UserInfoEntity(CHAT)));

        OAuthConnectionEntity connection = mock(OAuthConnectionEntity.class);
        when(oAuthConnectionRepository.findByChat(CHAT)).thenReturn(Optional.of(connection));

        UnlinkCommandHandler handler = new UnlinkCommandHandler(
                chatRepository, userInfoRepository, oAuthConnectionRepository, notificationService);

        var update = TelegramFixtures.textUpdate(1L, "/unlink drive");

        assertDoesNotThrow(() -> handler.executeCommand(update));

        verify(oAuthConnectionRepository).delete(connection);
        assertFalse(CHAT.isDriveLinked());
        assertNull(CHAT.getDriveFolderId());
    }

    @Test
    void executeCommand_onDriveTarget_withNoStoredConnection_stillClearsChatFlags_withoutThrowing() {
        TelegramChatRepository chatRepository = mock(TelegramChatRepository.class);
        UserInfoRepository userInfoRepository = mock(UserInfoRepository.class);
        OAuthConnectionRepository oAuthConnectionRepository = mock(OAuthConnectionRepository.class);
        NotificationService notificationService = mock(NotificationService.class);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(CHAT));
        when(userInfoRepository.findByChat(CHAT)).thenReturn(Optional.of(new UserInfoEntity(CHAT)));
        when(oAuthConnectionRepository.findByChat(CHAT)).thenReturn(Optional.empty());

        UnlinkCommandHandler handler = new UnlinkCommandHandler(
                chatRepository, userInfoRepository, oAuthConnectionRepository, notificationService);

        var update = TelegramFixtures.textUpdate(1L, "/unlink drive");

        assertDoesNotThrow(() -> handler.executeCommand(update));

        verify(oAuthConnectionRepository, never()).delete(any());
        assertFalse(CHAT.isDriveLinked());
    }

    @Test
    void executeCommand_onUnknownTarget_mustReplyWithUsage_insteadOfThrowing() {
        TelegramChatRepository chatRepository = mock(TelegramChatRepository.class);
        UserInfoRepository userInfoRepository = mock(UserInfoRepository.class);
        OAuthConnectionRepository oAuthConnectionRepository = mock(OAuthConnectionRepository.class);
        NotificationService notificationService = mock(NotificationService.class);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(CHAT));
        when(userInfoRepository.findByChat(CHAT)).thenReturn(Optional.of(new UserInfoEntity(CHAT)));

        UnlinkCommandHandler handler = new UnlinkCommandHandler(
                chatRepository, userInfoRepository, oAuthConnectionRepository, notificationService);

        var update = TelegramFixtures.textUpdate(1L, "/unlink something_else");

        assertDoesNotThrow(() -> handler.executeCommand(update));
        verify(notificationService).sendErrorMessage(any());
    }
}

package com.openapi.backend.controller;

import com.openapi.backend.dto.AppResponse;
import com.openapi.domain.entity.App;
import com.openapi.backend.service.AppService;
import com.openapi.backend.service.UserService;
import com.openapi.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppControllerTest {

    @Mock
    private AppService appService;

    @Mock
    private UserService userService;

    @Mock
    private HttpServletRequest request;

    private AppController controller;

    @BeforeEach
    void setUp() {
        controller = new AppController(appService, userService);
        when(request.getAttribute("openapi.userId")).thenReturn(42L);
    }

    @Test
    void createUsesAuthenticatedUserInsteadOfClientSuppliedUserId() {
        App app = app(1L, 42L, "SKabcdefghijklmnopqrstuvwxyz123456");
        when(appService.createApp("demo", 42L)).thenReturn(app);

        AppResponse response = controller.create(" demo ", request).getData();

        assertEquals(42L, response.getUserId());
        assertEquals(app.getSecretKey(), response.getSecretKey());
        verify(appService).createApp("demo", 42L);
    }

    @Test
    void listMasksSecretKey() {
        when(appService.listByUserId(42L))
                .thenReturn(List.of(app(1L, 42L, "SKabcdefghijklmnopqrstuvwxyz123456")));

        AppResponse response = controller.list(request).getData().get(0);

        assertNull(response.getSecretKey());
        assertEquals("SKab****3456", response.getSecretKeyHint());
    }

    @Test
    void revealSecretReturnsFullSecretForOwner() {
        App app = app(1L, 42L, "SKabcdefghijklmnopqrstuvwxyz123456");
        when(appService.getById(1L)).thenReturn(app);

        AppResponse response = controller.revealSecret(1L, request).getData();

        assertEquals(app.getSecretKey(), response.getSecretKey());
    }

    @Test
    void updateRejectsApplicationOwnedByAnotherUser() {
        when(appService.getById(1L)).thenReturn(app(1L, 99L, "SKabcdefghijklmnopqrstuvwxyz123456"));

        assertThrows(BusinessException.class, () -> controller.update(1L, "renamed", request));
        verify(appService, never()).updateById(any());
    }

    private App app(Long id, Long userId, String secretKey) {
        App app = new App();
        app.setId(id);
        app.setUserId(userId);
        app.setAppName("demo");
        app.setAccessKey("AKdemo");
        app.setSecretKey(secretKey);
        app.setStatus(1);
        return app;
    }
}

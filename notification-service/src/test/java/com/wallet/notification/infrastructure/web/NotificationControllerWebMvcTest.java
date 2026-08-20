package com.wallet.notification.infrastructure.web;

import com.wallet.notification.infrastructure.persistence.NotificationJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.wallet.notification.domain.Notification;
import com.wallet.notification.domain.NotificationStatus;
import com.wallet.notification.domain.NotificationType;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NotificationController.class)
class NotificationControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationJpaRepository repository;

    @MockitoBean
    @SuppressWarnings("unused")
    private com.wallet.notification.application.NotificationService notificationService;

    @Test
    @DisplayName("GET /notifications/{userId} con paginación retorna 200 con items, total, page, size")
    void list_retorna200() throws Exception {
        Notification n = new Notification();
        n.setId("notif-1");
        n.setUserId("user-1");
        n.setType(NotificationType.EMAIL);
        n.setSubject("Payment COMPLETED");
        n.setBody("body");
        n.setStatus(NotificationStatus.SENT);
        n.setProcessedEventId("evt-1");
        n.setCreatedAt(Instant.now());
        n.setSentAt(Instant.now());

        Page<Notification> page = new PageImpl<>(List.of(n), PageRequest.of(0, 20), 1);
        given(repository.findByUserIdOrderByCreatedAtDesc(eq("user-1"), any(PageRequest.class)))
            .willReturn(page);

        mockMvc.perform(get("/api/v1/notifications/user-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items").isArray())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].id").value("notif-1"))
            .andExpect(jsonPath("$.items[0].userId").value("user-1"))
            .andExpect(jsonPath("$.items[0].status").value("SENT"))
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    @DisplayName("GET /notifications/{userId} sin resultados retorna 200 con lista vacía")
    void list_sinResultados_retorna200ListaVacia() throws Exception {
        Page<Notification> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        given(repository.findByUserIdOrderByCreatedAtDesc(eq("unknown"), any(PageRequest.class)))
            .willReturn(emptyPage);

        mockMvc.perform(get("/api/v1/notifications/unknown"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items").isArray())
            .andExpect(jsonPath("$.items.length()").value(0))
            .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    @DisplayName("GET /notifications/{userId}?page=2&size=50 respeta los parámetros de paginación")
    void list_respetaParametrosPaginacion() throws Exception {
        Page<Notification> emptyPage = new PageImpl<>(List.of(), PageRequest.of(2, 50), 0);
        given(repository.findByUserIdOrderByCreatedAtDesc(eq("user-1"), any(PageRequest.class)))
            .willReturn(emptyPage);

        mockMvc.perform(get("/api/v1/notifications/user-1")
                .param("page", "2")
                .param("size", "50"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(2))
            .andExpect(jsonPath("$.size").value(50));
    }
}

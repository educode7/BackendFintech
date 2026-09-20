package com.wallet.notification.infrastructure.web;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import com.wallet.notification.application.NotificationService;
import com.wallet.notification.domain.Notification;
import com.wallet.notification.domain.NotificationRepository;
import com.wallet.shared.api.PageResponse;

import io.smallrye.common.annotation.Blocking;

/**
 * REST adapter: Notification API endpoint.
 */
@Path("/api/v1/notifications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Blocking
@Tag(name = "Notifications", description = "Notification retrieval")
public class NotificationResource {

    private static final Logger log = Logger.getLogger(NotificationResource.class);

    private final NotificationRepository repository;
    private final NotificationService notificationService;

    @Inject
    public NotificationResource(NotificationRepository repository, NotificationService notificationService) {
        this.repository = repository;
        this.notificationService = notificationService;
    }

    @GET
    @Path("/{userId}")
    @Operation(summary = "List notifications for a user")
    public PageResponse<NotificationResponse> list(
            @PathParam("userId") String userId,
            @QueryParam("page") @Min(0) int page,
            @QueryParam("size") @Min(1) @Max(100) int size) {

        if (page < 0) page = 0;
        if (size < 1 || size > 100) size = 20;

        int offset = page * size;
        List<Notification> notifications = repository.findByUserId(userId, offset, size);
        long total = repository.countByUserId(userId);

        List<NotificationResponse> items = notifications.stream()
                .map(NotificationResponse::from)
                .toList();

        return PageResponse.of(items, total, page, size);
    }

    /**
     * Notification response DTO — hides internal details.
     */
    public record NotificationResponse(
            String id, String userId, String type, String subject, String body,
            String status, java.time.Instant createdAt, java.time.Instant sentAt) {
        public static NotificationResponse from(Notification n) {
            return new NotificationResponse(n.id(), n.userId(), n.type().name(),
                    n.subject(), n.body(), n.status().name(), n.createdAt(), n.sentAt());
        }
    }
}

package com.wallet.notification.infrastructure.web;

import com.wallet.notification.application.NotificationService;
import com.wallet.notification.infrastructure.persistence.NotificationJpaRepository;
import com.wallet.shared.api.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationJpaRepository repository;
    // Reserved for future use — direct injection keeps the path consistent with other services.
    @SuppressWarnings("unused")
    private final NotificationService notificationService;

    @GetMapping("/{userId}")
    public PageResponse<com.wallet.notification.domain.Notification> list(
        @PathVariable String userId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        var result = repository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
        return PageResponse.of(result.getContent(), result.getTotalElements(), page, size);
    }
}

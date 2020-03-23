package io.gr1d.billing.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationScheduler {

    private final NotificationService notificationService;

    @Autowired
    public NotificationScheduler(final NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "${gr1d.notification.cron}")
    public void sendPendingNotification() {
        log.info("Starting schedule job to pending notification");
        notificationService.syncSendEmail();
    }

}

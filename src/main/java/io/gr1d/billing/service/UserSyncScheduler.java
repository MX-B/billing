package io.gr1d.billing.service;

import io.gr1d.billing.model.enumerations.UserStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserSyncScheduler {

	private final UserService userService;

	@Autowired
	public UserSyncScheduler(final UserService userService) {
		this.userService = userService;
	}

	@Scheduled(cron = "${gr1d.userSync.cron}")
	public void updatePendingUsers() {
		log.info("Starting schedule job to user update status");
        userService.findByUserStatusAndPendingSyncTrue(UserStatus.ACTIVE).forEach(user -> {
            try {
                userService.unblockUserSync(user);
            } catch (final Exception e) {
                log.error("Error while trying to unblock user", e);
            }
        });

        userService.findByUserStatusAndPendingSyncTrue(UserStatus.BLOCKED).forEach(user -> {
            try {
                userService.blockUserSync(user);
            } catch (final Exception e) {
                log.error("Error while trying to block user", e);
            }
        });
	}

}

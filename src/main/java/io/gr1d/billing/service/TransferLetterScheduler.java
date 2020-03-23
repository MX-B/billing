package io.gr1d.billing.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Slf4j
@Service
public class TransferLetterScheduler {

    private final TransferLetterService transferLetterService;

    @Autowired
    public TransferLetterScheduler(final TransferLetterService transferLetterService) {
        this.transferLetterService = transferLetterService;
    }

    @Scheduled(cron = "${gr1d.transferLetter.cron}")
    public void scheduledTransferLetter() {
        log.info("Starting schedule job to create TransferLetter");
        final LocalDate date = LocalDate.now();
        transferLetterService.createPayables(date);
    }

}

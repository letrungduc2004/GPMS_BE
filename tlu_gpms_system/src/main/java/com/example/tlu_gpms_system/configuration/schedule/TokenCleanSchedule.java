package com.example.tlu_gpms_system.configuration.schedule;

import com.example.tlu_gpms_system.features.account.repository.InvalidateTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TokenCleanSchedule {
    private final InvalidateTokenRepository invalidateTokenRepository;

    @Scheduled(cron = "0 0 0,12 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void cleanInvalidateToken() {
        invalidateTokenRepository.cleanUpToken();
    }
}

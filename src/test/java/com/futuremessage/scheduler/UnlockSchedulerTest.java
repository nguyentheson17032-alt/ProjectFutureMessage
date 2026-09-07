package com.futuremessage.scheduler;

import com.futuremessage.config.UnlockSchedulerProperties;
import com.futuremessage.service.UnlockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnlockSchedulerTest {

    @Mock
    private UnlockService unlockService;

    @Mock
    private ScheduledTaskRegistrar registrar;

    private UnlockScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new UnlockScheduler(
                unlockService,
                new UnlockSchedulerProperties(true, Duration.ofSeconds(30), 50)
        );
    }

    @Test
    void registersFixedDelayTask() {
        scheduler.configureTasks(registrar);

        verify(registrar).addFixedDelayTask(any(Runnable.class), eq(Duration.ofSeconds(30)));
    }

    @Test
    void runSafelyDelegatesToUnlockService() {
        when(unlockService.unlockDueMessages()).thenReturn(2);

        scheduler.runSafely();

        verify(unlockService).unlockDueMessages();
    }

    @Test
    void runSafelyDoesNotPropagateServiceFailure() {
        doThrow(new RuntimeException("db down")).when(unlockService).unlockDueMessages();

        scheduler.runSafely();

        verify(unlockService, times(1)).unlockDueMessages();
    }
}

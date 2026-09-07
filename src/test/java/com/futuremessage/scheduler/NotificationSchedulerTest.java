package com.futuremessage.scheduler;

import com.futuremessage.config.NotificationSchedulerProperties;
import com.futuremessage.service.NotificationService;
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
class NotificationSchedulerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private ScheduledTaskRegistrar registrar;

    private NotificationScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new NotificationScheduler(
                notificationService,
                new NotificationSchedulerProperties(true, Duration.ofSeconds(30), 50)
        );
    }

    @Test
    void registersFixedDelayTask() {
        scheduler.configureTasks(registrar);

        verify(registrar).addFixedDelayTask(any(Runnable.class), eq(Duration.ofSeconds(30)));
    }

    @Test
    void runSafelyDelegatesToNotificationService() {
        when(notificationService.sendPendingNotifications()).thenReturn(2);

        scheduler.runSafely();

        verify(notificationService).sendPendingNotifications();
    }

    @Test
    void runSafelyDoesNotPropagateServiceFailure() {
        doThrow(new RuntimeException("smtp down")).when(notificationService).sendPendingNotifications();

        scheduler.runSafely();

        verify(notificationService, times(1)).sendPendingNotifications();
    }
}

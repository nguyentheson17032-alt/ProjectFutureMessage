/**
 * Scheduled jobs. {@link com.futuremessage.scheduler.UnlockScheduler} chỉ là trigger;
 * chuyển trạng thái nằm ở {@link com.futuremessage.service.UnlockService}
 * và {@link com.futuremessage.domain.Message#markAvailable(java.time.Instant)}.
 */
package com.futuremessage.scheduler;

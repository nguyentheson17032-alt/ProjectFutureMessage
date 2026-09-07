/**
 * Application services orchestrate use cases; invariants live on
 * {@link com.futuremessage.domain.Message} and {@link com.futuremessage.domain.MessageRules}.
 * {@link UnlockService} là use case của scheduler: LOCKED → AVAILABLE khi đến hạn.
 */
package com.futuremessage.service;

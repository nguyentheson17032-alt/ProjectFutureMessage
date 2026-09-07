package com.futuremessage.mail;

/**
 * Nội dung email thông báo message đã mở khóa: subject + plain text + HTML.
 * Không chứa {@code content} của message — email chỉ metadata + link inbox.
 */
public record UnlockMailContent(
        String subject,
        String textBody,
        String htmlBody
) {
}

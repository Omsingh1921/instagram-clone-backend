package com.instagramclone.mail.template;

public record RenderedEmail(
        String subject,
        String htmlBody,
        String textBody
) {}

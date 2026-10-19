package com.instagramclone.mail.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreviewEmailTemplateResponse {

    private String subject;
    private String renderedHtml;
    private String renderedText;
}

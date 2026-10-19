package com.instagramclone.mail.service.interfaces;

import com.instagramclone.mail.dto.request.DynamicEmailTemplateRequest;
import com.instagramclone.mail.dto.request.PreviewEmailTemplateRequest;
import com.instagramclone.mail.dto.response.DynamicEmailTemplateResponse;
import com.instagramclone.mail.dto.response.PreviewEmailTemplateResponse;
import com.instagramclone.mail.entity.DynamicEmailTemplate;
import com.instagramclone.mail.enums.EmailType;

import java.util.List;
import java.util.Optional;

public interface IDynamicEmailTemplateService {

    List<DynamicEmailTemplateResponse> getAllTemplates();

    DynamicEmailTemplateResponse getTemplateByType(EmailType emailType);

    DynamicEmailTemplateResponse saveOrUpdateTemplate(DynamicEmailTemplateRequest request);

    Optional<DynamicEmailTemplate> findActiveTemplate(EmailType emailType);

    PreviewEmailTemplateResponse previewTemplate(PreviewEmailTemplateRequest request);

    void resetTemplateToDefault(EmailType emailType);

    void initDefaultTemplates();
}

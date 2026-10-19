package com.instagramclone.mail.service.interfaces;

import com.instagramclone.mail.config.MailProperties;
import com.instagramclone.mail.dto.request.DynamicEmailConfigRequest;
import com.instagramclone.mail.dto.request.TestEmailConfigRequest;
import com.instagramclone.mail.dto.response.DynamicEmailConfigResponse;
import com.instagramclone.mail.enums.EmailCategory;

import java.util.List;

public interface IDynamicEmailConfigService {

    List<DynamicEmailConfigResponse> getAllConfigs();

    DynamicEmailConfigResponse getConfigByCategory(EmailCategory category);

    DynamicEmailConfigResponse saveOrUpdateConfig(DynamicEmailConfigRequest request);

    MailProperties.SenderIdentity resolveSender(EmailCategory category);

    void sendTestEmail(TestEmailConfigRequest request);

    void initDefaultConfigs();
}

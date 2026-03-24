package uk.gov.moj.cp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.moj.cp.client.oauth.OAuthTokenClient;
import uk.gov.moj.cp.client.oauth.ProsecutionCaseOAuthTokenClient;

@Service
@Slf4j
public class OAuthTokenService {

    @Autowired
    private OAuthTokenClient oauthTokenClient;

    @Autowired
    private ProsecutionCaseOAuthTokenClient prosecutionCaseOAuthTokenClient;

    public String getJwtToken() {
        return oauthTokenClient.getJwtToken().accessToken();
    }

    public String getProsecutionCaseJwtToken() {
        return prosecutionCaseOAuthTokenClient.getJwtToken().accessToken();
    }
}


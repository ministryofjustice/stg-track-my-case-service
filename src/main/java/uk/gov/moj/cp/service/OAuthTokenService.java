package uk.gov.moj.cp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.moj.cp.client.oauth.OAuthTokenClient;
import uk.gov.moj.cp.model.AmpApiType;

@Service
@Slf4j
public class OAuthTokenService {

    @Autowired
    private OAuthTokenClient oauthTokenClient;

    public String getJwtToken(AmpApiType ampApiType) {
        return oauthTokenClient.getJwtToken(ampApiType).accessToken();
    }

}


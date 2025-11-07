package uk.gov.moj.cp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.moj.cp.client.TokenClient;

@Service
@Slf4j
public class TokenService {

    @Autowired
    private TokenClient tokenClient;

    public String getJwtToken() {
        return tokenClient.getJwtToken().access_token();
    }
}


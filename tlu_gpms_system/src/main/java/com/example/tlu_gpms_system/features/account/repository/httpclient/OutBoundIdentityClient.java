package com.example.tlu_gpms_system.features.account.repository.httpclient;


import com.example.tlu_gpms_system.features.account.dto.request.ExchangeTokenRequest;
import com.example.tlu_gpms_system.features.account.dto.response.ExchangeTokenResponse;
import feign.QueryMap;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "outBound-identity"
        , url = "https://oauth2.googleapis.com")
public interface OutBoundIdentityClient  {
    @PostMapping(value = "/token",
            produces = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ExchangeTokenResponse exchangeToken(@QueryMap ExchangeTokenRequest request);
}

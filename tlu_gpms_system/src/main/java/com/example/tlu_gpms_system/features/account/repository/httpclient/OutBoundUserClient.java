package com.example.tlu_gpms_system.features.account.repository.httpclient;

import com.example.tlu_gpms_system.features.account.dto.response.OutBoundUserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "outBound-user-client"
        , url = "https://www.googleapis.com")
public interface OutBoundUserClient {
    @GetMapping(value = "oauth2/v1/userinfo",
            produces = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public OutBoundUserResponse getUserInfo(@RequestParam("alt") String alt,
                                            @RequestParam("access_token") String accessToken);
}

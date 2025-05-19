package org.mdental.cliniccore.client;

import org.mdental.authcore.api.dto.CreateRealmRequest;
import org.mdental.authcore.api.dto.RealmResponse;
import org.mdental.cliniccore.model.dto.CreateClinicRequest;
import org.mdental.commons.model.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "auth-core", url = "${AUTH_CORE_URL:http://localhost:8081}")
public interface AuthCoreClient {

    @PostMapping("/realms")
    ApiResponse<RealmResponse> createRealm(@RequestBody CreateRealmRequest request);
}
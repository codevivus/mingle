package com.mingle.match_service.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Public profile data for the discover list. Needs the caller JWT, which FeignAuthRelayConfig forwards. */
@FeignClient(name = "a2srv-service")
public interface UserClient {

    @GetMapping("/user/{id}")
    Profile findById(@PathVariable("id") Long id);

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Profile(Long id, String username, String name, String surname, Integer age, String city,
                   String photoUrl, String bio) {
    }
}

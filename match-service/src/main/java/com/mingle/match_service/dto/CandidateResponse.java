package com.mingle.match_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/** userId is always set; the profile fields are null when the profile could not be loaded. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CandidateResponse(Long userId, String name, String surname, Integer age, String city,
                                String photoUrl, String bio) {

    public static CandidateResponse idOnly(Long userId) {
        return new CandidateResponse(userId, null, null, null, null, null, null);
    }
}

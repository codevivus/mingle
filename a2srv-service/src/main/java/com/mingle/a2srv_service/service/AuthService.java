package com.mingle.a2srv_service.service;

import com.mingle.a2srv_service.dto.AuthRequest;
import com.mingle.a2srv_service.dto.AuthResponse;
import com.mingle.a2srv_service.dto.RefreshTokenRequest;
import com.mingle.a2srv_service.dto.ValidateResponse;

public interface AuthService {
    AuthResponse authenticate(AuthRequest request);
    ValidateResponse validate(String token);
    void logout(String bearerToken);
    AuthResponse refreshAccessToken(RefreshTokenRequest request);
}

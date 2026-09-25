package com.rumbou.backend.service;

import com.rumbou.backend.dto.request.LoginRequest;
import com.rumbou.backend.dto.request.RegisterRequest;
import com.rumbou.backend.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(String refreshToken);

    void forgotPassword(String email);

    void resetPassword(String token, String newPassword);
}

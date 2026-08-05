package com.example.shoppingmall.auth;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.shoppingmall.auth.dto.LoginRequest;
import com.example.shoppingmall.auth.dto.LoginResponse;
import com.example.shoppingmall.auth.services.Login;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final Login login;

    public AuthController(Login login) {
        this.login = login;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(login.login(request));
    }

}

package com.example.shoppingmall.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignUpRequest(
    @NotBlank String memberName,
    @NotBlank @Pattern(regexp = "\\d{11}") String telephone,
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8) @Pattern(regexp = ".*[^A-Za-z0-9\\s].*", message = "...") String password,
    @NotBlank @Pattern(regexp = "^[a-z0-9]{3,20}$") String memberHandler,
    String address
) {}

package com.example.shoppingmall.auth.services;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.shoppingmall.auth.JwtProvider;
import com.example.shoppingmall.auth.dto.LoginRequest;
import com.example.shoppingmall.auth.dto.LoginResponse;
import com.example.shoppingmall.auth.exception.InvalidCredentialsException;
import com.example.shoppingmall.member.Member;
import com.example.shoppingmall.member.MemberRepository;

@Service
public class Login {

    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final MemberRepository memberRepository;

    public Login(
            MemberRepository memberRepository,
            JwtProvider jwtProvider,
            PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.jwtProvider = jwtProvider;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse login(LoginRequest request) {

        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new InvalidCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new InvalidCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        String token = jwtProvider.generateToken(member.getId(), member.getRole());
        return new LoginResponse(token);
    }
}

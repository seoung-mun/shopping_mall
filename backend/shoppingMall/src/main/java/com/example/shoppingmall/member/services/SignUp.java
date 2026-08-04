package com.example.shoppingmall.member.services;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.shoppingmall.member.Member;
import com.example.shoppingmall.member.MemberRepository;
import com.example.shoppingmall.member.dto.SignUpRequest;
import com.example.shoppingmall.member.exception.DuplicateMemberException;

@Service
public class SignUp {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public SignUp(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Member signUp(SignUpRequest request) {

        if (memberRepository.existsByEmail(request.email())) {
            throw new DuplicateMemberException("이미 사용 중인 이메일입니다.");
        }
        if (memberRepository.existsByMemberHandler(request.memberHandler())) {
            throw new DuplicateMemberException("이미 사용 중인 아이디입니다.");
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        Member member = new Member(request.memberName(), request.telephone(), request.email(),
                encodedPassword, request.address(), request.memberHandler());

        return memberRepository.save(member);
    }

}

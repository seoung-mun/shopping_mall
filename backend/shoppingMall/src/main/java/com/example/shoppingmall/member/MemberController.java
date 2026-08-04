package com.example.shoppingmall.member;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.shoppingmall.member.dto.MemberResponse;
import com.example.shoppingmall.member.dto.SignUpRequest;
import com.example.shoppingmall.member.dto.SignUpResponse;
import com.example.shoppingmall.member.exception.MemberNotFoundException;
import com.example.shoppingmall.member.services.SignUp;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final SignUp signUp;
    private final MemberRepository memberRepository;

    public MemberController(SignUp signUp, MemberRepository memberRepository) {
        this.signUp = signUp;
        this.memberRepository = memberRepository;
    }

    @PostMapping("/signup")
    public ResponseEntity<SignUpResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        Member member = signUp.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new SignUpResponse(member.getId(), member.getMemberHandler(), member.getEmail()));
    }

    @GetMapping("/{id}")
    public MemberResponse getMember(@PathVariable Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new MemberNotFoundException("존재하지 않는 회원입니다."));
        return new MemberResponse(
                member.getId(), member.getMemberHandler(), member.getMemberName(), member.getGrade());
    }

}

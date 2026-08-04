package com.example.shoppingmall.auth.dto;

import com.example.shoppingmall.common.Role;

public record MemberPrincipal(Long memberId, Role role) {

}

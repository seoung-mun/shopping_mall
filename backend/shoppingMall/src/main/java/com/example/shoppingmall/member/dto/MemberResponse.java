package com.example.shoppingmall.member.dto;

import com.example.shoppingmall.common.Grade;

public record MemberResponse(Long id, String memberHandler, String memberName, Grade grade) {

}

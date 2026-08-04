package com.example.shoppingmall.member;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

    public boolean existsByEmail(String email);

    public boolean existsByMemberHandler(String memberHandler);
}

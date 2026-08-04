package com.example.shoppingmall.member;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

    public boolean existsByEmail(String email);

    public boolean existsByMemberHandler(String memberHandler);

    public Optional<Member> findByEmail(String email);
}

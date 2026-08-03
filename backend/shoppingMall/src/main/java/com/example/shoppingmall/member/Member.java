package com.example.shoppingmall.member;

import java.io.Serializable;
import java.time.LocalDate;

import lombok.Getter;

import com.example.shoppingmall.common.Grade;
import com.example.shoppingmall.common.Role;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Entity
@Getter
@Table(name = "members")
public class Member implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long memberId;

    @Column
    @NotBlank
    private String memberName;

    @Column
    @NotBlank
    @Pattern(regexp = "\\d{11}", message = "{telephone.invalid}")
    private String telephone;

    @Column
    private LocalDate created; //계정을 생성한 날짜

    @Enumerated(EnumType.STRING)
    @Column
    @NotNull
    private Grade grade; // 등급

    @Column
    @NotBlank
    @Email(message = "{email.invalid}")
    private String email;

    @Column
    @NotBlank
    private String password;

    @Enumerated(EnumType.STRING)
    @Column
    @NotNull
    private Role role;

    @Column
    private boolean isDeleted;
    
    public boolean isAdmin(){
        return this.role == Role.ADMIN;
    }

    public void recalculateGrade(int totalPurchaseAmount) {
        if (totalPurchaseAmount >= Grade.VIP.getMinPurchaseAmount()) {
            this.grade = Grade.VIP;
        }
        else if (totalPurchaseAmount >= Grade.PLATINUM.getMinPurchaseAmount()) {
            this.grade = Grade.PLATINUM;
        }
        else if (totalPurchaseAmount >= Grade.GOLD.getMinPurchaseAmount()) {
            this.grade = Grade.GOLD;
        }
        else if (totalPurchaseAmount >= Grade.SILVER.getMinPurchaseAmount()) {
            this. grade = Grade.SILVER;
        }
    }



    


}
